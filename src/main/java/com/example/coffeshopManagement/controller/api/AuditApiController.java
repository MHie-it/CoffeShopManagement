package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.audit.AuditLogResponse;
import com.example.coffeshopManagement.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class AuditApiController {
    private final AuditLogService auditLogService;

    public AuditApiController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/admin/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username) {
        return ResponseEntity.ok(auditLogService.search(from, to, action, username));
    }
}
