package io.cruder.apt.model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.Optional;

public class ModelContext {
    public final Elements elementUtils;
    public final Types typeUtils;

    public ModelContext(ProcessingEnvironment processingEnv) {
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    public Optional<? extends AnnotationMirror> findAnnotation(AnnotatedConstruct construct,
                                                               String annotationFqn) {
        return construct.getAnnotationMirrors().stream()
                .filter(it -> ((TypeElement) it.getAnnotationType().asElement())
                        .getQualifiedName().contentEquals(annotationFqn))
                .findFirst();
    }

    public Optional<? extends AnnotationValue> findAnnotationValue(AnnotatedConstruct construct,
                                                                   String annotationFqn,
                                                                   String name) {
        return findAnnotation(construct, annotationFqn)
                .flatMap(it -> it.getElementValues().entrySet().stream()
                        .filter(e -> e.getKey().getSimpleName().contentEquals(name))
                        .findFirst())
                .map(Map.Entry::getValue);
    }

    public Optional<? extends AnnotationValue> findAnnotationValue(AnnotatedConstruct construct,
                                                                   String annotationFqn) {
        return findAnnotationValue(construct, annotationFqn, "value");
    }

    public boolean isAnnotationPresent(AnnotatedConstruct construct,
                                       String annotationFqn) {
        return findAnnotation(construct, annotationFqn).isPresent();
    }

}
