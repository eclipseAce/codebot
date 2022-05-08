package io.cruder.apt.type;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Type {
    private final TypeFactory typeFactory;

    private final TypeMirror typeMirror;
    private final TypeElement typeElement;

    private final Supplier<List<Type>> typeArguments;

    private final Supplier<List<VariableElement>> fields;
    private final Supplier<List<ExecutableElement>> methods;
    private final Supplier<List<Accessor>> accessors;

    Type(TypeFactory typeFactory, TypeMirror typeMirror) {
        this.typeFactory = typeFactory;
        this.typeMirror = typeMirror;

        if (typeMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) typeMirror;

            this.typeElement = (TypeElement) declaredType.asElement();

            this.typeArguments = Suppliers.memoize(() -> {
                return declaredType.getTypeArguments().stream()
                        .map(typeFactory::getType)
                        .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
            });

            this.fields = Suppliers.memoize(() -> {
                List<VariableElement> fields = Lists.newArrayList();
                collectFieldsInHierarchy(declaredType, fields);
                return ImmutableList.copyOf(fields);
            });

            this.methods = Suppliers.memoize(() -> {
                List<ExecutableElement> methods = Lists.newArrayList();
                collectMethodsInHierarchy(declaredType, methods, Sets.newHashSet());
                return ImmutableList.copyOf(methods);
            });

            this.accessors = Suppliers.memoize(() -> {
                return Accessor.fromMethods(getTypeFactory(), declaredType, methods.get());
            });
        } else {
            this.typeElement = null;
            this.typeArguments = Suppliers.memoize(ImmutableList::of);
            this.fields = Suppliers.memoize(ImmutableList::of);
            this.methods = Suppliers.memoize(ImmutableList::of);
            this.accessors = Suppliers.memoize(ImmutableList::of);
        }
    }

    public TypeFactory getTypeFactory() {
        return typeFactory;
    }

    public TypeMirror getTypeMirror() {
        return typeMirror;
    }

    public DeclaredType getDeclaredType() {
        if (!isDeclared()) {
            throw new IllegalStateException("Not DeclaredType");
        }
        return (DeclaredType) typeMirror;
    }

    public TypeElement getTypeElement() {
        return typeElement;
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

    public boolean isWildcard() {
        return typeMirror.getKind() == TypeKind.WILDCARD;
    }

    public boolean isInterface() {
        return isDeclared() && typeElement.getKind() == ElementKind.INTERFACE;
    }

    public boolean isSubtype(TypeMirror type) {
        return getTypeUtils().isSubtype(typeMirror, type);
    }

    public boolean isSubtype(TypeElement typeElement, TypeMirror ...typeArgs) {
        return isSubtype(getTypeUtils().getDeclaredType(typeElement, typeArgs));
    }

    public boolean isSubtype(String qualifiedName, TypeMirror ...typeArgs) {
        TypeElement typeElement = getElementUtils().getTypeElement(qualifiedName);
        if (typeElement == null) {
            throw new IllegalArgumentException("No such type '" + qualifiedName + "'");
        }
        return isSubtype(typeElement, typeArgs);
    }

    public List<VariableElement> getFields() {
        return fields.get();
    }

    public List<VariableElement> findFields(Predicate<VariableElement> filter) {
        return getFields().stream().filter(filter).collect(Collectors.toList());
    }

    public List<ExecutableElement> getMethods() {
        return methods.get();
    }

    public List<ExecutableElement> findMethods(Predicate<ExecutableElement> filter) {
        return getMethods().stream().filter(filter).collect(Collectors.toList());
    }

    public List<Accessor> getAccessors() {
        return accessors.get();
    }

    public List<Accessor> findAccessors(Predicate<Accessor> filter) {
        return getAccessors().stream().filter(filter).collect(Collectors.toList());
    }

    public List<Accessor> findReadAccessors() {
        return findAccessors(it -> it.getKind() == AccessorKind.READ);
    }

    public List<Accessor> findWriteAccessors() {
        return findAccessors(it -> it.getKind() == AccessorKind.WRITE);
    }

    public Optional<Accessor> findReadAccessor(String name, TypeMirror type) {
        return findAccessors(it -> {
            return it.getKind() == AccessorKind.READ
                    && it.getAccessedName().equals(name)
                    && getTypeUtils().isAssignable(it.getAccessedType(), type);
        }).stream().findFirst();
    }

    public Optional<Accessor> findWriteAccessor(String name, TypeMirror type) {
        return findAccessors(it -> {
            return it.getKind() == AccessorKind.WRITE
                    && it.getAccessedName().equals(name)
                    && getTypeUtils().isAssignable(type, it.getAccessedType());
        }).stream().findFirst();
    }

    public List<Type> getTypeArguments() {
        return typeArguments.get();
    }

    public ExecutableType asMember(ExecutableElement method) {
        return (ExecutableType) getTypeUtils().asMemberOf(getDeclaredType(), method);
    }

    public TypeMirror asMember(VariableElement variable) {
        return getTypeUtils().asMemberOf(getDeclaredType(), variable);
    }

    public boolean isAssignableTo(TypeMirror type) {
        return getTypeUtils().isAssignable(typeMirror, type);
    }

    public boolean isAssignableFrom(TypeMirror type) {
        return getTypeUtils().isAssignable(type, typeMirror);
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
        if (!"java.lang.Object".contentEquals(element.getQualifiedName()) && visited.add(element)) {
            ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                    .filter(method -> {
                        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
                        boolean isPrivate = method.getModifiers().contains(Modifier.PRIVATE);
                        boolean isOverridden = collected.stream().anyMatch(it -> {
                            return getElementUtils().overrides(it, method, (TypeElement) it.getEnclosingElement());
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
