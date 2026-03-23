package com.example.coffeshopManagement.dto.customer;

import com.example.coffeshopManagement.entity.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RedeemVoucherRequest {
    @NotBlank
    @Size(max = 20)
    private String code;

    @NotNull
    private VoucherType type;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal value;

    @NotNull
    @Min(0)
    private Integer pointsRequired;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public VoucherType getType() {
        return type;
    }

    public void setType(VoucherType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Integer getPointsRequired() {
        return pointsRequired;
    }

    public void setPointsRequired(Integer pointsRequired) {
        this.pointsRequired = pointsRequired;
    }
}
