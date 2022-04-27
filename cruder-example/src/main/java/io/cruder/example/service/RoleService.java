package io.cruder.example.service;

import io.cruder.autoservice.AutoService;
import io.cruder.example.domain.Role;
import io.cruder.example.dto.role.RoleCreate;

@AutoService(Role.class)
public interface RoleService {

    long create(RoleCreate dto);

}
