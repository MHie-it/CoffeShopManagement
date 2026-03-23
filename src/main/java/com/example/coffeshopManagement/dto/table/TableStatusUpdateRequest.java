package com.example.coffeshopManagement.dto.table;

import com.example.coffeshopManagement.entity.TableStatus;
import jakarta.validation.constraints.NotNull;

public class TableStatusUpdateRequest {
    @NotNull
    private TableStatus status;

    public TableStatus getStatus() {
        return status;
    }

    public void setStatus(TableStatus status) {
        this.status = status;
    }
}
