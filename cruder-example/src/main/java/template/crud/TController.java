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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;
import template.crud.dto.TQueryDTO;

@Template
@Replica(name = "generated.user.UserController", replaces = {
        @Replace(type = "type", args = { "template[.]crud[.]TEntity[$]Wrapper[$]Id", "java.lang.Long" }),
        @Replace(type = "type", args = { "template[.]crud[.]TEntity", "io.cruder.example.domain.User" }),
        @Replace(type = "type", args = { "template[.]crud[.]T(.+)", "generated.user.User$1" }),
        @Replace(type = "type", args = { "template[.]crud[.]dto[.]T(.+)DTO", "io.cruder.example.dto.user.User$1DTO" }),
        @Replace(type = "literal", args = { "#<path>", "user" }),
        @Replace(type = "literal", args = { "#<title>", "用户" })
})
@Replica(name = "generated.user.RoleController", replaces = {
        @Replace(type = "type", args = { "template[.]crud[.]TEntity[$]Wrapper[$]Id", "java.lang.Long" }),
        @Replace(type = "type", args = { "template[.]crud[.]TEntity", "io.cruder.example.domain.Role" }),
        @Replace(type = "type", args = { "template[.]crud[.]T(.+)", "generated.role.Role$1" }),
        @Replace(type = "type", args = { "template[.]crud[.]dto[.]T(.+)DTO", "io.cruder.example.dto.role.Role$1DTO" }),
        @Replace(type = "literal", args = { "#<path>", "role" }),
        @Replace(type = "literal", args = { "#<title>", "角色" })
})
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