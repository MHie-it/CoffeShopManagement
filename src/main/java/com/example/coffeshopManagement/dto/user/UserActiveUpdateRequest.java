package com.example.coffeshopManagement.dto.user;

import jakarta.validation.constraints.NotNull;

public class UserActiveUpdateRequest {
    @NotNull
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
