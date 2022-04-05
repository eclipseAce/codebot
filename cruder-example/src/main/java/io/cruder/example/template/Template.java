package io.cruder.example.template;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cruder.example.core.ApiResult;

public interface Template {
	class Id {}

	class Entity {}

	class AddDTO {}

	class ListItemDTO {}

	@Repository
	interface Repo extends JpaRepository<Entity, Id> {}

	@Mapper(componentModel = "spring")
	interface Conv {
		Entity addToEntity(AddDTO dto);

		Id entityToId(Entity entity);

		ListItemDTO entityToListItem(Entity entity);
	}

	@RestController
	@RequestMapping("/api/user")
	class Api {

		@Autowired
		private Conv conv;

		@Autowired
		private Repo repo;

		@PostMapping("/add")
		public ApiResult<Id> add(AddDTO body) {
			Entity entity = conv.addToEntity(body);
			repo.save(entity);
			return new ApiResult<>("OK", null, conv.entityToId(entity));
		}

		@GetMapping("/list")
		public ApiResult<List<ListItemDTO>> list() {
			return repo.findAll().stream()
					.map(conv::entityToListItem)
					.collect(Collectors.collectingAndThen(
							Collectors.toList(),
							list -> new ApiResult<>("OK", null, list)));
		}
	}
}
