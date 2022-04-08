package template.crud;

import io.cruder.apt.Replace;
import io.cruder.apt.Replica;
import io.cruder.apt.Template;
import io.cruder.example.domain.Role;
import io.cruder.example.domain.User;
import io.cruder.example.dto.role.RoleAddDTO;
import io.cruder.example.dto.role.RoleDetailsDTO;
import io.cruder.example.dto.role.RoleListItemDTO;
import io.cruder.example.dto.user.UserAddDTO;
import io.cruder.example.dto.user.UserDetailsDTO;
import io.cruder.example.dto.user.UserListItemDTO;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;

@Template
@Replica(name = "generated.conv.UserConverter", replace = @Replace(types = {
		@Replace.Type(target = TEntity.class, type = User.class),
		@Replace.Type(target = TAddDTO.class, type = UserAddDTO.class),
		@Replace.Type(target = TDetailsDTO.class, type = UserDetailsDTO.class),
		@Replace.Type(target = TListItemDTO.class, type = UserListItemDTO.class)
}))
@Replica(name = "generated.conv.RoleConverter", replace = @Replace(types = {
		@Replace.Type(target = TEntity.class, type = Role.class),
		@Replace.Type(target = TAddDTO.class, type = RoleAddDTO.class),
		@Replace.Type(target = TDetailsDTO.class, type = RoleDetailsDTO.class),
		@Replace.Type(target = TListItemDTO.class, type = RoleListItemDTO.class)
}))
public interface TConverter {
	TEntity addToEntity(TAddDTO dto);

	TListItemDTO entityToListItem(TEntity entity);

	TDetailsDTO entityToDetails(TEntity entity);
}