package com.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 20)
	private String unit;

	@Column(name = "stock_quantity", precision = 10, scale = 2)
	private BigDecimal stockQuantity = BigDecimal.ZERO;

	@Column(name = "min_threshold", precision = 10, scale = 2)
	private BigDecimal minThreshold = BigDecimal.ZERO;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
