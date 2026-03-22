package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findByCategoryId(Integer categoryId);
    List<MenuItem> findByIsAvailableTrue();
}
