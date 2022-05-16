package io.codebot.test.repository;

import io.codebot.test.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends
        JpaRepository<Role, Long>,
        JpaSpecificationExecutor<Role>,
        QuerydslPredicateExecutor<Role> {
}
