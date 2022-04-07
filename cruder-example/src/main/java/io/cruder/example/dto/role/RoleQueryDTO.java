package io.cruder.example.dto.role;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import io.cruder.example.domain.QRole;
import lombok.Data;

@Data
public class RoleQueryDTO {
	private String name;

	private Boolean disabled;

	public Predicate toPredicate() {
		BooleanBuilder bb = new BooleanBuilder();
		if (name != null) {
			bb.and(QRole.role.name.containsIgnoreCase(name));
		}
		if (disabled != null) {
			bb.and(QRole.role.disabled.eq(disabled));
		}
		return bb;
	}
}
