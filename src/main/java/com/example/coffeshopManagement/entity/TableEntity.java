package com.example.coffeshopManagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tables")
@Getter
@Setter
@NoArgsConstructor
public class TableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = 20)
	private String name;

	@Column
	private Integer capacity = 4;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TableStatus status = TableStatus.empty;

	@Column(columnDefinition = "TEXT")
	private String note;
}
