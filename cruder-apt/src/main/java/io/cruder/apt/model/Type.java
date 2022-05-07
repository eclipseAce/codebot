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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;

public class Type {
    private final TypeFactory typeFactory;
    private final Elements elementUtils;
    private final Types typeUtils;

    private final TypeMirror typeMirror;
    private final TypeElement typeElement;

    private final Supplier<List<VariableElement>> fields;
    private final Supplier<List<ExecutableElement>> methods;
    private final Supplier<List<Accessor>> accessors;

    Type(TypeFactory typeFactory, TypeMirror typeMirror) {
        this.typeFactory = typeFactory;
        this.elementUtils = typeFactory.getElementUtils();
        this.typeUtils = typeFactory.getTypeUtils();

        this.typeMirror = typeMirror;

        if (typeMirror.getKind() == TypeKind.DECLARED) {
            this.typeElement = (TypeElement) typeUtils.asElement(typeMirror);
            this.fields = Suppliers.memoize(() -> {
                List<VariableElement> fields = Lists.newArrayList();
                collectFieldsInHierarchy((DeclaredType) typeMirror, fields);
                return ImmutableList.copyOf(fields);
            });
            this.methods = Suppliers.memoize(() -> {
                List<ExecutableElement> methods = Lists.newArrayList();
                collectMethodsInHierarchy((DeclaredType) typeMirror, methods, Sets.newHashSet());
                return ImmutableList.copyOf(methods);
            });
            this.accessors = Suppliers.memoize(() -> {
                return findAccessors((DeclaredType) typeMirror, methods.get());
            });
        } else {
            this.typeElement = null;
            this.fields = Suppliers.memoize(ImmutableList::of);
            this.methods = Suppliers.memoize(ImmutableList::of);
            this.accessors = Suppliers.memoize(ImmutableList::of);
        }
    }

    public TypeFactory getTypeFactory() {
        return typeFactory;
    }

    private List<Accessor> findAccessors(DeclaredType declaredType,
                                         List<? extends ExecutableElement> methods) {
        List<Accessor> accessors = Lists.newArrayList();
        for (ExecutableElement method : methods) {
            ExecutableType methodType = (ExecutableType) typeUtils.asMemberOf(declaredType, method);
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

    private void collectFieldsInHierarchy(DeclaredType declaredType,
                                          List<VariableElement> collected) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
                    .filter(field -> {
                        return !field.getModifiers().contains(Modifier.STATIC);
                    })
                    .forEach(collected::add);
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectFieldsInHierarchy((DeclaredType) element.getSuperclass(), collected);
            }
        }
    }

    private void collectMethodsInHierarchy(DeclaredType declaredType,
                                           List<ExecutableElement> collected,
                                           Set<TypeElement> visited) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            visited.add(element);
            ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                    .filter(method -> {
                        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
                        boolean isPrivate = method.getModifiers().contains(Modifier.PRIVATE);
                        boolean isOverridden = collected.stream().anyMatch(it -> {
                            return elementUtils.overrides(it, method, (TypeElement) it.getEnclosingElement());
                        });
                        return !isStatic && !isPrivate && !isOverridden;
                    })
                    .forEach(collected::add);

            element.getInterfaces().forEach(it -> {
                collectMethodsInHierarchy((DeclaredType) it, collected, visited);
            });

            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectMethodsInHierarchy((DeclaredType) element.getSuperclass(), collected, visited);
            }
        }
    }
}
