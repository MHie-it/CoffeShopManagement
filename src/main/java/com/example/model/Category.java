package com.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "sort_order")
	private Integer sortOrder = 0;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@OneToMany(mappedBy = "category")
	private List<MenuItem> menuItems = new ArrayList<>();
}
