package com.example.coffeshopManagement.dto.inventory;

import com.example.coffeshopManagement.entity.InventoryLogType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class InventoryAdjustRequest {
    @NotNull
    private InventoryLogType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;

    private String note;

    public InventoryLogType getType() {
        return type;
    }

    public void setType(InventoryLogType type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
