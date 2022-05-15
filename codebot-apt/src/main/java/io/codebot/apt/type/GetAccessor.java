package io.codebot.apt.type;

import javax.lang.model.element.ExecutableElement;

public class GetAccessor extends Executable implements Accessor {
    private final String accessedName;

    GetAccessor(Type enclosingType, ExecutableElement executableElement, String accessedName) {
        super(enclosingType, executableElement);
        this.accessedName = accessedName;
    }

    @Override
    public String getAccessedName() {
        return accessedName;
    }

    @Override
    public Type getAccessedType() {
        return getReturnType();
    }
}