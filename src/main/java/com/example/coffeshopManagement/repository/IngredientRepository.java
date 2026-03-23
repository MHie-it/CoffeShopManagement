package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    List<Ingredient> findByStockQuantityLessThanEqual(java.math.BigDecimal threshold);
}
