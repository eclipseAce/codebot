package io.cruder.example.template;

import io.cruder.apt.ReplaceName;
import io.cruder.apt.ReplaceType;
import io.cruder.apt.Template;
import io.cruder.example.domain.Role;
import io.cruder.example.dto.RoleAddDTO;
import io.cruder.example.dto.RoleListItemDTO;
import io.cruder.example.template.crud.TAddDTO;
import io.cruder.example.template.crud.TController;
import io.cruder.example.template.crud.TConverter;
import io.cruder.example.template.crud.TEntity;
import io.cruder.example.template.crud.TListItemDTO;
import io.cruder.example.template.crud.TRepository;

@Template(basePackage = "io.cruder.example.generated.role", uses = {
		TController.class,
		TRepository.class,
		TConverter.class
})
@ReplaceName(regex = "T(.*)", replacement = "Role$1")
@ReplaceType(target = TEntity.Id.class, with = Long.class)
@ReplaceType(target = TEntity.class, with = Role.class)
@ReplaceType(target = TAddDTO.class, with = RoleAddDTO.class)
@ReplaceType(target = TListItemDTO.class, with = RoleListItemDTO.class)
public interface RoleCrud {

}
