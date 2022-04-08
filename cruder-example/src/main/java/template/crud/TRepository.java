package template.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import io.cruder.apt.Replace;
import io.cruder.apt.Replica;
import io.cruder.apt.Template;
import io.cruder.example.domain.Role;
import io.cruder.example.domain.User;

@Template
@Replica(name = "generated.dao.UserRepository", replace = @Replace(types = {
		@Replace.Type(target = TEntity.Wrapper.Id.class, type = Long.class),
		@Replace.Type(target = TEntity.class, type = User.class)
}))
@Replica(name = "generated.dao.RoleRepository", replace = @Replace(types = {
		@Replace.Type(target = TEntity.Wrapper.Id.class, type = Long.class),
		@Replace.Type(target = TEntity.class, type = Role.class)
}))
@Repository
public interface TRepository extends
		JpaRepository<TEntity, TEntity.Wrapper.Id>,
		QuerydslPredicateExecutor<TEntity> {
}