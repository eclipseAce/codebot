package io.cruder.example.template;

import io.cruder.apt.ReplaceStringLiteral;
import io.cruder.apt.ReplaceType;
import io.cruder.apt.ReplaceTypeName;
import io.cruder.apt.Template;
import io.cruder.example.domain.User;
import io.cruder.example.dto.user.UserAddDTO;
import io.cruder.example.dto.user.UserListItemDTO;
import io.cruder.example.template.crud.TController;
import io.cruder.example.template.crud.TConverter;
import io.cruder.example.template.crud.TEntity;
import io.cruder.example.template.crud.TRepository;
import io.cruder.example.template.crud.dto.TAddDTO;
import io.cruder.example.template.crud.dto.TListItemDTO;

@Template(basePackage = "io.cruder.example.generated.user", uses = {
		TController.class,
		TRepository.class,
		TConverter.class
})
@ReplaceTypeName(regex = "T(.*)", replacement = "User$1")
@ReplaceStringLiteral(regex = "#<path>", replacement = "user")
@ReplaceType(target = TEntity.Id.class, with = Long.class)
@ReplaceType(target = TEntity.class, with = User.class)
@ReplaceType(target = TAddDTO.class, with = UserAddDTO.class)
@ReplaceType(target = TListItemDTO.class, with = UserListItemDTO.class)
public interface UserCrud {

}
