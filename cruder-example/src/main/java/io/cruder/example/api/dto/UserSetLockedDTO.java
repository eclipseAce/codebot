package io.cruder.example.api.dto;

import lombok.Data;

@Data
public class UserSetLockedDTO {
	private Long id;

	private Boolean locked;
}
