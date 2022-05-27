package io.codebot.test.service;

import io.codebot.apt.annotation.AutoExpose;
import io.codebot.apt.annotation.Exposed;
import io.codebot.test.dto.role.RoleCreate;

@AutoExpose(tag = "角色管理", path = "/api/role")
public interface RoleService {

    @Exposed(title = "创建角色")
    long create(@Exposed.Body RoleCreate dto);
}
