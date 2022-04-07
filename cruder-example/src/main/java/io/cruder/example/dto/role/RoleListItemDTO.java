package io.cruder.example.dto.role;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RoleListItemDTO {
	private Long id;
	
	private String name;
	
	private Boolean disabled;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
}
