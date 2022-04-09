package template.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import io.cruder.apt.Template;
import template.annotation.RoleReplica;
import template.annotation.UserReplica;

@Template({ UserReplica.class, RoleReplica.class })
@Repository
public interface TRepository extends
		JpaRepository<TEntity, TEntity.Wrapper.Id>,
		QuerydslPredicateExecutor<TEntity> {
}