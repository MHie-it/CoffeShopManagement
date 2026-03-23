package com.example.coffeshopManagement.dto.inventory;

import com.example.coffeshopManagement.entity.InventoryLogType;

import java.math.BigDecimal;
import java.time.Instant;

public class InventoryLogResponse {
    private Integer id;
    private Integer ingredientId;
    private String ingredientName;
    private InventoryLogType type;
    private BigDecimal quantity;
    private String note;
    private String username;
    private Instant createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
