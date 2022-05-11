package io.codebot.test.service;

import io.codebot.CrudService;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@CrudService(Role.class)
public interface RoleService {

    long create(RoleCreate dto);

}
