package template.crud;

import io.cruder.apt.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Template
@Repository
public interface TRepository extends JpaRepository<TEntity, Long>, QuerydslPredicateExecutor<TEntity> {
}