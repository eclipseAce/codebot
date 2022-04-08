package template.crud;

import io.cruder.apt.Replace;
import io.cruder.apt.Replica;
import io.cruder.apt.Template;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;

@Template
@Replica(name = "generated.conv.UserConverter", replaces = {
        @Replace(type = "type", args = { "template[.]crud[.]TEntity", "io.cruder.example.domain.User" }),
        @Replace(type = "type", args = { "template[.]crud[.]dto[.]T(.+)DTO", "io.cruder.example.dto.user.User$1DTO" })
})
@Replica(name = "generated.conv.RoleConverter", replaces = {
        @Replace(type = "type", args = { "template[.]crud[.]TEntity", "io.cruder.example.domain.Role" }),
        @Replace(type = "type", args = { "template[.]crud[.]dto[.]T(.+)DTO", "io.cruder.example.dto.role.Role$1DTO" })
})
public interface TConverter {
	TEntity addToEntity(TAddDTO dto);

	TListItemDTO entityToListItem(TEntity entity);

	TDetailsDTO entityToDetails(TEntity entity);
}