package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Integer> {
    List<InventoryLog> findByCreatedAtBetween(Instant from, Instant to);
    List<InventoryLog> findByIngredientId(Integer ingredientId);
}
