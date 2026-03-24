package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.dashboard.ManagerDashboardSummaryResponse;
import com.example.coffeshopManagement.service.ManagerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagerDashboardApiController {
    private final ManagerDashboardService managerDashboardService;

    public ManagerDashboardApiController(ManagerDashboardService managerDashboardService) {
        this.managerDashboardService = managerDashboardService;
    }

    @GetMapping("/api/manager/dashboard/summary")
    public ResponseEntity<ManagerDashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(managerDashboardService.getSummary());
    }
}
