package com.example.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InventoryLogTypeConverter implements AttributeConverter<InventoryLogType, String> {

	@Override
	public String convertToDatabaseColumn(InventoryLogType attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute == InventoryLogType.import_ ? "import" : attribute.name();
	}

	@Override
	public InventoryLogType convertToEntityAttribute(String db) {
		if (db == null) {
			return null;
		}
		if ("import".equalsIgnoreCase(db)) {
			return InventoryLogType.import_;
		}
		return InventoryLogType.valueOf(db.toLowerCase());
	}
}
