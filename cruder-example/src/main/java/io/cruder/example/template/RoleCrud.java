package io.cruder.example.template;

import io.cruder.apt.ReplaceStringLiteral;
import io.cruder.apt.ReplaceType;
import io.cruder.apt.ReplaceTypeName;
import io.cruder.apt.Template;
import io.cruder.example.domain.Role;
import io.cruder.example.dto.role.RoleAddDTO;
import io.cruder.example.dto.role.RoleDetailsDTO;
import io.cruder.example.dto.role.RoleListItemDTO;
import io.cruder.example.dto.role.RoleQueryDTO;
import io.cruder.example.template.crud.TController;
import io.cruder.example.template.crud.TConverter;
import io.cruder.example.template.crud.TEntity;
import io.cruder.example.template.crud.TRepository;
import io.cruder.example.template.crud.dto.TAddDTO;
import io.cruder.example.template.crud.dto.TDetailsDTO;
import io.cruder.example.template.crud.dto.TListItemDTO;
import io.cruder.example.template.crud.dto.TQueryDTO;

@Template(basePackage = "io.cruder.example.generated.role", uses = {
		TController.class,
		TRepository.class,
		TConverter.class
})
@ReplaceTypeName(regex = "T(.*)", replacement = "Role$1")
@ReplaceStringLiteral(regex = "#<path>", replacement = "role")
@ReplaceType(target = TEntity.Id.class, with = Long.class)
@ReplaceType(target = TEntity.class, with = Role.class)
@ReplaceType(target = TAddDTO.class, with = RoleAddDTO.class)
@ReplaceType(target = TListItemDTO.class, with = RoleListItemDTO.class)
@ReplaceType(target = TDetailsDTO.class, with = RoleDetailsDTO.class)
@ReplaceType(target = TQueryDTO.class, with = RoleQueryDTO.class)
public interface RoleCrud {

}
