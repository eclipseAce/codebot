package io.cruder.example.service;

import io.cruder.example.codegen.AutoService;
import io.cruder.example.domain.Role;
import io.cruder.example.domain.User;
import io.cruder.example.dto.RoleAdd;

@AutoService(Role.class)
public interface RoleService {

    @AutoService.Creating
    long add(RoleAdd dto);

}
