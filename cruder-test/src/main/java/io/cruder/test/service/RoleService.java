package io.cruder.test.service;

import io.cruder.EntityService;
import io.cruder.test.domain.Role;
import io.cruder.test.dto.role.RoleCreate;

@EntityService(Role.class)
public interface RoleService {

    long create(RoleCreate dto);

}
