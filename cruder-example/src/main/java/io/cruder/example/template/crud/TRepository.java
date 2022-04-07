package io.cruder.example.template.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TRepository extends
		JpaRepository<TEntity, TEntity.Id>,
		QuerydslPredicateExecutor<TEntity> {
}