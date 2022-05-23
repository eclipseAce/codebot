package io.codebot.apt.code;

import io.codebot.apt.type.Type;
import lombok.Value;

public final class Variables {
    private Variables() {
    }

    public static Variable of(Type type, String name) {
        return new VariableImpl(type, name);
    }

    @Value
    private static class VariableImpl implements Variable {
        Type type;
        String name;
    }
}
