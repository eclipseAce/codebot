package io.cruder.example.template.crud;

public interface TConverter {
    TEntity addToEntity(TAddDTO dto);

    TListItemDTO entityToListItem(TEntity entity);
}