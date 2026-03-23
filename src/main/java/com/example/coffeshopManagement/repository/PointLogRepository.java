package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Integer> {
    List<PointLog> findByCustomerId(Integer customerId);
}
