package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

public class SetAccessor extends Executable implements Accessor {
    private final String accessedName;

    SetAccessor(Type enclosingType, ExecutableElement executableElement, String accessedName) {
        super(enclosingType, executableElement);
        this.accessedName = accessedName;
    }

    @Override
    public String getAccessedName() {
        return accessedName;
    }

    @Override
    public Type getAccessedType() {
        return getParameters().get(0).getType();
    }
}