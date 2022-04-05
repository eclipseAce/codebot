package io.cruder.example.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;

import io.cruder.example.core.ApiResult;
import io.cruder.example.dao.UserRepository;
import io.cruder.example.domain.User;
import io.cruder.example.model.UserAdd;
import io.cruder.example.model.UserApi;
import io.cruder.example.model.UserListItem;
import io.cruder.example.model.UserSetLocked;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserApiImpl implements UserApi {
	private final UserRepository userRepository;

	@Override
	public ApiResult<Long> add(UserAdd body) {
		User entity = new User();
		entity.setUsername(body.getUsername());
		entity.setPassword(body.getPassword());
		userRepository.save(entity);
		return new ApiResult<>("OK", null, entity.getId());
	}

	@Override
	public ApiResult<Void> setLocked(UserSetLocked body) {
		User entity = userRepository.getById(body.getId());
		if (entity == null) {
			return new ApiResult<>("NOT_FOUND", "user not exists", null);
		}
		entity.setLocked(body.getLocked());
		userRepository.save(entity);
		return new ApiResult<>("OK", null, null);
	}

	@Override
	public ApiResult<List<UserListItem>> list() {
		return userRepository.findAll().stream()
				.map(it -> {
					UserListItem item = new UserListItem();
					item.setId(it.getId());
					item.setUsername(it.getUsername());
					item.setLocked(it.isLocked());
					item.setCreatedAt(it.getCreatedAt());
					item.setUpdatedAt(it.getUpdatedAt());
					return item;
				})
				.collect(Collectors.collectingAndThen(
						Collectors.toList(),
						list -> new ApiResult<>("OK", null, list)));
	}

}
