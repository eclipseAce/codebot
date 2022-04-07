package io.cruder.example.dto.role;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Data;

@Data
public class RoleDetailsDTO {
	private Long id;

	private String name;

	private Boolean disabled;

	private Set<String> permissions;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
