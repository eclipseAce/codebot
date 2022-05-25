package io.codebot.apt.code;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import javax.lang.model.type.TypeMirror;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Variables {

    public static Variable of(TypeMirror type, String name) {
        return new VariableImpl(type, name);
    }

    @Value
    private static class VariableImpl implements Variable {
        TypeMirror type;
        String name;
    }
}
