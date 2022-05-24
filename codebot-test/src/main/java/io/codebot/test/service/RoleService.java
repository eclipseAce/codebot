package io.codebot.test.service;

import io.codebot.AutoCrud;
import io.codebot.AutoExposed;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@AutoCrud(Role.class)
@AutoExposed("/api/role")
public interface RoleService {

    long create(RoleCreate dto);

}
