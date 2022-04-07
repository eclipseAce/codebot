package io.cruder.example.dto.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import io.cruder.example.domain.QUser;
import lombok.Data;

@Data
public class UserQueryDTO {
	private String username;

	private Boolean locked;

	public Predicate toPredicate() {
		BooleanBuilder bb = new BooleanBuilder();
		if (username != null) {
			bb.and(QUser.user.username.containsIgnoreCase(username));
		}
		if (locked != null) {
			bb.and(QUser.user.locked.eq(locked));
		}
		return bb;
	}
}
