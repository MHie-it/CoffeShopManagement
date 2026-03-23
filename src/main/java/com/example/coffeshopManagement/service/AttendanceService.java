package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.attendance.AttendanceResponse;
import com.example.coffeshopManagement.dto.attendance.EmployeeCreateRequest;
import com.example.coffeshopManagement.dto.attendance.EmployeeResponse;
import com.example.coffeshopManagement.dto.attendance.EmployeeUpdateRequest;
import com.example.coffeshopManagement.dto.attendance.PayrollSummaryResponse;
import com.example.coffeshopManagement.entity.Attendance;
import com.example.coffeshopManagement.entity.Employee;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.AttendanceRepository;
import com.example.coffeshopManagement.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    public AttendanceService(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.auditLogService = auditLogService;
    }

    public EmployeeResponse createEmployee(EmployeeCreateRequest request, String username) {
        if (employeeRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone already exists");
        }

        Employee employee = new Employee();
        employee.setFullName(request.getFullName());
        employee.setPhone(request.getPhone());
        employee.setPosition(request.getPosition());
        employee.setShiftCode(request.getShiftCode());
        employee.setHourlyRate(request.getHourlyRate() == null ? BigDecimal.ZERO : request.getHourlyRate());
        employee.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        Employee saved = employeeRepository.save(employee);
        auditLogService.log(username, "EMPLOYEE_CREATE", "employees", saved.getId(),
                Map.of("fullName", saved.getFullName(), "phone", saved.getPhone()));
        return toEmployeeResponse(saved);
    }

    public List<EmployeeResponse> getEmployees() {
        return employeeRepository.findAll().stream().map(this::toEmployeeResponse).toList();
    }

    public EmployeeResponse updateEmployee(Integer employeeId, EmployeeUpdateRequest request, String username) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (request.getPhone() != null && !request.getPhone().equals(employee.getPhone())
                && employeeRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone already exists");
        }

        if (request.getFullName() != null) employee.setFullName(request.getFullName());
        if (request.getPhone() != null) employee.setPhone(request.getPhone());
        if (request.getPosition() != null) employee.setPosition(request.getPosition());
        if (request.getShiftCode() != null) employee.setShiftCode(request.getShiftCode());
        if (request.getHourlyRate() != null) employee.setHourlyRate(request.getHourlyRate());
        if (request.getIsActive() != null) employee.setIsActive(request.getIsActive());

        Employee saved = employeeRepository.save(employee);
        auditLogService.log(username, "EMPLOYEE_UPDATE", "employees", saved.getId(),
                Map.of("phone", saved.getPhone(), "position", String.valueOf(saved.getPosition())));
        return toEmployeeResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkInByPhone(String phone, String username) {
        Employee employee = findActiveEmployeeByPhone(phone);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setEmployee(employee);
                    a.setWorkDate(today);
                    return a;
                });

        if (attendance.getCheckIn() != null && attendance.getCheckOut() == null) {
            throw new BadRequestException("Employee already checked in and not checked out");
        }
        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            throw new BadRequestException("Employee already completed attendance for today");
        }

        attendance.setCheckIn(now);
        Attendance saved = attendanceRepository.save(attendance);
        auditLogService.log(username, "ATTENDANCE_CHECK_IN", "attendance", saved.getId(),
                Map.of("employeePhone", phone, "workDate", String.valueOf(saved.getWorkDate())));
        return toAttendanceResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOutByPhone(String phone, String username) {
        Employee employee = findActiveEmployeeByPhone(phone);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("Employee has not checked in today"));

        if (attendance.getCheckIn() == null) {
            throw new BadRequestException("Employee has not checked in today");
        }
        if (attendance.getCheckOut() != null) {
            throw new BadRequestException("Employee already checked out");
        }
        if (now.isBefore(attendance.getCheckIn())) {
            throw new BadRequestException("Check-out time is invalid");
        }

        attendance.setCheckOut(now);
        long minutes = Duration.between(attendance.getCheckIn(), now).toMinutes();
        BigDecimal hoursWorked = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        attendance.setHoursWorked(hoursWorked);

        Attendance saved = attendanceRepository.save(attendance);
        auditLogService.log(username, "ATTENDANCE_CHECK_OUT", "attendance", saved.getId(),
                Map.of("employeePhone", phone, "hoursWorked", String.valueOf(saved.getHoursWorked())));
        return toAttendanceResponse(saved);
    }

    public List<AttendanceResponse> getTodayAttendance() {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByWorkDate(today).stream().map(this::toAttendanceResponse).toList();
    }

    public List<AttendanceResponse> getAttendanceReport(LocalDate from, LocalDate to, String phone) {
        if (from == null || to == null) {
            throw new BadRequestException("from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from date must be before or equal to to date");
        }

        List<Attendance> records = (phone == null || phone.isBlank())
                ? attendanceRepository.findByWorkDateBetween(from, to)
                : attendanceRepository.findByEmployeePhoneAndWorkDateBetween(phone, from, to);
        return records.stream().map(this::toAttendanceResponse).toList();
    }

    public List<PayrollSummaryResponse> getPayrollSummary(LocalDate from, LocalDate to) {
        List<Attendance> records = attendanceRepository.findByWorkDateBetween(from, to);
        Map<Integer, PayrollSummaryResponse> map = new java.util.LinkedHashMap<>();

        for (Attendance a : records) {
            Integer employeeId = a.getEmployee().getId();
            PayrollSummaryResponse row = map.computeIfAbsent(employeeId, id -> {
                PayrollSummaryResponse r = new PayrollSummaryResponse();
                r.setEmployeeId(id);
                r.setFullName(a.getEmployee().getFullName());
                r.setPhone(a.getEmployee().getPhone());
                r.setHourlyRate(a.getEmployee().getHourlyRate() == null ? BigDecimal.ZERO : a.getEmployee().getHourlyRate());
                r.setTotalHours(BigDecimal.ZERO);
                r.setSalaryAmount(BigDecimal.ZERO);
                return r;
            });

            BigDecimal hours = a.getHoursWorked() == null ? BigDecimal.ZERO : a.getHoursWorked();
            row.setTotalHours(row.getTotalHours().add(hours));
            row.setSalaryAmount(row.getTotalHours().multiply(row.getHourlyRate()).setScale(2, RoundingMode.HALF_UP));
        }
        return new java.util.ArrayList<>(map.values());
    }

    private Employee findActiveEmployeeByPhone(String phone) {
        Employee employee = employeeRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            throw new BadRequestException("Employee is inactive");
        }
        return employee;
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setFullName(employee.getFullName());
        response.setPhone(employee.getPhone());
        response.setPosition(employee.getPosition());
        response.setShiftCode(employee.getShiftCode());
        response.setHourlyRate(employee.getHourlyRate());
        response.setIsActive(employee.getIsActive());
        return response;
    }

    private AttendanceResponse toAttendanceResponse(Attendance attendance) {
        AttendanceResponse response = new AttendanceResponse();
        response.setId(attendance.getId());
        response.setEmployeeId(attendance.getEmployee().getId());
        response.setEmployeeName(attendance.getEmployee().getFullName());
        response.setPhone(attendance.getEmployee().getPhone());
        response.setWorkDate(attendance.getWorkDate());
        response.setCheckIn(attendance.getCheckIn());
        response.setCheckOut(attendance.getCheckOut());
        response.setHoursWorked(attendance.getHoursWorked());
        response.setNote(attendance.getNote());
        return response;
    }
}
