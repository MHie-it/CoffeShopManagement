package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.user.AssignRoleRequest;
import com.example.coffeshopManagement.dto.user.RoleCreateRequest;
import com.example.coffeshopManagement.dto.user.RoleResponse;
import com.example.coffeshopManagement.dto.user.UserResponse;
import com.example.coffeshopManagement.service.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminApiController {
    private final UserRoleService userRoleService;

    public AdminApiController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping("/api/admin/roles")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(userRoleService.getRoles());
    }

    @PostMapping("/api/admin/roles")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userRoleService.createRole(request));
    }

    @GetMapping("/api/admin/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userRoleService.getUsers());
    }

    @PutMapping("/api/admin/users/{userId}/roles")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Integer userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(userRoleService.assignRole(userId, request.getRoleName()));
    }
}
