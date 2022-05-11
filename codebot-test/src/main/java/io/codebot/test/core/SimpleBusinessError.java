package io.codebot.test.core;

import lombok.Value;

@Value
public class SimpleBusinessError implements BusinessError {
    private final String errorCode;
    private final String errorMessage;
}
