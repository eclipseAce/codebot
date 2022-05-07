package io.cruder.apt.model;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;

public class Type {
    private final TypeFactory typeFactory;

    private final TypeMirror typeMirror;
    private final TypeElement typeElement;

    private final Supplier<List<VariableElement>> fields;
    private final Supplier<List<ExecutableElement>> methods;
    private final Supplier<List<Accessor>> accessors;

    Type(TypeFactory typeFactory, TypeMirror typeMirror) {
        this.typeFactory = typeFactory;
        this.typeMirror = typeMirror;

        this.typeElement = isDeclared() ? (TypeElement) getTypeUtils().asElement(typeMirror) : null;

        this.fields = Suppliers.memoize(() -> isDeclared()
                ? ImmutableList.copyOf(findFields((DeclaredType) typeMirror, Lists.newArrayList()))
                : ImmutableList.of()
        );

        this.methods = Suppliers.memoize(() -> isDeclared()
                ? ImmutableList.copyOf(findMethods((DeclaredType) typeMirror, Lists.newArrayList(), Sets.newHashSet()))
                : ImmutableList.of()
        );

        this.accessors = Suppliers.memoize(() -> isDeclared()
                ? ImmutableList.copyOf(findAccessors((DeclaredType) typeMirror, methods.get()))
                : ImmutableList.of()
        );
    }

    public Elements getElementUtils() {
        return typeFactory.getElementUtils();
    }

    public Types getTypeUtils() {
        return typeFactory.getTypeUtils();
    }

    public boolean isDeclared() {
        return typeMirror.getKind() == TypeKind.DECLARED;
    }

    public boolean isPrimitive() {
        return typeMirror.getKind().isPrimitive();
    }

    public Type boxed() {
        if (isPrimitive()) {
            return typeFactory.getType(getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType());
        }
        return this;
    }

    public Type unboxed() {
        if (!isPrimitive()) {
            return typeFactory.getType(getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType());
        }
        return this;
    }

    private List<Accessor> findAccessors(DeclaredType declaredType,
                                         List<? extends ExecutableElement> methods) {
        List<Accessor> accessors = Lists.newArrayList();
        for (ExecutableElement method : methods) {
            ExecutableType methodType = (ExecutableType) getTypeUtils().asMemberOf(declaredType, method);
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
        return accessors;
    }

    private List<VariableElement> findFields(DeclaredType declaredType,
                                             List<VariableElement> collected) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            collected.addAll(ElementFilter.fieldsIn(element.getEnclosedElements()));
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                findFields((DeclaredType) element.getSuperclass(), collected);
            }
        }
        return collected;
    }

    private List<ExecutableElement> findMethods(DeclaredType declaredType,
                                                List<ExecutableElement> collected,
                                                Set<TypeElement> visited) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            visited.add(element);
            for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
                if (isPrivateOrStatic(method) || isAlreadyOverridded(collected, method)) {
                    continue;
                }
                collected.add(method);
            }
            for (TypeMirror iface : element.getInterfaces()) {
                findMethods((DeclaredType) iface, collected, visited);
            }
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                findMethods((DeclaredType) element.getSuperclass(), collected, visited);
            }
        }
        return collected;
    }

    private boolean isAlreadyOverridded(List<ExecutableElement> collected,
                                        ExecutableElement method) {
        return collected.stream().anyMatch(it -> getElementUtils()
                .overrides(it, method, (TypeElement) it.getEnclosingElement()));
    }

    private boolean isPrivateOrStatic(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.STATIC)
                || method.getModifiers().contains(Modifier.PRIVATE);
    }
}
