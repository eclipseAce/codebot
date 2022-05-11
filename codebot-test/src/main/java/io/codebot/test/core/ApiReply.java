package io.codebot.test.core;

import lombok.Value;

@Value(staticConstructor = "of")
public class ApiReply<T> {
    String code;

    String message;

    T data;

    public static <T> ApiReply<T> of(BusinessError error, T data) {
        return ApiReply.of(error.getErrorCode(), error.getErrorMessage(), data);
    }

    public static <T> ApiReply<T> of(BusinessError error) {
        return ApiReply.of(error, null);
    }

    public static <T> ApiReply<T> ok(T data) {
        return ApiReply.of(BusinessErrors.OK, data);
    }

    public static <T> ApiReply<T> ok() {
        return ok(null);
    }
}
