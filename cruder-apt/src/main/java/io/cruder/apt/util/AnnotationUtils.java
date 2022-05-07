package io.cruder.apt.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import java.util.Map;
import java.util.Optional;

public class AnnotationUtils {
    public static Optional<? extends AnnotationMirror> findAnnotation(AnnotatedConstruct construct,
                                                                      String annotationFqn) {
        return construct.getAnnotationMirrors().stream()
                .filter(ann -> ((TypeElement) ann.getAnnotationType().asElement())
                        .getQualifiedName().contentEquals(annotationFqn))
                .findAny();
    }

    public static Optional<? extends AnnotationValue> findAnnotationValue(AnnotatedConstruct construct,
                                                                          String annotationFqn,
                                                                          String name) {
        return findAnnotation(construct, annotationFqn)
                .map(ann -> ann.getElementValues().entrySet())
                .flatMap(entries -> entries.stream()
                        .filter(entry -> entry.getKey().getSimpleName().contentEquals(name))
                        .findAny())
                .map(Map.Entry::getValue);
    }

    public static Optional<? extends AnnotationValue> findAnnotationValue(AnnotatedConstruct construct,
                                                                          String annotationFqn) {
        return findAnnotationValue(construct, annotationFqn, "value");
    }
}
