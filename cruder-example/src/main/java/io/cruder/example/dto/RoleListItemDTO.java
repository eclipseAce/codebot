package io.cruder.example.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RoleListItemDTO {
	private Long id;
	
	private String username;
	
	private Boolean locked;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
}
