package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.dashboard.ManagerDashboardSummaryResponse;
import com.example.coffeshopManagement.entity.Ingredient;
import com.example.coffeshopManagement.entity.OrderItem;
import com.example.coffeshopManagement.entity.OrderStatus;
import com.example.coffeshopManagement.entity.ShopOrder;
import com.example.coffeshopManagement.repository.EmployeeRepository;
import com.example.coffeshopManagement.repository.IngredientRepository;
import com.example.coffeshopManagement.repository.OrderItemRepository;
import com.example.coffeshopManagement.repository.ShopOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManagerDashboardService {
    private final ShopOrderRepository shopOrderRepository;
    private final EmployeeRepository employeeRepository;
    private final IngredientRepository ingredientRepository;
    private final OrderItemRepository orderItemRepository;

    public ManagerDashboardService(
            ShopOrderRepository shopOrderRepository,
            EmployeeRepository employeeRepository,
            IngredientRepository ingredientRepository,
            OrderItemRepository orderItemRepository) {
        this.shopOrderRepository = shopOrderRepository;
        this.employeeRepository = employeeRepository;
        this.ingredientRepository = ingredientRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public ManagerDashboardSummaryResponse getSummary() {
        ManagerDashboardSummaryResponse response = new ManagerDashboardSummaryResponse();

        long servingOrders = shopOrderRepository.findByStatus(OrderStatus.pending).size()
                + shopOrderRepository.findByStatus(OrderStatus.serving).size();
        response.setServingOrdersCount(servingOrders);

        long activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                .count();
        response.setActiveEmployeesCount(activeEmployees);

        long lowStock = ingredientRepository.findAll().stream()
                .filter(this::isLowStock)
                .count();
        response.setLowStockCount(lowStock);

        response.setTopMenuItemName(resolveTopMenuItemToday());
        return response;
    }

    private boolean isLowStock(Ingredient ingredient) {
        BigDecimal stock = ingredient.getStockQuantity() == null ? BigDecimal.ZERO : ingredient.getStockQuantity();
        BigDecimal threshold = ingredient.getMinThreshold() == null ? BigDecimal.ZERO : ingredient.getMinThreshold();
        return stock.compareTo(threshold) <= 0;
    }

    private String resolveTopMenuItemToday() {
        LocalDate today = LocalDate.now();
        Instant from = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        List<ShopOrder> doneOrders = shopOrderRepository.findByStatusAndCreatedAtBetween(OrderStatus.done, from, to);
        if (doneOrders.isEmpty()) {
            return "Chưa có";
        }

        List<Integer> orderIds = doneOrders.stream().map(ShopOrder::getId).toList();
        List<OrderItem> items = orderItemRepository.findByOrderIdIn(orderIds);
        if (items.isEmpty()) {
            return "Chưa có";
        }

        Map<String, Integer> soldByName = new HashMap<>();
        for (OrderItem item : items) {
            if (item.getMenuItem() == null) {
                continue;
            }
            String name = item.getMenuItem().getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            soldByName.put(name, soldByName.getOrDefault(name, 0) + qty);
        }
        if (soldByName.isEmpty()) {
            return "Chưa có";
        }

        return soldByName.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("Chưa có");
    }
}
