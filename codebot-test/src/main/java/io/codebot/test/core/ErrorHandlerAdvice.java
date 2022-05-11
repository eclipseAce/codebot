package io.codebot.test.core;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandlerAdvice {
    @ExceptionHandler
    public ApiReply<Void> handleBusinessException(BusinessException ex) {
        return ApiReply.of(ex.getError());
    }
}
