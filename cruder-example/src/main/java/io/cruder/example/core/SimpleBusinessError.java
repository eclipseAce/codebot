package io.cruder.example.core;

import lombok.Value;

@Value
public class SimpleBusinessError implements BusinessError {
    private final String errorCode;
    private final String errorMessage;
}
