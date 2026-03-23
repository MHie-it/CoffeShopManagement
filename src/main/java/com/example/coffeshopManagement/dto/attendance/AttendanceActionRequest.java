package com.example.coffeshopManagement.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AttendanceActionRequest {
    @NotBlank
    @Size(max = 15)
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
