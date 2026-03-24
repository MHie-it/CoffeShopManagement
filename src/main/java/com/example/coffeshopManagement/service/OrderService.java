package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.order.*;
import com.example.coffeshopManagement.entity.*;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableEntityRepository tableEntityRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final PaymentRepository paymentRepository;
    private final VoucherRepository voucherRepository;
    private final PointLogRepository pointLogRepository;
    private final AuditLogService auditLogService;
    private final MomoGatewayService momoGatewayService;

    public OrderService(
            ShopOrderRepository shopOrderRepository,
            OrderItemRepository orderItemRepository,
            TableEntityRepository tableEntityRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            MenuItemRepository menuItemRepository,
            PaymentRepository paymentRepository,
            VoucherRepository voucherRepository,
            PointLogRepository pointLogRepository,
            AuditLogService auditLogService,
            MomoGatewayService momoGatewayService) {
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableEntityRepository = tableEntityRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.menuItemRepository = menuItemRepository;
        this.paymentRepository = paymentRepository;
        this.voucherRepository = voucherRepository;
        this.pointLogRepository = pointLogRepository;
        this.auditLogService = auditLogService;
        this.momoGatewayService = momoGatewayService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String username) {
        TableEntity table = tableEntityRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShopOrder order = new ShopOrder();
        order.setTable(table);
        order.setUser(user);
        order.setStatus(OrderStatus.pending);
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            order.setCustomer(customer);
        }

        table.setStatus(TableStatus.occupied);
        tableEntityRepository.save(table);

        ShopOrder saved = shopOrderRepository.save(order);
        auditLogService.log(username, "ORDER_CREATE", "orders", saved.getId(),
                java.util.Map.of("tableId", saved.getTable().getId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<ShopOrder> orders = status == null
                ? shopOrderRepository.findAll()
                : shopOrderRepository.findByStatus(status);
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer orderId) {
        ShopOrder order = findOrder(orderId);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse addItem(Integer orderId, OrderItemUpsertRequest request) {
        ShopOrder order = findOrder(orderId);
        ensureEditable(order);

        MenuItem menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setMenuItem(menuItem);
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(menuItem.getPrice());
        item.setNote(request.getNote());
        orderItemRepository.save(item);

        recalculateAndSave(order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateItem(Integer orderId, Integer itemId, UpdateOrderItemRequest request) {
        ShopOrder order = findOrder(orderId);
        ensureEditable(order);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
        if (!item.getOrder().getId().equals(order.getId())) {
            throw new BadRequestException("Order item does not belong to this order");
        }

        item.setQuantity(request.getQuantity());
        item.setNote(request.getNote());
        orderItemRepository.save(item);

        recalculateAndSave(order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse removeItem(Integer orderId, Integer itemId) {
        ShopOrder order = findOrder(orderId);
        ensureEditable(order);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
        if (!item.getOrder().getId().equals(order.getId())) {
            throw new BadRequestException("Order item does not belong to this order");
        }

        orderItemRepository.delete(item);
        recalculateAndSave(order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Integer orderId, CancelOrderRequest request) {
        ShopOrder order = findOrder(orderId);
        ensureNotPaid(orderId);
        if (order.getStatus() == OrderStatus.cancelled) {
            throw new BadRequestException("Order already cancelled");
        }
        if (order.getStatus() == OrderStatus.done) {
            throw new BadRequestException("Order already completed");
        }

        order.setStatus(OrderStatus.cancelled);
        order.setCancelReason(request.getReason());

        TableEntity table = order.getTable();
        table.setStatus(TableStatus.empty);
        tableEntityRepository.save(table);

        ShopOrder saved = shopOrderRepository.save(order);
        String actor = saved.getUser() == null ? "unknown" : saved.getUser().getUsername();
        auditLogService.log(actor, "ORDER_CANCEL", "orders", saved.getId(),
                java.util.Map.of("reason", request.getReason()));
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse checkout(Integer orderId, CheckoutOrderRequest request) {
        ShopOrder order = findOrder(orderId);
        ensureEditable(order);
        ensureNotPaid(orderId);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new BadRequestException("Cannot checkout empty order");
        }

        CheckoutAmount checkoutAmount = calculateCheckoutAmount(order, items, request.getVoucherCode());

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.getPaymentMethod());
        payment.setAmount(checkoutAmount.total());
        payment.setStatus(PaymentStatus.success);
        payment.setMomoRef(request.getMomoRef());
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        completePaidOrder(order, checkoutAmount, request.getPaymentMethod().name());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId());
        response.setOrderId(order.getId());
        response.setMethod(payment.getMethod());
        response.setStatus(payment.getStatus());
        response.setAmount(payment.getAmount());
        response.setPaidAt(payment.getPaidAt());
        return response;
    }

    @Transactional
    public MomoCreatePaymentResponse createMomoPayment(Integer orderId, MomoCreatePaymentRequest request) {
        ShopOrder order = findOrder(orderId);
        ensureEditable(order);
        ensureNotPaid(orderId);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new BadRequestException("Cannot checkout empty order");
        }

        CheckoutAmount checkoutAmount = calculateCheckoutAmount(order, items, request.getVoucherCode());
        String momoOrderId = "ORDER-" + orderId + "-" + System.currentTimeMillis();
        String orderInfo = request.getOrderInfo();
        if (orderInfo == null || orderInfo.isBlank()) {
            orderInfo = "Thanh toan don #" + orderId;
        }

        MomoGatewayService.MomoCreateResult result =
                momoGatewayService.createPayment(momoOrderId, checkoutAmount.total(), orderInfo);
        log.info(
                "MoMo payment created: orderId={}, momoOrderId={}, amount={}",
                orderId,
                result.getMomoOrderId(),
                checkoutAmount.total());

        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.momo);
        payment.setAmount(checkoutAmount.total());
        payment.setStatus(PaymentStatus.pending);
        payment.setMomoRef(result.getMomoOrderId());
        payment.setPaidAt(null);
        Payment savedPayment = paymentRepository.save(payment);

        order.setVoucher(checkoutAmount.usedVoucher());
        order.setSubtotal(checkoutAmount.subtotal());
        order.setDiscount(checkoutAmount.discount());
        order.setTotal(checkoutAmount.total());
        shopOrderRepository.save(order);

        MomoCreatePaymentResponse response = new MomoCreatePaymentResponse();
        response.setPaymentId(savedPayment.getId());
        response.setOrderId(orderId);
        response.setAmount(checkoutAmount.total());
        response.setMomoOrderId(result.getMomoOrderId());
        response.setRequestId(result.getRequestId());
        response.setPayUrl(result.getPayUrl());
        response.setDeeplink(result.getDeeplink());
        response.setQrCodeUrl(result.getQrCodeUrl());
        return response;
    }

    @Transactional
    public void handleMomoCallback(java.util.Map<String, String> callbackParams) {
        log.info(
                "MoMo callback received: orderId={}, requestId={}, resultCode={}, transId={}",
                callbackParams.getOrDefault("orderId", ""),
                callbackParams.getOrDefault("requestId", ""),
                callbackParams.getOrDefault("resultCode", ""),
                callbackParams.getOrDefault("transId", ""));
        boolean signatureOk = momoGatewayService.verifyCallbackSignature(callbackParams);
        if (!signatureOk) {
            throw new BadRequestException("Invalid MoMo callback signature");
        }

        String momoOrderId = callbackParams.get("orderId");
        if (momoOrderId == null || momoOrderId.isBlank()) {
            throw new BadRequestException("Missing MoMo orderId");
        }

        Payment payment = paymentRepository.findByMomoRef(momoOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for MoMo order"));

        if (payment.getStatus() == PaymentStatus.success) {
            return;
        }

        int resultCode;
        try {
            resultCode = Integer.parseInt(callbackParams.getOrDefault("resultCode", "-1"));
        } catch (NumberFormatException ex) {
            resultCode = -1;
        }

        if (resultCode != 0) {
            payment.setStatus(PaymentStatus.failed);
            paymentRepository.save(payment);
            log.warn(
                    "MoMo callback payment failed: orderId={}, momoOrderId={}, resultCode={}",
                    payment.getOrder().getId(),
                    momoOrderId,
                    resultCode);
            return;
        }

        payment.setStatus(PaymentStatus.success);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        log.info(
                "MoMo callback payment success: orderId={}, momoOrderId={}",
                payment.getOrder().getId(),
                momoOrderId);

        ShopOrder order = payment.getOrder();
        CheckoutAmount checkoutAmount = new CheckoutAmount(
                order.getSubtotal(),
                order.getDiscount(),
                payment.getAmount(),
                order.getVoucher());
        completePaidOrder(order, checkoutAmount, PaymentMethod.momo.name());
    }

    private void addPointsIfEligible(ShopOrder order, BigDecimal total) {
        if (order.getCustomer() == null) {
            return;
        }
        int earnedPoints = total.divide(new BigDecimal("10000"), RoundingMode.DOWN).intValue();
        if (earnedPoints <= 0) {
            return;
        }

        Customer customer = order.getCustomer();
        customer.setPoints(customer.getPoints() + earnedPoints);
        customerRepository.save(customer);

        PointLog log = new PointLog();
        log.setCustomer(customer);
        log.setOrder(order);
        log.setDelta(earnedPoints);
        log.setDescription("Earn points from checkout order #" + order.getId());
        pointLogRepository.save(log);
    }

    private void validateVoucherForOrder(Voucher voucher, ShopOrder order) {
        if (Boolean.TRUE.equals(voucher.getIsUsed())) {
            throw new BadRequestException("Voucher already used");
        }
        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Voucher expired");
        }
        if (order.getCustomer() == null) {
            throw new BadRequestException("Order has no customer to apply voucher");
        }
        if (!voucher.getCustomer().getId().equals(order.getCustomer().getId())) {
            throw new BadRequestException("Voucher does not belong to this customer");
        }
    }

    private CheckoutAmount calculateCheckoutAmount(ShopOrder order, List<OrderItem> items, String voucherCode) {
        BigDecimal subtotal = calculateSubtotal(items);
        BigDecimal discount = BigDecimal.ZERO;
        Voucher usedVoucher = order.getVoucher();

        if (voucherCode != null && !voucherCode.isBlank()) {
            Voucher voucher = voucherRepository.findByCode(voucherCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
            validateVoucherForOrder(voucher, order);
            discount = computeDiscount(subtotal, voucher);
            usedVoucher = voucher;
        } else if (usedVoucher != null) {
            validateVoucherForOrder(usedVoucher, order);
            discount = computeDiscount(subtotal, usedVoucher);
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        BigDecimal total = subtotal.subtract(discount);
        return new CheckoutAmount(subtotal, discount, total, usedVoucher);
    }

    private void completePaidOrder(ShopOrder order, CheckoutAmount checkoutAmount, String paymentMethod) {
        order.setVoucher(checkoutAmount.usedVoucher());
        order.setSubtotal(checkoutAmount.subtotal());
        order.setDiscount(checkoutAmount.discount());
        order.setTotal(checkoutAmount.total());
        order.setStatus(OrderStatus.done);
        shopOrderRepository.save(order);

        Voucher usedVoucher = checkoutAmount.usedVoucher();
        if (usedVoucher != null && !Boolean.TRUE.equals(usedVoucher.getIsUsed())) {
            usedVoucher.setIsUsed(Boolean.TRUE);
            voucherRepository.save(usedVoucher);
        }

        TableEntity table = order.getTable();
        table.setStatus(TableStatus.empty);
        tableEntityRepository.save(table);

        addPointsIfEligible(order, checkoutAmount.total());
        String actor = order.getUser() == null ? "unknown" : order.getUser().getUsername();
        auditLogService.log(actor, "ORDER_CHECKOUT", "orders", order.getId(),
                java.util.Map.of("paymentMethod", paymentMethod, "total", checkoutAmount.total().toString()));
    }

    private BigDecimal computeDiscount(BigDecimal subtotal, Voucher voucher) {
        return switch (voucher.getType()) {
            case percent -> subtotal.multiply(voucher.getValue()).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            case fixed -> voucher.getValue();
        };
    }

    private void ensureNotPaid(Integer orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.success) {
                throw new BadRequestException("Order already paid");
            }
        });
    }

    private void ensureEditable(ShopOrder order) {
        if (order.getStatus() == OrderStatus.cancelled || order.getStatus() == OrderStatus.done) {
            throw new BadRequestException("Order is not editable");
        }
    }

    private ShopOrder recalculateAndSave(ShopOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        BigDecimal subtotal = calculateSubtotal(items);
        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(subtotal);
        return shopOrderRepository.save(order);
    }

    private BigDecimal calculateSubtotal(List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal line = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(line);
        }
        return subtotal;
    }

    private record CheckoutAmount(BigDecimal subtotal, BigDecimal discount, BigDecimal total, Voucher usedVoucher) {
    }

    private ShopOrder findOrder(Integer orderId) {
        return shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private OrderResponse toResponse(ShopOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemResponse r = new OrderItemResponse();
            r.setId(item.getId());
            r.setMenuItemId(item.getMenuItem().getId());
            r.setMenuItemName(item.getMenuItem().getName());
            r.setQuantity(item.getQuantity());
            r.setUnitPrice(item.getUnitPrice());
            r.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            r.setNote(item.getNote());
            itemResponses.add(r);
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setTableId(order.getTable().getId());
        response.setTableName(order.getTable().getName());
        response.setUserId(order.getUser().getId());
        response.setUsername(order.getUser().getUsername());
        response.setStatus(order.getStatus());
        response.setSubtotal(order.getSubtotal());
        response.setDiscount(order.getDiscount());
        response.setTotal(order.getTotal());
        response.setCancelReason(order.getCancelReason());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(itemResponses);
        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getId());
            response.setCustomerName(order.getCustomer().getFullName());
        }
        return response;
    }
}
