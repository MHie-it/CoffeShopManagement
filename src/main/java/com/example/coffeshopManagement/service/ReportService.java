package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.report.SalesSummaryResponse;
import com.example.coffeshopManagement.dto.report.TopMenuItemResponse;
import com.example.coffeshopManagement.entity.OrderItem;
import com.example.coffeshopManagement.entity.OrderStatus;
import com.example.coffeshopManagement.entity.ShopOrder;
import com.example.coffeshopManagement.repository.OrderItemRepository;
import com.example.coffeshopManagement.repository.ShopOrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public ReportService(ShopOrderRepository shopOrderRepository, OrderItemRepository orderItemRepository) {
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public SalesSummaryResponse summaryByDate(LocalDate date) {
        Instant from = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        return summaryByRange(from, to);
    }

    public SalesSummaryResponse summaryByMonth(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate next = first.plusMonths(1);
        Instant from = first.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = next.atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
        return summaryByRange(from, to);
    }

    public List<TopMenuItemResponse> topMenuItems(Instant from, Instant to, int limit) {
        List<ShopOrder> orders = shopOrderRepository.findByStatusAndCreatedAtBetween(OrderStatus.done, from, to);
        Map<Integer, TopAccumulator> map = new HashMap<>();

        for (ShopOrder order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                Integer key = item.getMenuItem().getId();
                TopAccumulator acc = map.computeIfAbsent(key, id -> new TopAccumulator(
                        id, item.getMenuItem().getName(), 0L, BigDecimal.ZERO));
                acc.quantity += item.getQuantity();
                acc.revenue = acc.revenue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        return map.values().stream()
                .sorted((a, b) -> Long.compare(b.quantity, a.quantity))
                .limit(Math.max(limit, 1))
                .map(a -> {
                    TopMenuItemResponse r = new TopMenuItemResponse();
                    r.setMenuItemId(a.menuItemId);
                    r.setName(a.name);
                    r.setQuantitySold(a.quantity);
                    r.setRevenue(a.revenue);
                    return r;
                })
                .toList();
    }

    public String exportSalesCsv(Instant from, Instant to) {
        SalesSummaryResponse summary = summaryByRange(from, to);
        List<TopMenuItemResponse> topItems = topMenuItems(from, to, 20);
        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("revenue,").append(summary.getRevenue()).append("\n");
        sb.append("discount,").append(summary.getDiscount()).append("\n");
        sb.append("orders_count,").append(summary.getOrdersCount()).append("\n");
        sb.append("avg_order_value,").append(summary.getAvgOrderValue()).append("\n\n");
        sb.append("top_menu_item_id,top_menu_item_name,quantity_sold,revenue\n");
        for (TopMenuItemResponse item : topItems) {
            sb.append(item.getMenuItemId()).append(",");
            sb.append(escapeCsv(item.getName())).append(",");
            sb.append(item.getQuantitySold()).append(",");
            sb.append(item.getRevenue()).append("\n");
        }
        return sb.toString();
    }

    private SalesSummaryResponse summaryByRange(Instant from, Instant to) {
        List<ShopOrder> orders = shopOrderRepository.findByStatusAndCreatedAtBetween(OrderStatus.done, from, to);
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        for (ShopOrder order : orders) {
            revenue = revenue.add(order.getTotal() == null ? BigDecimal.ZERO : order.getTotal());
            discount = discount.add(order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount());
        }
        SalesSummaryResponse response = new SalesSummaryResponse();
        response.setRevenue(revenue);
        response.setDiscount(discount);
        response.setOrdersCount(orders.size());
        response.setAvgOrderValue(orders.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP));
        return response;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static class TopAccumulator {
        Integer menuItemId;
        String name;
        long quantity;
        BigDecimal revenue;

        TopAccumulator(Integer menuItemId, String name, long quantity, BigDecimal revenue) {
            this.menuItemId = menuItemId;
            this.name = name;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }
}
