package io.codebot.apt.coding;

import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeOps {
    private final Elements elementUtils;
    private final Types typeUtils;

    private final Map<String, TypeElement> typeElements = Maps.newHashMap();

    public static TypeOps instanceOf(ProcessingEnvironment processingEnv) {
        return new TypeOps(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
    }

    public TypeElement getTypeElement(String fqn) {
        return typeElements.computeIfAbsent(fqn, elementUtils::getTypeElement);
    }

    public DeclaredType getDeclared(TypeElement element, TypeMirror... typeArgs) {
        return typeUtils.getDeclaredType(element, typeArgs);
    }

    public DeclaredType getDeclared(String fqn, TypeMirror... typeArgs) {
        return getDeclared(getTypeElement(fqn), typeArgs);
    }

    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        return typeUtils.isAssignable(t1, t2);
    }

    public boolean isAssignable(TypeMirror t, String fqn, TypeMirror... typeArgs) {
        return isAssignable(t, getDeclared(fqn, typeArgs));
    }

    public boolean isSame(TypeMirror t1, TypeMirror t2) {
        return typeUtils.isSameType(t1, t2);
    }

    public boolean isIterable(TypeMirror t) {
        return isAssignable(t, getDeclared(Iterable.class.getName()));
    }

    public boolean isIterableOf(TypeMirror t, TypeMirror element) {
        return isAssignable(t, getDeclared(
                Iterable.class.getName(),
                typeUtils.getWildcardType(element, null)
        ));
    }

    public boolean isCollection(TypeMirror t) {
        return isAssignable(t, getDeclared(Collection.class.getName()));
    }

    public boolean isCollectionOf(TypeMirror t, TypeMirror element) {
        return isAssignable(t, getDeclared(
                Collection.class.getName(),
                typeUtils.getWildcardType(element, null)
        ));
    }

    public boolean isList(TypeMirror t) {
        return isAssignable(t, getDeclared(List.class.getName()));
    }

    public boolean isListOf(TypeMirror t, TypeMirror element) {
        return isAssignable(t, getDeclared(
                List.class.getName(),
                typeUtils.getWildcardType(element, null)
        ));
    }

    public boolean isPrimitive(TypeMirror t) {
        return t.getKind().isPrimitive();
    }

    public boolean isVoid(TypeMirror t) {
        return t.getKind() == TypeKind.VOID;
    }

    public boolean isDeclared(TypeMirror t) {
        return t.getKind() == TypeKind.DECLARED;
    }

    public PrimitiveType getBooleanType() {
        return typeUtils.getPrimitiveType(TypeKind.BOOLEAN);
    }

    public TypeMirror boxed(TypeMirror t) {
        return isPrimitive(t) ? typeUtils.boxedClass((PrimitiveType) t).asType() : t;
    }

    public DeclaredType resolveTypeParameter(DeclaredType containing, TypeElement element, int index) {
        return (DeclaredType) typeUtils.asMemberOf(containing, element.getTypeParameters().get(index));
    }

    public DeclaredType resolveTypeParameter(DeclaredType containing, String fqn, int index) {
        return resolveTypeParameter(containing, getTypeElement(fqn), index);
    }

    public DeclaredType resolveListElementType(DeclaredType containing) {
        return resolveTypeParameter(containing, List.class.getName(), 0);
    }

    public DeclaredType resolveIterableElementType(DeclaredType containing) {
        return resolveTypeParameter(containing, Iterable.class.getName(), 0);
    }
}
