package io.codebot.apt.crud;

import com.squareup.javapoet.ClassName;
import io.codebot.apt.type.Type;

public class Service {
    private final Type type;
    private final ClassName typeName;
    private final ClassName implTypeName;

    public Service(Type type) {
        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
        this.implTypeName = ClassName.get(typeName.packageName(), typeName.simpleName() + "Impl");
    }

    public Type getType() {
        return type;
    }

    public ClassName getTypeName() {
        return typeName;
    }

    public ClassName getImplTypeName() {
        return implTypeName;
    }
}
