package io.cruder.apt.type;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetAccessor implements Accessor {
    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";

    private final String accessedName;
    private final Type accessedType;
    private final ExecutableElement executableElement;

    protected GetAccessor(String accessedName, Type accessedType, ExecutableElement executableElement) {
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

    public boolean isAssignableTo(TypeMirror type) {
        return accessedType.isAssignableTo(type);
    }

    public static List<GetAccessor> of(Type type) {
        return type.methods().stream().flatMap(method -> {
            String methodName = method.getSimpleName().toString();
            ExecutableType methodType = type.asMember(method);
            if (methodName.length() > GETTER_PREFIX.length()
                    && methodName.startsWith(GETTER_PREFIX)
                    && method.getParameters().isEmpty()
                    && methodType.getReturnType().getKind() != TypeKind.VOID) {
                return Stream.of(new GetAccessor(
                        StringUtils.uncapitalize(methodName.substring(GETTER_PREFIX.length())),
                        type.factory().getType(methodType.getReturnType()),
                        method
                ));
            }
            if (methodName.length() > BOOLEAN_GETTER_PREFIX.length()
                    && methodName.startsWith(BOOLEAN_GETTER_PREFIX)
                    && method.getParameters().isEmpty()
                    && methodType.getReturnType().getKind() == TypeKind.BOOLEAN) {
                return Stream.of(new GetAccessor(
                        StringUtils.uncapitalize(methodName.substring(BOOLEAN_GETTER_PREFIX.length())),
                        type.factory().getType(methodType.getReturnType()),
                        method
                ));
            }
            return Stream.empty();
        }).collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }
}