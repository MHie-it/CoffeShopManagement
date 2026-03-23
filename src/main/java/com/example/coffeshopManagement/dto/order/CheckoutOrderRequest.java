package com.example.coffeshopManagement.dto.order;

import com.example.coffeshopManagement.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class CheckoutOrderRequest {
    @NotNull
    private PaymentMethod paymentMethod;

    private String voucherCode;
    private String momoRef;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getMomoRef() {
        return momoRef;
    }

    public void setMomoRef(String momoRef) {
        this.momoRef = momoRef;
    }
}
