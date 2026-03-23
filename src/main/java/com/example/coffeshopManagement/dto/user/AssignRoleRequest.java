package com.example.coffeshopManagement.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AssignRoleRequest {
    @NotBlank
    @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "Role must match ROLE_<UPPER_CASE>")
    private String roleName;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
