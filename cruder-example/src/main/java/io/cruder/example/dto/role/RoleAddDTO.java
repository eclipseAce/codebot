package io.cruder.example.dto.role;

import java.util.Set;

import lombok.Data;

@Data
public class RoleAddDTO {
	private String name;

	private Set<String> permissions;
}
