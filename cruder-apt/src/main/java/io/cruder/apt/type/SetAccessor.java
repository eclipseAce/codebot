package io.cruder.apt.type;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetAccessor implements Accessor {
    private static final String SETTER_PREFIX = "set";

    private final String accessedName;
    private final Type accessedType;
    private final ExecutableElement executableElement;

    protected SetAccessor(String accessedName, Type accessedType, ExecutableElement executableElement) {
        this.accessedName = accessedName;
        this.accessedType = accessedType;
        this.executableElement = executableElement;
    }

    @Override
    public String accessedName() {
        return accessedName;
    }

    @Override
    public Type accessedType() {
        return accessedType;
    }

    public String simpleName() {
        return executableElement.getSimpleName().toString();
    }

    public boolean isAssignableFrom(TypeMirror type) {
        return accessedType.isAssignableFrom(type);
    }

    public static List<SetAccessor> of(Type type) {
        return type.methods().stream().flatMap(method -> {
            String methodName = method.getSimpleName().toString();
            ExecutableType methodType = type.asMember(method);
            if (methodName.length() > SETTER_PREFIX.length()
                    && methodName.startsWith(SETTER_PREFIX)
                    && method.getParameters().size() == 1) {
                return Stream.of(new SetAccessor(
                        StringUtils.uncapitalize(methodName.substring(SETTER_PREFIX.length())),
                        type.factory().getType(methodType.getParameterTypes().get(0)),
                        method
                ));
            }
            return Stream.empty();
        }).collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }
}