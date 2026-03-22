package com.example.coffeshopManagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "attendance",
		uniqueConstraints = @UniqueConstraint(name = "uq_attendance", columnNames = {"user_id", "work_date"})
)
@Getter
@Setter
@NoArgsConstructor
public class Attendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;

	@Column(name = "check_in")
	private LocalDateTime checkIn;

	@Column(name = "check_out")
	private LocalDateTime checkOut;

	@Column(name = "hours_worked", insertable = false, updatable = false, precision = 4, scale = 2)
	private BigDecimal hoursWorked;

	@Column(columnDefinition = "TEXT")
	private String note;
}
