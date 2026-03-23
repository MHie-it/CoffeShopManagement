package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.customer.CustomerCreateRequest;
import com.example.coffeshopManagement.dto.customer.CustomerResponse;
import com.example.coffeshopManagement.dto.customer.PointLogResponse;
import com.example.coffeshopManagement.dto.customer.RedeemVoucherRequest;
import com.example.coffeshopManagement.dto.customer.VoucherResponse;
import com.example.coffeshopManagement.service.CustomerMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CustomerMemberApiController {
    private final CustomerMemberService customerMemberService;

    public CustomerMemberApiController(CustomerMemberService customerMemberService) {
        this.customerMemberService = customerMemberService;
    }

    @PostMapping("/api/staff/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerMemberService.createCustomer(request, authentication.getName()));
    }

    @GetMapping("/api/staff/customers/by-phone")
    public ResponseEntity<CustomerResponse> getByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(customerMemberService.getByPhone(phone));
    }

    @PostMapping("/api/staff/customers/{customerId}/vouchers/redeem")
    public ResponseEntity<VoucherResponse> redeemVoucher(
            @PathVariable Integer customerId,
            @Valid @RequestBody RedeemVoucherRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerMemberService.redeemVoucher(customerId, request, authentication.getName()));
    }

    @GetMapping("/api/staff/customers/{customerId}/vouchers")
    public ResponseEntity<List<VoucherResponse>> getCustomerVouchers(@PathVariable Integer customerId) {
        return ResponseEntity.ok(customerMemberService.getCustomerVouchers(customerId));
    }

    @GetMapping("/api/staff/customers/{customerId}/points/history")
    public ResponseEntity<List<PointLogResponse>> getCustomerPointHistory(@PathVariable Integer customerId) {
        return ResponseEntity.ok(customerMemberService.getCustomerPointLogs(customerId));
    }
}
