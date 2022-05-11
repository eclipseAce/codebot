package io.codebot.apt.type;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public interface TypeSupport {
    TypeMirror typeMirror();

    Types typeUtils();

    Elements elementUtils();

    default DeclaredType asDeclaredType() {
        if (!isDeclared()) {
            throw new IllegalStateException("Not DeclaredType");
        }
        return (DeclaredType) typeMirror();
    }

    default TypeElement asTypeElement() {
        return (TypeElement) asDeclaredType().asElement();
    }

    default boolean isInterface() {
        return isDeclared() && asTypeElement().getKind() == ElementKind.INTERFACE;
    }

    default boolean isClass() {
        return isDeclared() && asTypeElement().getKind() == ElementKind.CLASS;
    }

    default boolean isDeclared() {
        return typeMirror().getKind() == TypeKind.DECLARED;
    }

    default boolean isVoid() {
        return typeMirror().getKind() == TypeKind.VOID;
    }

    default boolean isPrimitive() {
        return typeMirror().getKind().isPrimitive();
    }

    default boolean isWildcard() {
        return typeMirror().getKind() == TypeKind.WILDCARD;
    }

    default boolean isAssignableTo(TypeMirror type) {
        return typeUtils().isAssignable(typeMirror(), type);
    }

    default boolean isAssignableTo(TypeElement typeElement, TypeMirror... typeArgs) {
        return isAssignableTo(typeUtils().getDeclaredType(typeElement, typeArgs));
    }

    default boolean isAssignableTo(String qualifiedName, TypeMirror... typeArgs) {
        return isAssignableTo(elementUtils().getTypeElement(qualifiedName), typeArgs);
    }

    default boolean isAssignableTo(TypeSupport type) {
        return isAssignableTo(type.typeMirror());
    }

    default boolean isAssignableFrom(TypeMirror type) {
        return typeUtils().isAssignable(type, typeMirror());
    }

    default boolean isAssignableFrom(TypeElement typeElement, TypeMirror... typeArgs) {
        return isAssignableFrom(typeUtils().getDeclaredType(typeElement, typeArgs));
    }

    default boolean isAssignableFrom(String qualifiedName, TypeMirror... typeArgs) {
        return isAssignableFrom(elementUtils().getTypeElement(qualifiedName), typeArgs);
    }

    default boolean isAssignableFrom(TypeSupport type) {
        return isAssignableFrom(type.typeMirror());
    }

    default boolean isSubtype(TypeMirror type) {
        return typeUtils().isSubtype(typeMirror(), type);
    }

    default boolean isSubtype(TypeElement typeElement, TypeMirror... typeArgs) {
        return isSubtype(typeUtils().getDeclaredType(typeElement, typeArgs));
    }

    default boolean isSubtype(String qualifiedName, TypeMirror... typeArgs) {
        TypeElement typeElement = elementUtils().getTypeElement(qualifiedName);
        if (typeElement == null) {
            throw new IllegalArgumentException("No such type '" + qualifiedName + "'");
        }
        return isSubtype(typeElement, typeArgs);
    }

    default boolean isSubtype(TypeSupport type) {
        return typeUtils().isSubtype(typeMirror(), type.typeMirror());
    }

    default ExecutableType asMember(ExecutableElement executableElement) {
        return (ExecutableType) typeUtils().asMemberOf(asDeclaredType(), executableElement);
    }

    default TypeMirror asMember(Element element) {
        return typeUtils().asMemberOf(asDeclaredType(), element);
    }
}
