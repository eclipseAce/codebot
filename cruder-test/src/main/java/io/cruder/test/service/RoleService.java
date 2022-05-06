package io.cruder.test.service;

import io.cruder.JpaService;
import io.cruder.test.domain.Role;
import io.cruder.test.dto.role.RoleCreate;

@JpaService(Role.class)
public interface RoleService {

    long create(RoleCreate dto);

}
