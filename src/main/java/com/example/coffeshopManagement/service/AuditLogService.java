package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.audit.AuditLogResponse;
import com.example.coffeshopManagement.entity.AuditLog;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.AuditLogRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(String username, String action, String tableName, Integer recordId, Map<String, Object> detail) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new ResourceNotFoundException("Audit user not found")));
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> search(Instant from, Instant to, String action, String username) {
        List<AuditLog> logs;
        if (from != null && to != null) {
            logs = auditLogRepository.findByCreatedAtBetween(from, to);
        } else if (action != null && !action.isBlank()) {
            logs = auditLogRepository.findByActionContainingIgnoreCase(action);
        } else if (username != null && !username.isBlank()) {
            logs = auditLogRepository.findByUserUsername(username);
        } else {
            logs = auditLogRepository.findAll();
        }
        return logs.stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setUsername(log.getUser().getUsername());
        response.setAction(log.getAction());
        response.setTableName(log.getTableName());
        response.setRecordId(log.getRecordId());
        response.setDetail(log.getDetail());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
