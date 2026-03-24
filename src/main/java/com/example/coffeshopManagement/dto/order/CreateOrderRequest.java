package com.example.coffeshopManagement.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {
    @NotNull
    private Integer tableId;

    private Integer customerId;

    @Valid
    private List<OrderItemUpsertRequest> items;

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

    public List<OrderItemUpsertRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemUpsertRequest> items) {
        this.items = items;
    }
}
