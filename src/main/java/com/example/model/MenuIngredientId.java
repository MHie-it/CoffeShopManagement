package com.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuIngredientId implements Serializable {

	@Column(name = "menu_item_id")
	private Integer menuItemId;

	@Column(name = "ingredient_id")
	private Integer ingredientId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MenuIngredientId that = (MenuIngredientId) o;
		return Objects.equals(menuItemId, that.menuItemId) && Objects.equals(ingredientId, that.ingredientId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(menuItemId, ingredientId);
	}
}
