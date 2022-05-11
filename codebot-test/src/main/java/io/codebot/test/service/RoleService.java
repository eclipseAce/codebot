package io.codebot.test.service;

import io.codebot.CrudService;
import io.codebot.test.domain.Role;
import io.codebot.test.dto.role.RoleCreate;
import io.codebot.test.repository.RoleRepository;

@CrudService(entity = Role.class, repository = RoleRepository.class)
public interface RoleService {

    long create(RoleCreate dto);

}
