package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findByCreatedAtBetween(Instant from, Instant to);
    List<AuditLog> findByActionContainingIgnoreCase(String action);
    List<AuditLog> findByUserUsername(String username);
}
