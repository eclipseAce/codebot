package io.codebot.test.core;

public interface BusinessError {
    String getErrorCode();

    String getErrorMessage();

    default BusinessException toException() {
        return new BusinessException(this);
    }

    default BusinessException toException(Throwable cause) {
        return new BusinessException(this, cause);
    }
}
