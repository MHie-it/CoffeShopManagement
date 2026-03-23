package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.OrderStatus;
import com.example.coffeshopManagement.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Integer> {
    List<ShopOrder> findByStatus(OrderStatus status);
    List<ShopOrder> findByStatusAndCreatedAtBetween(OrderStatus status, Instant from, Instant to);
}
