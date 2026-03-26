package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.common.ApiMessageResponse;
import com.example.coffeshopManagement.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PaymentApiController {
    private final OrderService orderService;

    public PaymentApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/payments/momo/callback")
    public ResponseEntity<ApiMessageResponse> momoCallback(@RequestParam Map<String, String> params) {
        orderService.handleMomoCallback(params);
        return ResponseEntity.ok(new ApiMessageResponse("ok"));
    }

    @PostMapping("/api/payments/momo/callback")
    public ResponseEntity<ApiMessageResponse> momoCallbackPost(@RequestBody Map<String, Object> body) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            params.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        orderService.handleMomoCallback(params);
        return ResponseEntity.ok(new ApiMessageResponse("ok"));
    }
}
