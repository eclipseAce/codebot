package io.codebot.test.service;

import io.codebot.apt.annotation.Crud;
import io.codebot.apt.annotation.Expose;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@Crud(Role.class)
@Expose(path = "/api/role")
public interface RoleService {

    @Expose(title = "创建角色")
    long create(@Expose.Body RoleCreate dto);

}
