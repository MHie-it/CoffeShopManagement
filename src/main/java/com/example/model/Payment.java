package com.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private ShopOrder order;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod method;

	@Column(nullable = false, precision = 12, scale = 0)
	private BigDecimal amount;

	@Column(name = "momo_ref", length = 100)
	private String momoRef;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status = PaymentStatus.pending;

	@Column(name = "paid_at")
	private Instant paidAt;
}
