package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    Optional<Attendance> findByEmployeeIdAndWorkDate(Integer employeeId, LocalDate workDate);
    List<Attendance> findByWorkDateBetween(LocalDate fromDate, LocalDate toDate);
    List<Attendance> findByEmployeePhoneAndWorkDateBetween(String phone, LocalDate fromDate, LocalDate toDate);
    List<Attendance> findByWorkDate(LocalDate workDate);
}
