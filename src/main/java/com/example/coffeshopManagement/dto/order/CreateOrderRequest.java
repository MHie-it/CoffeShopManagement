package com.example.coffeshopManagement.dto.order;

import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {
    @NotNull
    private Integer tableId;

    private Integer customerId;

    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }
}
