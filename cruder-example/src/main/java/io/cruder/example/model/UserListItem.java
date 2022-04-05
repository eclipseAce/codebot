package io.cruder.example.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserListItem {
	private Long id;
	
	private String username;
	
	private Boolean locked;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
}
