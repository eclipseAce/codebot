package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Type implements Annotated, Modified {
    private final TypeFactory factory;
    private final Elements elementUtils;
    private final Types typeUtils;

    private final TypeMirror typeMirror;

    private final Lazy<List<Annotation>> lazyAnnotations;
    private final Lazy<List<Type>> lazyTypeArguments;
    private final Lazy<List<Variable>> lazyFields;
    private final Lazy<List<Executable>> lazyMethods;
    private final Lazy<List<GetAccessor>> lazyGetters;
    private final Lazy<List<SetAccessor>> lazySetters;

    Type(TypeFactory factory, TypeMirror typeMirror) {
        this.factory = factory;
        this.elementUtils = factory.elementUtils();
        this.typeUtils = factory.typeUtils();
        this.typeMirror = typeMirror;
        this.lazyAnnotations = lazyAnnotations();
        this.lazyTypeArguments = lazyTypeArguments();
        this.lazyFields = lazyFields();
        this.lazyMethods = lazyMethods();
        this.lazyGetters = lazyGetAccessors();
        this.lazySetters = lazySetAccessors();
    }

    public Types typeUtils() {
        return typeUtils;
    }

    public Elements elementUtils() {
        return elementUtils;
    }

    public TypeMirror typeMirror() {
        return typeMirror;
    }

    public List<Annotation> annotations() {
        return lazyAnnotations.get();
    }

    public Set<Modifier> modifiers() {
        return isDeclared() ? asTypeElement().getModifiers() : ImmutableSet.of();
    }

    public TypeFactory factory() {
        return factory;
    }

    public List<Type> typeArguments() {
        return lazyTypeArguments.get();
    }

    public List<Variable> fields() {
        return lazyFields.get();
    }

    public List<Executable> methods() {
        return lazyMethods.get();
    }

    public List<GetAccessor> getters() {
        return lazyGetters.get();
    }

    public List<SetAccessor> setters() {
        return lazySetters.get();
    }

    public Optional<GetAccessor> findGetter(String accessedName, Type accessedType) {
        return findGetter(accessedName, accessedType.typeMirror());
    }

    public Optional<GetAccessor> findGetter(String accessedName, TypeMirror accessedType) {
        return getters().stream()
                .filter(it -> it.accessedName().equals(accessedName)
                        && it.accessedType().isAssignableTo(accessedType))
                .findFirst();
    }

    public Optional<SetAccessor> findSetter(String accessedName, Type accessedType) {
        return findSetter(accessedName, accessedType.typeMirror());
    }

    public Optional<SetAccessor> findSetter(String accessedName, TypeMirror accessedType) {
        return setters().stream()
                .filter(it -> it.accessedName().equals(accessedName)
                        && it.accessedType().isAssignableFrom(accessedType))
                .findFirst();
    }

    public DeclaredType asDeclaredType() {
        if (!isDeclared()) {
            throw new IllegalStateException("Not DeclaredType");
        }
        return (DeclaredType) typeMirror();
    }

    public TypeElement asTypeElement() {
        return (TypeElement) asDeclaredType().asElement();
    }

    public boolean isInterface() {
        return isDeclared() && asTypeElement().getKind() == ElementKind.INTERFACE;
    }

    public boolean isClass() {
        return isDeclared() && asTypeElement().getKind() == ElementKind.CLASS;
    }

    public boolean isDeclared() {
        return typeMirror().getKind() == TypeKind.DECLARED;
    }

    public boolean isVoid() {
        return typeMirror().getKind() == TypeKind.VOID;
    }

    public boolean isPrimitive() {
        return typeMirror().getKind().isPrimitive();
    }

    public boolean isWildcard() {
        return typeMirror().getKind() == TypeKind.WILDCARD;
    }

    public boolean isAssignableTo(TypeMirror type) {
        return typeUtils().isAssignable(typeMirror(), type);
    }

    public boolean isAssignableTo(TypeElement typeElement, TypeMirror... typeArgs) {
        return isAssignableTo(typeUtils().getDeclaredType(typeElement, typeArgs));
    }

    public boolean isAssignableTo(String qualifiedName, TypeMirror... typeArgs) {
        return isAssignableTo(elementUtils().getTypeElement(qualifiedName), typeArgs);
    }

    public boolean isAssignableTo(Type type) {
        return isAssignableTo(type.typeMirror());
    }

    public boolean isAssignableFrom(TypeMirror type) {
        return typeUtils().isAssignable(type, typeMirror());
    }

    public boolean isAssignableFrom(TypeElement typeElement, TypeMirror... typeArgs) {
        return isAssignableFrom(typeUtils().getDeclaredType(typeElement, typeArgs));
    }

    public boolean isAssignableFrom(String qualifiedName, TypeMirror... typeArgs) {
        return isAssignableFrom(elementUtils().getTypeElement(qualifiedName), typeArgs);
    }

    public boolean isAssignableFrom(Type type) {
        return isAssignableFrom(type.typeMirror());
    }

    public boolean isSubtype(TypeMirror type) {
        return typeUtils().isSubtype(typeMirror(), type);
    }

    public boolean isSubtype(TypeElement typeElement, TypeMirror... typeArgs) {
        return isSubtype(typeUtils().getDeclaredType(typeElement, typeArgs));
    }

    public boolean isSubtype(String qualifiedName, TypeMirror... typeArgs) {
        TypeElement typeElement = elementUtils().getTypeElement(qualifiedName);
        if (typeElement == null) {
            throw new IllegalArgumentException("No such type '" + qualifiedName + "'");
        }
        return isSubtype(typeElement, typeArgs);
    }

    public boolean isSubtype(Type type) {
        return typeUtils().isSubtype(typeMirror(), type.typeMirror());
    }

    public ExecutableType asMember(ExecutableElement executableElement) {
        return (ExecutableType) typeUtils().asMemberOf(asDeclaredType(), executableElement);
    }

    public TypeMirror asMember(Element element) {
        return typeUtils().asMemberOf(asDeclaredType(), element);
    }

    public Type erasure() {
        return factory().getType(typeUtils().erasure(typeMirror()));
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

    @Override
    public String toString() {
        return typeMirror.toString();
    }

    private Lazy<List<Annotation>> lazyAnnotations() {
        return Lazy.of(() -> {
            List<? extends AnnotationMirror> mirrors = isDeclared()
                    ? elementUtils.getAllAnnotationMirrors(asTypeElement())
                    : typeMirror.getAnnotationMirrors();
            return ImmutableList.copyOf(mirrors.stream().map(Annotation::new).iterator());
        });
    }

    private Lazy<List<Variable>> lazyFields() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> Variable.fieldsOf(this));
    }

    private Lazy<List<Executable>> lazyMethods() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> Executable.methodsOf(this));
    }

    private Lazy<List<GetAccessor>> lazyGetAccessors() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> GetAccessor.from(this));
    }

    private Lazy<List<SetAccessor>> lazySetAccessors() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> SetAccessor.from(this));
    }

    private Lazy<List<Type>> lazyTypeArguments() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> ImmutableList.copyOf(
                asDeclaredType().getTypeArguments().stream().map(factory::getType).collect(Collectors.toList())
        ));
    }
}
