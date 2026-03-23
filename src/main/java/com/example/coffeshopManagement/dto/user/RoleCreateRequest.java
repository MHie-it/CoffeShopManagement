package com.example.coffeshopManagement.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RoleCreateRequest {
    @NotBlank
    @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "Role must match ROLE_<UPPER_CASE>")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
