package template.crud;

import javax.validation.Valid;

import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cruder.apt.Replace;
import io.cruder.apt.Replica;
import io.cruder.apt.Template;
import io.cruder.example.core.ApiResult;
import io.cruder.example.domain.Role;
import io.cruder.example.domain.User;
import io.cruder.example.dto.role.RoleAddDTO;
import io.cruder.example.dto.role.RoleDetailsDTO;
import io.cruder.example.dto.role.RoleListItemDTO;
import io.cruder.example.dto.role.RoleQueryDTO;
import io.cruder.example.dto.user.UserAddDTO;
import io.cruder.example.dto.user.UserDetailsDTO;
import io.cruder.example.dto.user.UserListItemDTO;
import io.cruder.example.dto.user.UserQueryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;
import template.crud.dto.TQueryDTO;

@Template
@Replica(name = "generated.api.UserController", replace = @Replace(types = {
		@Replace.Type(target = TEntity.Wrapper.Id.class, type = Long.class),
		@Replace.Type(target = TEntity.class, type = User.class),
		@Replace.Type(target = TAddDTO.class, type = UserAddDTO.class),
		@Replace.Type(target = TDetailsDTO.class, type = UserDetailsDTO.class),
		@Replace.Type(target = TListItemDTO.class, type = UserListItemDTO.class),
		@Replace.Type(target = TQueryDTO.class, type = UserQueryDTO.class),
		@Replace.Type(target = TConverter.class, name = "generated.conv.UserConverter"),
		@Replace.Type(target = TRepository.class, name = "generated.dao.UserRepository")
}, literals = {
		@Replace.Literal(regex = "#<path>", replacement = "user"),
		@Replace.Literal(regex = "#<title>", replacement = "用户")
}))
@Replica(name = "generated.api.RoleController", replace = @Replace(types = {
		@Replace.Type(target = TEntity.Wrapper.Id.class, type = Long.class),
		@Replace.Type(target = TEntity.class, type = Role.class),
		@Replace.Type(target = TAddDTO.class, type = RoleAddDTO.class),
		@Replace.Type(target = TDetailsDTO.class, type = RoleDetailsDTO.class),
		@Replace.Type(target = TListItemDTO.class, type = RoleListItemDTO.class),
		@Replace.Type(target = TQueryDTO.class, type = RoleQueryDTO.class),
		@Replace.Type(target = TConverter.class, name = "generated.conv.RoleConverter"),
		@Replace.Type(target = TRepository.class, name = "generated.dao.RoleRepository")
}, literals = {
		@Replace.Literal(regex = "#<path>", replacement = "role"),
		@Replace.Literal(regex = "#<title>", replacement = "角色")
}))
@Tag(name = "#<title>管理接口")
@RestController
@RequestMapping("/api/#<path>")
public class TController {

	@Autowired
	private TConverter converter;

	@Autowired
	private TRepository repository;

	@Operation(summary = "新增#<title>")
	@PostMapping("/add")
	public ApiResult<TEntity.Wrapper.Id> add(@RequestBody @Valid TAddDTO body) {
		TEntity entity = converter.addToEntity(body);
		repository.save(entity);
		return new ApiResult<>("OK", null, entity.getId());
	}

	@Operation(summary = "获取#<title>")
	@GetMapping("/get")
	public ApiResult<TDetailsDTO> get(@RequestParam("id") TEntity.Wrapper.Id id) {
		TEntity entity = repository.findById(id).orElse(null);
		if (entity == null) {
			return new ApiResult<>("NOT_FOUND", "#<path> not exists", null);
		}
		return new ApiResult<>("OK", null, converter.entityToDetails(entity));
	}

	@Operation(summary = "删除#<title>")
	@PostMapping("/delete")
	public ApiResult<Void> delete(@RequestParam("id") TEntity.Wrapper.Id id) {
		TEntity entity = repository.findById(id).orElse(null);
		if (entity == null) {
			return new ApiResult<>("NOT_FOUND", "#<path> not exists", null);
		}
		return new ApiResult<>("OK", null, null);
	}

	@Operation(summary = "分页查询#<title>")
	@PageableAsQueryParam
	@PostMapping("/page")
	public ApiResult<Page<TListItemDTO>> page(@RequestBody @Valid TQueryDTO body, Pageable pageable) {
		return new ApiResult<>("OK", null,
				repository.findAll(body.toPredicate(), pageable)
						.map(converter::entityToListItem));
	}
}