package io.cruder.autoservice;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Optional;

public final class ProcessingUtils {
    public final Types types;
    public final Elements elements;

    public ProcessingUtils(ProcessingEnvironment processingEnv) {
        this.types = processingEnv.getTypeUtils();
        this.elements = processingEnv.getElementUtils();
    }

    public TypeElement asTypeElement(TypeMirror type) {
        return (TypeElement) types.asElement(type);
    }

    public boolean isAssignable(TypeMirror type, String fqn, TypeMirror... typeArgs) {
        return types.isAssignable(type, types.getDeclaredType(elements.getTypeElement(fqn), typeArgs));
    }

    public boolean isTypeOfName(TypeMirror type, String fqn) {
        return types.isSameType(type, elements.getTypeElement(fqn).asType());
    }

    public boolean isSameType(TypeMirror t1, TypeMirror t2, boolean boxed) {
        if (boxed && t1.getKind().isPrimitive()) {
            t1 = types.boxedClass((PrimitiveType) t1).asType();
        }
        if (boxed && t2.getKind().isPrimitive()) {
            t2 = types.boxedClass((PrimitiveType) t2).asType();
        }
        return types.isSameType(t1, t2);
    }

    public boolean isAnnotationPresent(Element element, String annotationFqn) {
        return findAnnotation(element, annotationFqn).isPresent();
    }

    public Optional<? extends AnnotationMirror> findAnnotation(Element element, String annotationFqn) {
        return element.getAnnotationMirrors().stream()
                .filter(anno -> isTypeOfName(anno.getAnnotationType(), annotationFqn))
                .findFirst();
    }

    public Optional<? extends TypeMirror> findClassAnnotationValue(AnnotationMirror annotation, String member) {
        return annotation.getElementValues().entrySet().stream()
                .filter(it -> member.contentEquals(it.getKey().getSimpleName())
                        && (it.getValue().getValue() instanceof TypeMirror))
                .map(it -> (TypeMirror) it.getValue().getValue())
                .findFirst();
    }
}
