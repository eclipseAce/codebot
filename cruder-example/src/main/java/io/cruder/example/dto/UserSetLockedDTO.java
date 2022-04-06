package io.cruder.example.dto;

import lombok.Data;

@Data
public class UserSetLockedDTO {
	private Long id;

	private Boolean locked;
}
