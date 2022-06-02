package io.codebot.test.service;

import io.codebot.apt.EntityService;
import io.codebot.test.core.BaseService;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;

@EntityService(Role.class)
public abstract class RoleService extends BaseService {
    public abstract long create(RoleCreate dto);
}
