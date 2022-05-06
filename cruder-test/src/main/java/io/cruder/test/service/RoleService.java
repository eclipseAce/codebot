package io.cruder.test.service;

import io.cruder.CrudService;
import io.cruder.test.domain.Role;
import io.cruder.test.dto.role.RoleCreate;

@CrudService(Role.class)
public interface RoleService {

    long create(RoleCreate dto);

}
