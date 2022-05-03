package io.cruder.autoservice;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Optional;

public class ProcessingContext {
    public final Types types;
    public final Elements elements;
    public final Filer filer;

    public ProcessingContext(ProcessingEnvironment processingEnv) {
        this.types = processingEnv.getTypeUtils();
        this.elements = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
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
