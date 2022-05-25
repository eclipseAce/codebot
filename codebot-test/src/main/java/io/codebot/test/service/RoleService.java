package io.codebot.test.service;

import io.codebot.apt.annotation.AutoCrud;
import io.codebot.apt.annotation.AutoExpose;
import io.codebot.apt.annotation.Exposed;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@AutoCrud(entity = Role.class)
@AutoExpose
@Exposed(path = "/api/role")
public interface RoleService {

    @Exposed(title = "创建角色")
    long create(@Exposed.Body RoleCreate dto);

}
