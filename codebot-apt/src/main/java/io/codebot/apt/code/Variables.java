package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Variables {

    public static Variable of(Type type, String name) {
        return new VariableImpl(type, name);
    }

    @Value
    private static class VariableImpl implements Variable {
        Type type;
        String name;
    }
}
