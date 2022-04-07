package io.cruder.example.template;

import io.cruder.apt.ReplaceType;
import io.cruder.apt.Template;
import io.cruder.example.domain.User;
import io.cruder.example.dto.UserAddDTO;
import io.cruder.example.dto.UserListItemDTO;
import io.cruder.example.template.crud.TAddDTO;
import io.cruder.example.template.crud.TController;
import io.cruder.example.template.crud.TConverter;
import io.cruder.example.template.crud.TEntity;
import io.cruder.example.template.crud.TId;
import io.cruder.example.template.crud.TListItemDTO;
import io.cruder.example.template.crud.TRepository;

@Template(uses = { TController.class, TRepository.class, TConverter.class }, regex = "T(.*)", replacement = "User$1")
@ReplaceType(target = TId.class, with = Long.class)
@ReplaceType(target = TEntity.class, with = User.class)
@ReplaceType(target = TAddDTO.class, with = UserAddDTO.class)
@ReplaceType(target = TListItemDTO.class, with = UserListItemDTO.class)
public interface UserCrud {

}
