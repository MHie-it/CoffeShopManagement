package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.dto.attendance.AttendanceResponse;
import com.example.coffeshopManagement.dto.attendance.EmployeeCreateRequest;
import com.example.coffeshopManagement.dto.attendance.EmployeeResponse;
import com.example.coffeshopManagement.dto.attendance.EmployeeUpdateRequest;
import com.example.coffeshopManagement.dto.attendance.PayrollSummaryResponse;
import com.example.coffeshopManagement.entity.Attendance;
import com.example.coffeshopManagement.entity.Employee;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.AttendanceRepository;
import com.example.coffeshopManagement.repository.EmployeeRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AttendanceService(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public List<EmployeeResponse> getEmployees() {
        syncEmployeesFromStaffUsers();
        return employeeRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(
                        b.getId() == null ? 0 : b.getId(),
                        a.getId() == null ? 0 : a.getId()))
                .map(this::toEmployeeResponse)
                .toList();
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
        String normalizedPhone = normalizePhone(phone);
        Employee employee = findOrCreateActiveEmployeeByPhone(normalizedPhone);
        User actor = findUserByUsernameOrEmail(username);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setEmployee(employee);
                    a.setUser(actor);
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
        if (attendance.getUser() == null) {
            attendance.setUser(actor);
        }
        Attendance saved = attendanceRepository.save(attendance);
        auditLogService.log(username, "ATTENDANCE_CHECK_IN", "attendance", saved.getId(),
                Map.of("employeePhone", normalizedPhone, "workDate", String.valueOf(saved.getWorkDate())));
        return toAttendanceResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOutByPhone(String phone, String username) {
        String normalizedPhone = normalizePhone(phone);
        Employee employee = findOrCreateActiveEmployeeByPhone(normalizedPhone);
        User actor = findUserByUsernameOrEmail(username);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("Bạn cần check-in trước khi check-out."));

        if (attendance.getCheckIn() == null) {
            throw new BadRequestException("Bạn cần check-in trước khi check-out.");
        }
        if (attendance.getCheckOut() != null) {
            throw new BadRequestException("Employee already checked out");
        }
        if (now.isBefore(attendance.getCheckIn())) {
            throw new BadRequestException("Check-out time is invalid");
        }

        attendance.setCheckOut(now);
        if (attendance.getUser() == null) {
            attendance.setUser(actor);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditLogService.log(username, "ATTENDANCE_CHECK_OUT", "attendance", saved.getId(),
                Map.of("employeePhone", normalizedPhone, "hoursWorked", String.valueOf(saved.getHoursWorked())));
        return toAttendanceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getTodayAttendance() {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByWorkDate(today).stream().map(this::toAttendanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceReport(LocalDate from, LocalDate to, String phone) {
        if (from == null || to == null) {
            throw new BadRequestException("from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from date must be before or equal to to date");
        }
        String normalizedPhone = phone == null ? "" : phone.trim().replace(" ", "");

        List<Attendance> records = normalizedPhone.isBlank()
                ? attendanceRepository.findByWorkDateBetween(from, to)
                : attendanceRepository.findByEmployeePhoneAndWorkDateBetween(normalizedPhone, from, to);
        return records.stream().map(this::toAttendanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollSummaryResponse> getPayrollSummary(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from date must be before or equal to to date");
        }
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

    private Employee findOrCreateActiveEmployeeByPhone(String phone) {
        Employee employee = employeeRepository.findByPhone(phone)
                .orElseGet(() -> userRepository.findByPhone(phone).map(this::createEmployeeFromUser).orElse(null));
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            throw new BadRequestException("Employee is inactive");
        }
        return employee;
    }

    private Employee createEmployeeFromUser(User user) {
        Employee employee = new Employee();
        employee.setFullName(user.getFullName());
        employee.setPhone(user.getPhone());
        employee.setPosition("Staff");
        employee.setShiftCode("FULL");
        employee.setHourlyRate(BigDecimal.ZERO);
        employee.setIsActive(user.isActive());
        return employeeRepository.save(employee);
    }

    private void syncEmployeesFromStaffUsers() {
        List<User> staffUsers = userRepository.findAll().stream()
                .filter(user -> user.getRoles() != null
                        && user.getRoles().stream().anyMatch(role -> "ROLE_STAFF".equals(role.getName())))
                .toList();
        for (User staff : staffUsers) {
            String phone = staff.getPhone();
            if (phone == null || phone.isBlank()) {
                continue;
            }
            if (employeeRepository.existsByPhone(phone)) {
                continue;
            }
            createEmployeeFromUser(staff);
        }
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim().replace(" ", "");
        if (normalized.isEmpty()) {
            throw new BadRequestException("Phone is required");
        }
        return normalized;
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));
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
