package io.cruder.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class Accessor {
    private final ExecutableElement element;
    private final AccessorKind kind;
    private final String accessedName;
    private final TypeMirror accessedType;

    Accessor(ExecutableElement element,
             AccessorKind kind,
             String accessedName,
             TypeMirror accessedType) {
        this.element = element;
        this.kind = kind;
        this.accessedName = accessedName;
        this.accessedType = accessedType;
    }

    public ExecutableElement getElement() {
        return element;
    }

    public AccessorKind getKind() {
        return kind;
    }

    public String getAccessedName() {
        return accessedName;
    }

    public TypeMirror getAccessedType() {
        return accessedType;
    }

    public String getSimpleName() {
        return element.getSimpleName().toString();
    }

    static List<Accessor> fromMethods(TypeFactory factory,
                                      DeclaredType containing,
                                      List<ExecutableElement> methods) {
        List<Accessor> accessors = Lists.newArrayList();
        for (ExecutableElement method : methods) {
            ExecutableType methodType = (ExecutableType) factory.getTypeUtils().asMemberOf(containing, method);
            String methodName = method.getSimpleName().toString();
            if (methodName.length() > 3
                    && methodName.startsWith("get")
                    && method.getParameters().isEmpty()
                    && methodType.getReturnType().getKind() != TypeKind.VOID) {
                accessors.add(new Accessor(
                        method,
                        AccessorKind.READ,
                        StringUtils.uncapitalize(methodName.substring(3)),
                        methodType.getReturnType()
                ));
            } //
            else if (methodName.length() > 2
                    && methodName.startsWith("is")
                    && method.getParameters().isEmpty()
                    && methodType.getReturnType().getKind() == TypeKind.BOOLEAN) {
                accessors.add(new Accessor(
                        method,
                        AccessorKind.READ,
                        StringUtils.uncapitalize(methodName.substring(2)),
                        methodType.getReturnType()
                ));
            } //
            else if (methodName.length() > 3
                    && methodName.startsWith("set")
                    && method.getParameters().size() == 1) {
                accessors.add(new Accessor(
                        method,
                        AccessorKind.WRITE,
                        StringUtils.uncapitalize(methodName.substring(3)),
                        methodType.getParameterTypes().get(0)
                ));
            }
        }
        return ImmutableList.copyOf(accessors);
    }
}
