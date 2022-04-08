package io.cruder.example.template;

import io.cruder.apt.ReplaceStringLiteral;
import io.cruder.apt.ReplaceType;
import io.cruder.apt.ReplaceTypeName;
import io.cruder.apt.Template;
import io.cruder.example.domain.Role;
import io.cruder.example.template.crud.TController;
import io.cruder.example.template.crud.TConverter;
import io.cruder.example.template.crud.TEntity;
import io.cruder.example.template.crud.TRepository;

@Template(basePackage = "io.cruder.example.generated.role", uses = {
		TController.class,
		TRepository.class,
		TConverter.class
})
@ReplaceTypeName(regex = "T(.*)", replacement = "Role$1")
@ReplaceStringLiteral(regex = "#<path>", replacement = "role")
@ReplaceStringLiteral(regex = "#<nameCN>", replacement = "角色")
@ReplaceType(target = TEntity.Id.class, with = Long.class)
@ReplaceType(target = TEntity.class, with = Role.class)
@ReplaceType(regex = "(.+)\\.template\\.crud\\.dto\\.T(.+)DTO", replacement = "$1.dto.role.Role$2DTO")
public interface RoleCrud {

}
