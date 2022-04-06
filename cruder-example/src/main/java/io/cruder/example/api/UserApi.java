package io.cruder.example.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.cruder.example.core.ApiResult;
import io.cruder.example.dto.UserAddDTO;
import io.cruder.example.dto.UserListItemDTO;
import io.cruder.example.dto.UserSetLockedDTO;

@ResponseBody
@RequestMapping("/api/user")
public interface UserApi {

	@PostMapping("/add")
	ApiResult<Long> add(@RequestBody UserAddDTO body);

	@PostMapping("/setLocked")
	ApiResult<Void> setLocked(@RequestBody UserSetLockedDTO body);

	@GetMapping("/list")
	ApiResult<List<UserListItemDTO>> list();

}