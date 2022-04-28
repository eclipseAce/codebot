package io.cruder.autoservice.util;

import lombok.RequiredArgsConstructor;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Models {
    public final Types types;
    public final Elements elements;

    public String getQualifiedName(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            throw new IllegalArgumentException("Not DeclaredType: " + type);
        }
        return getQualifiedName(((DeclaredType) type).asElement());
    }

    public String getQualifiedName(Element element) {
        if (!(element instanceof QualifiedNameable)) {
            throw new IllegalArgumentException("Not QualifiedNameable element: " + element);
        }
        return ((QualifiedNameable) element).getQualifiedName().toString();
    }

    public PackageElement getPackage(Element element) {
        while (element.getKind() != ElementKind.PACKAGE) {
            element = element.getEnclosingElement();
        }
        return (PackageElement) element;
    }

    public PackageElement getParentPackage(PackageElement packageElement) {
        String fqn = getQualifiedName(packageElement);
        int sepIndex = fqn.lastIndexOf('.');
        if (sepIndex > -1) {
            return elements.getPackageElement(fqn.substring(0, sepIndex));
        }
        return null;
    }

    public String getPackageQualifiedName(Element element) {
        return getQualifiedName(getPackage(element));
    }

    public Optional<? extends AnnotationMirror> findAnnotation(
            Element element, String annotation) {
        return element.getAnnotationMirrors().stream()
                .filter(it -> getQualifiedName(it.getAnnotationType()).equals(annotation))
                .findFirst();
    }

    public Optional<? extends AnnotationValue> findAnnotationValue(
            Element element, String annotation, String member) {
        return findAnnotation(element, annotation)
                .flatMap(anno -> anno.getElementValues().entrySet().stream()
                        .filter(entry -> entry.getKey().getSimpleName().contentEquals(member))
                        .findFirst())
                .map(Map.Entry::getValue);
    }

    public Optional<? extends DeclaredType> findClassAnnotationValue(
            Element element, String annotation, String member) {
        return findAnnotationValue(element, annotation, member)
                .filter(it -> it instanceof DeclaredType)
                .map(it -> (DeclaredType) it);
    }

    public boolean isAnnotationPresent(Element element, String annotation) {
        return findAnnotation(element, annotation).isPresent();
    }

    public boolean isTypeKindOf(TypeMirror type, TypeKind ...kinds) {
        return Stream.of(kinds).anyMatch(kind -> type.getKind() == kind);
    }
}
