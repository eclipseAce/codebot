package io.cruder.example.template.crud;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cruder.example.core.ApiResult;

@RestController
@RequestMapping("/api/user")
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

	@GetMapping("/list")
	public ApiResult<List<TListItemDTO>> list() {
		return new ApiResult<>("OK", null, repository.findAll().stream()
				.map(converter::entityToListItem)
				.collect(Collectors.toList()));
	}
}