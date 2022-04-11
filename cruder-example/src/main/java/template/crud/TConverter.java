package template.crud;

import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;

public interface TConverter {
    TEntity addToEntity(TAddDTO dto);

    TListItemDTO entityToListItem(TEntity entity);

    TDetailsDTO entityToDetails(TEntity entity);
}