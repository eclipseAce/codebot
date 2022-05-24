package io.codebot.test.service;

import io.codebot.apt.annotation.AutoCrud;
import io.codebot.apt.annotation.AutoExpose;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@AutoCrud(entity = Role.class)
@AutoExpose(path = "/api/role")
public interface RoleService {

    long create(RoleCreate dto);

}
