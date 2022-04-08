package template.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import io.cruder.apt.Replace;
import io.cruder.apt.Replica;
import io.cruder.apt.Template;

@Template
@Replica(name = "generated.dao.UserRepository", replaces = {
        @Replace(type = "type", args = { "template[.]crud[.]TEntity[$]Wrapper[$]Id", "java.lang.Long" }),
        @Replace(type = "type", args = { "template[.]crud[.]TEntity", "io.cruder.example.domain.User" })
})
@Replica(name = "generated.dao.RoleRepository", replaces = {
        @Replace(type = "type", args = { "template[.]crud[.]TEntity[$]Wrapper[$]Id", "java.lang.Long" }),
        @Replace(type = "type", args = { "template[.]crud[.]TEntity", "io.cruder.example.domain.Role" })
})
@Repository
public interface TRepository extends
		JpaRepository<TEntity, TEntity.Wrapper.Id>,
		QuerydslPredicateExecutor<TEntity> {
}