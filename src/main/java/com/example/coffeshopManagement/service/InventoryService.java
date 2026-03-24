package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.inventory.IngredientCreateRequest;
import com.example.coffeshopManagement.dto.inventory.IngredientResponse;
import com.example.coffeshopManagement.dto.inventory.InventoryAdjustRequest;
import com.example.coffeshopManagement.dto.inventory.InventoryLogResponse;
import com.example.coffeshopManagement.entity.Ingredient;
import com.example.coffeshopManagement.entity.InventoryLog;
import com.example.coffeshopManagement.entity.InventoryLogType;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.IngredientRepository;
import com.example.coffeshopManagement.repository.InventoryLogRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    private final IngredientRepository ingredientRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public InventoryService(
            IngredientRepository ingredientRepository,
            InventoryLogRepository inventoryLogRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.ingredientRepository = ingredientRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public IngredientResponse createIngredient(IngredientCreateRequest request, String username) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.getName());
        ingredient.setUnit(request.getUnit());
        ingredient.setStockQuantity(request.getStockQuantity());
        ingredient.setMinThreshold(request.getMinThreshold());
        Ingredient saved = ingredientRepository.save(ingredient);
        auditLogService.log(username, "INGREDIENT_CREATE", "ingredients", saved.getId(),
                Map.of("name", saved.getName(), "stock", String.valueOf(saved.getStockQuantity())));
        return toIngredientResponse(saved);
    }

    public List<IngredientResponse> getIngredients() {
        return ingredientRepository.findAll().stream().map(this::toIngredientResponse).toList();
    }

    public List<IngredientResponse> getLowStockIngredients() {
        return ingredientRepository.findAll().stream()
                .filter(this::isLowStock)
                .map(this::toIngredientResponse)
                .toList();
    }

    @Transactional
    public IngredientResponse adjustStock(Integer ingredientId, InventoryAdjustRequest request, String username) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));

        if (request.getType() == InventoryLogType.import_) {
            ingredient.setStockQuantity(ingredient.getStockQuantity().add(request.getQuantity()));
        } else if (request.getType() == InventoryLogType.export) {
            BigDecimal remaining = ingredient.getStockQuantity().subtract(request.getQuantity());
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Insufficient stock");
            }
            ingredient.setStockQuantity(remaining);
        } else {
            ingredient.setStockQuantity(request.getQuantity());
        }
        Ingredient savedIngredient = ingredientRepository.save(ingredient);

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        InventoryLog log = new InventoryLog();
        log.setIngredient(savedIngredient);
        log.setUser(user);
        log.setType(request.getType());
        log.setQuantity(request.getQuantity());
        log.setNote(request.getNote());
        InventoryLog savedLog = inventoryLogRepository.save(log);

        auditLogService.log(username, "INVENTORY_ADJUST", "inventory_logs", savedLog.getId(),
                Map.of("ingredientId", ingredientId, "type", request.getType().name(), "quantity", request.getQuantity().toString()));
        return toIngredientResponse(savedIngredient);
    }

    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getInventoryLogs(Instant from, Instant to, Integer ingredientId) {
        List<InventoryLog> logs;
        if (ingredientId != null) {
            logs = inventoryLogRepository.findByIngredientId(ingredientId);
        } else if (from != null && to != null) {
            logs = inventoryLogRepository.findByCreatedAtBetween(from, to);
        } else {
            logs = inventoryLogRepository.findAll();
        }
        return logs.stream()
                .sorted((a, b) -> {
                    Instant aTime = a.getCreatedAt() == null ? Instant.EPOCH : a.getCreatedAt();
                    Instant bTime = b.getCreatedAt() == null ? Instant.EPOCH : b.getCreatedAt();
                    return bTime.compareTo(aTime);
                })
                .map(this::toInventoryLogResponse)
                .toList();
    }

    private boolean isLowStock(Ingredient ingredient) {
        return ingredient.getStockQuantity() != null
                && ingredient.getMinThreshold() != null
                && ingredient.getStockQuantity().compareTo(ingredient.getMinThreshold()) <= 0;
    }

    private IngredientResponse toIngredientResponse(Ingredient ingredient) {
        IngredientResponse response = new IngredientResponse();
        response.setId(ingredient.getId());
        response.setName(ingredient.getName());
        response.setUnit(ingredient.getUnit());
        response.setStockQuantity(ingredient.getStockQuantity());
        response.setMinThreshold(ingredient.getMinThreshold());
        response.setLowStock(isLowStock(ingredient));
        return response;
    }

    private InventoryLogResponse toInventoryLogResponse(InventoryLog log) {
        InventoryLogResponse response = new InventoryLogResponse();
        response.setId(log.getId());
        response.setIngredientId(log.getIngredient() != null ? log.getIngredient().getId() : null);
        response.setIngredientName(log.getIngredient() != null ? log.getIngredient().getName() : null);
        response.setType(log.getType());
        response.setQuantity(log.getQuantity());
        response.setNote(log.getNote());
        response.setUsername(log.getUser() != null ? log.getUser().getUsername() : null);
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
