package template.crud;

import org.mapstruct.Mapper;

import io.cruder.apt.WrapMapper;
import io.cruder.apt.Template;
import template.annotation.RoleReplica;
import template.annotation.UserReplica;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;

@Template({ UserReplica.class, RoleReplica.class })
@WrapMapper(@Mapper)
public interface TConverter {
	TEntity addToEntity(TAddDTO dto);

	TListItemDTO entityToListItem(TEntity entity);

	TDetailsDTO entityToDetails(TEntity entity);
}