package io.cruder.example.template.crud.dto;

import com.querydsl.core.types.Predicate;

public interface TQueryDTO {
	Predicate toPredicate();
}
