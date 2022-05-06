package io.cruder.test.core;

import lombok.Getter;

public class BusinessException extends RuntimeException {
    @Getter
    private final BusinessError error;

    public BusinessException(BusinessError error) {
        super(error.getErrorMessage());
        this.error = error;
    }

    public BusinessException(BusinessError error, Throwable cause) {
        super(error.getErrorMessage(), cause);
        this.error = error;
    }
}
