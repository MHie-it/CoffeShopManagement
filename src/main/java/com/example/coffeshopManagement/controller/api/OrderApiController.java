package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.order.*;
import com.example.coffeshopManagement.entity.OrderStatus;
import com.example.coffeshopManagement.service.InvoiceService;
import com.example.coffeshopManagement.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderApiController {
    private final OrderService orderService;
    private final InvoiceService invoiceService;

    public OrderApiController(OrderService orderService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    @PostMapping("/api/staff/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, authentication.getName()));
    }

    @GetMapping("/api/staff/orders")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/api/staff/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PostMapping("/api/staff/orders/{orderId}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Integer orderId,
            @Valid @RequestBody OrderItemUpsertRequest request) {
        return ResponseEntity.ok(orderService.addItem(orderId, request));
    }

    @PutMapping("/api/staff/orders/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponse> updateItem(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId,
            @Valid @RequestBody UpdateOrderItemRequest request) {
        return ResponseEntity.ok(orderService.updateItem(orderId, itemId, request));
    }

    @DeleteMapping("/api/staff/orders/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponse> removeItem(
            @PathVariable Integer orderId,
            @PathVariable Integer itemId) {
        return ResponseEntity.ok(orderService.removeItem(orderId, itemId));
    }

    @PostMapping("/api/staff/orders/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, request));
    }

    @PostMapping("/api/staff/orders/{orderId}/checkout")
    public ResponseEntity<PaymentResponse> checkout(
            @PathVariable Integer orderId,
            @Valid @RequestBody CheckoutOrderRequest request) {
        return ResponseEntity.ok(orderService.checkout(orderId, request));
    }

    @GetMapping("/api/staff/orders/{orderId}/invoice/pdf")
    public ResponseEntity<ByteArrayResource> printInvoice(@PathVariable Integer orderId) {
        byte[] pdf = invoiceService.generateInvoicePdf(orderId);
        ByteArrayResource resource = new ByteArrayResource(pdf);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("invoice-" + orderId + ".pdf")
                .build());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
