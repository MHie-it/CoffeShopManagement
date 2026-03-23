package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.inventory.IngredientCreateRequest;
import com.example.coffeshopManagement.dto.inventory.IngredientResponse;
import com.example.coffeshopManagement.dto.inventory.InventoryAdjustRequest;
import com.example.coffeshopManagement.dto.inventory.InventoryLogResponse;
import com.example.coffeshopManagement.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class InventoryApiController {
    private final InventoryService inventoryService;

    public InventoryApiController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/api/manager/inventory/ingredients")
    public ResponseEntity<IngredientResponse> createIngredient(
            @Valid @RequestBody IngredientCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.createIngredient(request, authentication.getName()));
    }

    @GetMapping("/api/manager/inventory/ingredients")
    public ResponseEntity<List<IngredientResponse>> getIngredients() {
        return ResponseEntity.ok(inventoryService.getIngredients());
    }

    @GetMapping("/api/manager/inventory/ingredients/low-stock")
    public ResponseEntity<List<IngredientResponse>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStockIngredients());
    }

    @PostMapping("/api/manager/inventory/ingredients/{ingredientId}/adjust")
    public ResponseEntity<IngredientResponse> adjustStock(
            @PathVariable Integer ingredientId,
            @Valid @RequestBody InventoryAdjustRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventoryService.adjustStock(ingredientId, request, authentication.getName()));
    }

    @GetMapping("/api/manager/inventory/logs")
    public ResponseEntity<List<InventoryLogResponse>> getLogs(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Integer ingredientId) {
        return ResponseEntity.ok(inventoryService.getInventoryLogs(from, to, ingredientId));
    }
}
