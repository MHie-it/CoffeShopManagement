package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.customer.CustomerCreateRequest;
import com.example.coffeshopManagement.dto.customer.CustomerResponse;
import com.example.coffeshopManagement.dto.customer.PointLogResponse;
import com.example.coffeshopManagement.dto.customer.RedeemVoucherRequest;
import com.example.coffeshopManagement.dto.customer.VoucherResponse;
import com.example.coffeshopManagement.entity.Customer;
import com.example.coffeshopManagement.entity.PointLog;
import com.example.coffeshopManagement.entity.Voucher;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.CustomerRepository;
import com.example.coffeshopManagement.repository.PointLogRepository;
import com.example.coffeshopManagement.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerMemberService {
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final PointLogRepository pointLogRepository;
    private final AuditLogService auditLogService;

    public CustomerMemberService(
            CustomerRepository customerRepository,
            VoucherRepository voucherRepository,
            PointLogRepository pointLogRepository,
            AuditLogService auditLogService) {
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
        this.pointLogRepository = pointLogRepository;
        this.auditLogService = auditLogService;
    }

    public CustomerResponse createCustomer(CustomerCreateRequest request, String username) {
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone already exists");
        }
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setPoints(0);
        Customer saved = customerRepository.save(customer);
        auditLogService.log(username, "CUSTOMER_CREATE", "customers", saved.getId(),
                java.util.Map.of("phone", saved.getPhone()));
        return toResponse(saved);
    }

    public CustomerResponse getByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return toResponse(customer);
    }

    @Transactional
    public VoucherResponse redeemVoucher(Integer customerId, RedeemVoucherRequest request, String username) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (voucherRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Voucher code already exists");
        }
        if (customer.getPoints() < request.getPointsRequired()) {
            throw new BadRequestException("Insufficient points");
        }

        customer.setPoints(customer.getPoints() - request.getPointsRequired());
        customerRepository.save(customer);

        Voucher voucher = new Voucher();
        voucher.setCustomer(customer);
        voucher.setCode(request.getCode());
        voucher.setType(request.getType());
        voucher.setValue(request.getValue());
        voucher.setPointsRequired(request.getPointsRequired());
        Voucher savedVoucher = voucherRepository.save(voucher);

        PointLog pointLog = new PointLog();
        pointLog.setCustomer(customer);
        pointLog.setVoucher(savedVoucher);
        pointLog.setDelta(-request.getPointsRequired());
        pointLog.setDescription("Redeem voucher " + savedVoucher.getCode());
        pointLogRepository.save(pointLog);

        auditLogService.log(username, "VOUCHER_REDEEM", "vouchers", savedVoucher.getId(),
                java.util.Map.of("customerId", customerId, "pointsRequired", request.getPointsRequired()));
        return toVoucherResponse(savedVoucher);
    }

    public List<VoucherResponse> getCustomerVouchers(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found");
        }
        return voucherRepository.findByCustomerId(customerId).stream().map(this::toVoucherResponse).toList();
    }

    public List<PointLogResponse> getCustomerPointLogs(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found");
        }
        return pointLogRepository.findByCustomerId(customerId).stream().map(this::toPointLogResponse).toList();
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setFullName(customer.getFullName());
        response.setPhone(customer.getPhone());
        response.setPoints(customer.getPoints());
        return response;
    }

    private VoucherResponse toVoucherResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setType(voucher.getType());
        response.setValue(voucher.getValue());
        response.setPointsRequired(voucher.getPointsRequired());
        response.setIsUsed(voucher.getIsUsed());
        response.setExpiresAt(voucher.getExpiresAt());
        return response;
    }

    private PointLogResponse toPointLogResponse(PointLog log) {
        PointLogResponse response = new PointLogResponse();
        response.setId(log.getId());
        response.setDelta(log.getDelta());
        response.setDescription(log.getDescription());
        response.setCreatedAt(log.getCreatedAt());
        if (log.getOrder() != null) {
            response.setOrderId(log.getOrder().getId());
        }
        if (log.getVoucher() != null) {
            response.setVoucherCode(log.getVoucher().getCode());
        }
        return response;
    }
}
