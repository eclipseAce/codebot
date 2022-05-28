package io.codebot.test.service;

import io.codebot.test.dto.role.RoleCreate;

public interface RoleService {
    long create(RoleCreate dto);
}
