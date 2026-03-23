package com.example.coffeshopManagement.dto.order;

import jakarta.validation.constraints.NotBlank;

public class CancelOrderRequest {
    @NotBlank
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
