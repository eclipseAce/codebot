package template.crud;

import io.cruder.apt.Template;
import io.cruder.example.core.ApiReply;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import template.crud.dto.TAddDTO;
import template.crud.dto.TDetailsDTO;
import template.crud.dto.TListItemDTO;
import template.crud.dto.TQueryDTO;

import javax.validation.Valid;

@Template
@Tag(name = "#<title>管理接口")
@RestController
@RequestMapping("/api/#<path>")
public class TController {

    @Autowired
    private TService service;

    @Operation(summary = "新增#<title>")
    @PostMapping("/add")
    public ApiReply<Long> add(@RequestBody @Valid TAddDTO body) {
        return ApiReply.ok(service.add(body));
    }

    @Operation(summary = "获取#<title>")
    @GetMapping("/get")
    public ApiReply<TDetailsDTO> get(@RequestParam("id") Long id) {
        return ApiReply.ok(service.get(id));
    }

    @Operation(summary = "删除#<title>")
    @PostMapping("/delete")
    public ApiReply<Void> delete(@RequestParam("id") Long id) {
        service.delete(id);
        return ApiReply.ok();
    }

    @Operation(summary = "分页查询#<title>")
    @PageableAsQueryParam
    @PostMapping("/page")
    public ApiReply<Page<TListItemDTO>> page(@RequestBody @Valid TQueryDTO body, Pageable pageable) {
        return ApiReply.ok(service.query(body, pageable));
    }
}