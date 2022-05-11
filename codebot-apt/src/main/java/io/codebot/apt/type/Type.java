package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.codebot.apt.util.Lazy;

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
    private final TypeFactory factory;
    private final Elements elementUtils;
    private final Types typeUtils;

    private final TypeMirror typeMirror;

    private final Lazy<List<Type>> lazyTypeArguments;
    private final Lazy<List<VariableElement>> lazyFields;
    private final Lazy<List<ExecutableElement>> lazyMethods;
    private final Lazy<List<GetAccessor>> lazyGetters;
    private final Lazy<List<SetAccessor>> lazySetters;

    Type(TypeFactory factory, TypeMirror typeMirror) {
        this.factory = factory;
        this.elementUtils = factory.elementUtils();
        this.typeUtils = factory.typeUtils();
        this.typeMirror = typeMirror;
        this.lazyTypeArguments = lazyTypeArguments();
        this.lazyFields = lazyFields();
        this.lazyMethods = lazyMethods();
        this.lazyGetters = lazyGetAccessors();
        this.lazySetters = lazySetAccessors();
    }

    public TypeFactory factory() {
        return factory;
    }

    public List<Type> typeArguments() {
        return lazyTypeArguments.get();
    }

    public List<VariableElement> fields() {
        return lazyFields.get();
    }

    public List<ExecutableElement> methods() {
        return lazyMethods.get();
    }

    public List<GetAccessor> getters() {
        return lazyGetters.get();
    }

    public List<SetAccessor> setters() {
        return lazySetters.get();
    }

    public Optional<GetAccessor> findGetter(String accessedName, Type accessedType) {
        return findGetter(accessedName, accessedType.asTypeMirror());
    }

    public Optional<GetAccessor> findGetter(String accessedName, TypeMirror accessedType) {
        return getters().stream()
                .filter(it -> it.accessedName().equals(accessedName) && it.isAssignableTo(accessedType))
                .findFirst();
    }

    public Optional<SetAccessor> findSetter(String accessedName, Type accessedType) {
        return findSetter(accessedName, accessedType.asTypeMirror());
    }

    public Optional<SetAccessor> findSetter(String accessedName, TypeMirror accessedType) {
        return setters().stream()
                .filter(it -> it.accessedName().equals(accessedName) && it.isAssignableFrom(accessedType))
                .findFirst();
    }

    public TypeMirror asTypeMirror() {
        return typeMirror;
    }

    public DeclaredType asDeclaredType() {
        return ensureDeclared();
    }

    public TypeElement asTypeElement() {
        return (TypeElement) ensureDeclared().asElement();
    }

    public boolean isDeclared() {
        return typeMirror.getKind() == TypeKind.DECLARED;
    }

    public boolean isVoid() {
        return typeMirror.getKind() == TypeKind.VOID;
    }

    public boolean isPrimitive() {
        return typeMirror.getKind().isPrimitive();
    }

    public boolean isWildcard() {
        return typeMirror.getKind() == TypeKind.WILDCARD;
    }

    public boolean isInterface() {
        return isDeclared() && asTypeElement().getKind() == ElementKind.INTERFACE;
    }

    public boolean isSubtype(TypeMirror type) {
        return typeUtils.isSubtype(typeMirror, type);
    }

    public boolean isSubtype(TypeElement typeElement, TypeMirror... typeArgs) {
        return isSubtype(typeUtils.getDeclaredType(typeElement, typeArgs));
    }

    public boolean isSubtype(String qualifiedName, TypeMirror... typeArgs) {
        TypeElement typeElement = elementUtils.getTypeElement(qualifiedName);
        if (typeElement == null) {
            throw new IllegalArgumentException("No such type '" + qualifiedName + "'");
        }
        return isSubtype(typeElement, typeArgs);
    }

    public ExecutableType asMember(ExecutableElement method) {
        return (ExecutableType) typeUtils.asMemberOf(asDeclaredType(), method);
    }

    public TypeMirror asMember(VariableElement variable) {
        return typeUtils.asMemberOf(asDeclaredType(), variable);
    }

    public boolean isAssignableTo(TypeMirror type) {
        return typeUtils.isAssignable(typeMirror, type);
    }

    public boolean isAssignableTo(TypeElement typeElement, TypeMirror... typeArgs) {
        return isAssignableTo(typeUtils.getDeclaredType(typeElement, typeArgs));
    }

    public boolean isAssignableTo(String qualifiedName, TypeMirror... typeArgs) {
        return isAssignableTo(elementUtils.getTypeElement(qualifiedName), typeArgs);
    }

    public boolean isAssignableTo(Type type) {
        return isAssignableTo(type.asTypeMirror());
    }

    public boolean isAssignableFrom(TypeMirror type) {
        return typeUtils.isAssignable(type, typeMirror);
    }

    public boolean isAssignableFrom(TypeElement typeElement, TypeMirror... typeArgs) {
        return isAssignableFrom(typeUtils.getDeclaredType(typeElement, typeArgs));
    }

    public boolean isAssignableFrom(String qualifiedName, TypeMirror... typeArgs) {
        return isAssignableFrom(elementUtils.getTypeElement(qualifiedName), typeArgs);
    }

    public boolean isAssignableFrom(Type type) {
        return isAssignableFrom(type.asTypeMirror());
    }

    public Type erasure() {
        return factory.getType(typeUtils.erasure(typeMirror));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return typeUtils.isSameType(typeMirror, ((Type) o).typeMirror);
    }

    @Override
    public int hashCode() {
        return typeMirror.hashCode();
    }

    private DeclaredType ensureDeclared() {
        if (!isDeclared()) {
            throw new IllegalStateException("Not DeclaredType");
        }
        return (DeclaredType) typeMirror;
    }

    private Lazy<List<VariableElement>> lazyFields() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> {
            List<VariableElement> fields = Lists.newArrayList();
            collectFieldsInHierarchy(ensureDeclared(), fields);
            return ImmutableList.copyOf(fields);
        });
    }

    private Lazy<List<ExecutableElement>> lazyMethods() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> {
            List<ExecutableElement> methods = Lists.newArrayList();
            collectMethodsInHierarchy(ensureDeclared(), methods, Sets.newHashSet());
            return ImmutableList.copyOf(methods);
        });
    }

    private Lazy<List<GetAccessor>> lazyGetAccessors() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> GetAccessor.of(this));
    }

    private Lazy<List<SetAccessor>> lazySetAccessors() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> SetAccessor.of(this));
    }

    private Lazy<List<Type>> lazyTypeArguments() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> ImmutableList.copyOf(
                ensureDeclared().getTypeArguments().stream().map(factory::getType).collect(Collectors.toList())
        ));
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
