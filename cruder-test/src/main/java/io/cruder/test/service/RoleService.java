package io.cruder.test.service;

import io.cruder.CrudService;
import io.cruder.test.domain.Role;
import io.cruder.test.dto.role.RoleCreate;
import io.cruder.test.repository.RoleRepository;

@CrudService(entity = Role.class, repository = RoleRepository.class)
public interface RoleService {

    long create(RoleCreate dto);

}
