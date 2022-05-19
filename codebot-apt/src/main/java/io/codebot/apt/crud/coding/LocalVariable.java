package io.codebot.apt.crud.coding;

import io.codebot.apt.type.Type;
import lombok.Value;

public class LocalVariable {
    private final String name;
    private final Type type;

    public LocalVariable(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}