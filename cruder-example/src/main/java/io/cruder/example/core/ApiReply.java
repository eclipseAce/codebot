package io.cruder.example.core;

import lombok.*;

@Value(staticConstructor = "of")
public class ApiReply<T> {
	private String code;

	private String message;

	private T data;

	public static <T> ApiReply of(BusinessError error, T data) {
		return ApiReply.of(error.getErrorCode(), error.getErrorMessage(), data);
	}

	public static <T> ApiReply of(BusinessError error) {
		return ApiReply.of(error, null);
	}

	public static <T> ApiReply ok(T data) {
		return ApiReply.of(BusinessErrors.OK, data);
	}

	public static <T> ApiReply<T> ok() {
		return ok(null);
	}
}
