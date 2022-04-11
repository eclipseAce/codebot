package io.cruder.example.core;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BusinessErrors implements BusinessError {
    OK("Success"),
    ENTITY_NOT_FOUND("Entity not found");

    private final String defaultErrorMessage;

    @Override
    public String getErrorCode() {
        return name();
    }

    @Override
    public String getErrorMessage() {
        return defaultErrorMessage;
    }

    public BusinessError withMessage(String message) {
        return new SimpleBusinessError(getErrorCode(), message);
    }
}
