package com.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class MenuIngredient {

	@EmbeddedId
	private MenuIngredientId id;

	@MapsId("menuItemId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_item_id", nullable = false)
	private MenuItem menuItem;

	@MapsId("ingredientId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ingredient_id", nullable = false)
	private Ingredient ingredient;

	@Column(name = "quantity_used", nullable = false, precision = 10, scale = 2)
	private BigDecimal quantityUsed;
}
