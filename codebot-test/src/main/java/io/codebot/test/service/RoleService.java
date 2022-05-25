package io.codebot.test.service;

import io.codebot.apt.annotation.CrudService;
import io.codebot.apt.annotation.Exposed;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@CrudService(Role.class)
@Exposed(path = "/api/role")
public interface RoleService {

    @Exposed(title = "创建角色")
    long create(@Exposed.Body RoleCreate dto);

}
