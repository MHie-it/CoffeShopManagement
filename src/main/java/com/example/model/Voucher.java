package com.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
public class Voucher {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VoucherType type;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal value;

	@Column(name = "points_required")
	private Integer pointsRequired = 0;

	@Column(name = "is_used")
	private Boolean isUsed = Boolean.FALSE;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
