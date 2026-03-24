package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.audit.AuditLogResponse;
import com.example.coffeshopManagement.entity.AuditLog;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.AuditLogRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
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

    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(Instant from, Instant to, String action, String username) {
        String actionFilter = action == null ? "" : action.trim().toLowerCase();
        String usernameFilter = username == null ? "" : username.trim().toLowerCase();

        return auditLogRepository.findAll().stream()
                .filter(log -> from == null || (log.getCreatedAt() != null && !log.getCreatedAt().isBefore(from)))
                .filter(log -> to == null || (log.getCreatedAt() != null && !log.getCreatedAt().isAfter(to)))
                .filter(log -> actionFilter.isBlank() || containsIgnoreCase(log.getAction(), actionFilter))
                .filter(log -> usernameFilter.isBlank()
                        || (log.getUser() != null && containsIgnoreCase(log.getUser().getUsername(), usernameFilter)))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setUsername(log.getUser() == null ? "unknown" : log.getUser().getUsername());
        response.setAction(log.getAction());
        response.setTableName(log.getTableName());
        response.setRecordId(log.getRecordId());
        response.setDetail(log.getDetail());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private boolean containsIgnoreCase(String value, String keywordLowerCase) {
        if (value == null || keywordLowerCase == null) {
            return false;
        }
        return value.toLowerCase().contains(keywordLowerCase);
    }
}
