package com.example.coffeshopManagement.controller.api;

import com.example.coffeshopManagement.dto.attendance.AttendanceActionRequest;
import com.example.coffeshopManagement.dto.attendance.AttendanceResponse;
import com.example.coffeshopManagement.dto.attendance.EmployeeCreateRequest;
import com.example.coffeshopManagement.dto.attendance.EmployeeResponse;
import com.example.coffeshopManagement.dto.attendance.EmployeeUpdateRequest;
import com.example.coffeshopManagement.dto.attendance.PayrollSummaryResponse;
import com.example.coffeshopManagement.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class AttendanceApiController {
    private final AttendanceService attendanceService;

    public AttendanceApiController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/api/manager/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createEmployee(request, authentication.getName()));
    }

    @GetMapping("/api/manager/employees")
    public ResponseEntity<List<EmployeeResponse>> getEmployees() {
        return ResponseEntity.ok(attendanceService.getEmployees());
    }

    @PutMapping("/api/manager/employees/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Integer employeeId,
            @Valid @RequestBody EmployeeUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(attendanceService.updateEmployee(employeeId, request, authentication.getName()));
    }

    @PostMapping("/api/staff/attendance/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(
            @Valid @RequestBody AttendanceActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(attendanceService.checkInByPhone(request.getPhone(), authentication.getName()));
    }

    @PostMapping("/api/staff/attendance/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(
            @Valid @RequestBody AttendanceActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(attendanceService.checkOutByPhone(request.getPhone(), authentication.getName()));
    }

    @GetMapping("/api/manager/attendance/today")
    public ResponseEntity<List<AttendanceResponse>> getTodayAttendance() {
        return ResponseEntity.ok(attendanceService.getTodayAttendance());
    }

    @GetMapping("/api/manager/attendance/report")
    public ResponseEntity<List<AttendanceResponse>> getReport(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(attendanceService.getAttendanceReport(from, to, phone));
    }

    @GetMapping("/api/manager/payroll/summary")
    public ResponseEntity<List<PayrollSummaryResponse>> payrollSummary(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(attendanceService.getPayrollSummary(from, to));
    }
}
