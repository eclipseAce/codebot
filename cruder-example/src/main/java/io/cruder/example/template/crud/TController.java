package io.cruder.example.template.crud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cruder.example.core.ApiResult;
import io.cruder.example.template.crud.dto.TAddDTO;
import io.cruder.example.template.crud.dto.TDetailsDTO;
import io.cruder.example.template.crud.dto.TListItemDTO;
import io.cruder.example.template.crud.dto.TQueryDTO;

@RestController
@RequestMapping("/api/#<path>")
public class TController {

	@Autowired
	private TConverter converter;

	@Autowired
	private TRepository repository;

	@PostMapping("/add")
	public ApiResult<TEntity.Id> add(TAddDTO body) {
		TEntity entity = converter.addToEntity(body);
		repository.save(entity);
		return new ApiResult<>("OK", null, entity.getId());
	}

	@GetMapping("/get")
	public ApiResult<TDetailsDTO> get(@RequestParam("id") TEntity.Id id) {
		TEntity entity = repository.findById(id).orElse(null);
		if (entity == null) {
			return new ApiResult<>("NOT_FOUND", "#<path> not exists", null);
		}
		return new ApiResult<>("OK", null, converter.entityToDetails(entity));
	}

	@PostMapping("/delete")
	public ApiResult<Void> delete(@RequestParam("id") TEntity.Id id) {
		TEntity entity = repository.getById(id);
		if (entity == null) {
			return new ApiResult<>("NOT_FOUND", "#<path> not exists", null);
		}
		return new ApiResult<>("OK", null, null);
	}

	@PostMapping("/page")
	public ApiResult<Page<TListItemDTO>> page(@RequestBody TQueryDTO body, Pageable pageable) {
		return new ApiResult<>("OK", null,
				repository.findAll(body.toPredicate(), pageable)
						.map(converter::entityToListItem));
	}
}