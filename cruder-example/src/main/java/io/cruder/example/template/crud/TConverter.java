package io.cruder.example.template.crud;

import io.cruder.apt.mapstruct.Mapper;
import io.cruder.example.template.crud.dto.TAddDTO;
import io.cruder.example.template.crud.dto.TDetailsDTO;
import io.cruder.example.template.crud.dto.TListItemDTO;

@Mapper(componentModel = "spring")
public interface TConverter {
	TEntity addToEntity(TAddDTO dto);

	TListItemDTO entityToListItem(TEntity entity);

	TDetailsDTO entityToDetails(TEntity entity);
}