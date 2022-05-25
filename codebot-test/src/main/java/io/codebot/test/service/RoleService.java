package io.codebot.test.service;

import io.codebot.apt.annotation.CrudImplement;
import io.codebot.apt.annotation.Exposed;
import io.codebot.apt.annotation.ExposeController;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@CrudImplement(entity = Role.class)
@ExposeController(tag = "角色管理", path = "/api/role")
public interface RoleService {

    @Exposed(title = "创建角色")
    long create(@Exposed.Body RoleCreate dto);
}
