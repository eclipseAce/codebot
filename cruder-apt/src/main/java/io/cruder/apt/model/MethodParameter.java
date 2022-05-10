package io.cruder.apt.model;

import io.cruder.apt.type.Type;

public class MethodParameter {
    private final String name;
    private final Type type;

    public MethodParameter(String name, Type type) {
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
