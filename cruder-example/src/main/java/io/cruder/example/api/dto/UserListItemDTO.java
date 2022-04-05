package io.cruder.example.api.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserListItemDTO {
	private Long id;
	
	private String username;
	
	private Boolean locked;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
}
