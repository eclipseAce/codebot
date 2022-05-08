package io.cruder.apt.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

public class AnnotationUtils {
    public static Optional<? extends AnnotationMirror> findAnnotation(AnnotatedConstruct construct,
                                                                      String annotationFqn) {
        return construct.getAnnotationMirrors().stream()
                .filter(ann -> ((TypeElement) ann.getAnnotationType().asElement())
                        .getQualifiedName().contentEquals(annotationFqn))
                .findAny();
    }

    public static boolean isAnnotationPresent(AnnotatedConstruct construct,
                                              String annotationFqn) {
        return findAnnotation(construct, annotationFqn).isPresent();
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> findValue(AnnotationMirror annotation, String name) {
        return annotation.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().contentEquals(name))
                .findAny()
                .map(entry -> (T) entry.getValue().getValue());
    }

    public static <T> Optional<T> findAnnotationValue(AnnotatedConstruct construct,
                                                      String annotationFqn,
                                                      String name) {
        return findAnnotation(construct, annotationFqn).flatMap(ann -> findValue(ann, name));
    }

    public static <T> Optional<T> findAnnotationValue(AnnotatedConstruct construct,
                                                      String annotationFqn) {
        return findAnnotationValue(construct, annotationFqn, "value");
    }
}
