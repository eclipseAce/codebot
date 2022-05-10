package io.cruder.apt.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.cruder.apt.type.Type;

public class Repository {
    private final Type type;
    private final ClassName typeName;

    public Repository(Type type) {
        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
    }

    public Type getType() {
        return type;
    }

    public ClassName getTypeName() {
        return typeName;
    }
}
