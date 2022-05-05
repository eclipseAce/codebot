package io.cruder.example.service;

import io.cruder.autoservice.annotation.EntityService;
import io.cruder.example.domain.Role;
import io.cruder.example.dto.role.RoleCreate;

@EntityService(Role.class)
public interface RoleService {

    long create(RoleCreate dto);

}
