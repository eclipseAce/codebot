package io.codebot.apt.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static Optional<? extends AnnotationMirror> find(AnnotatedConstruct construct, String annotationFqn) {
        return construct.getAnnotationMirrors().stream()
                .filter(ann -> ((TypeElement) ann.getAnnotationType().asElement())
                        .getQualifiedName().contentEquals(annotationFqn))
                .findAny();
    }

    public static boolean isPresent(AnnotatedConstruct construct, String annotationFqn) {
        return find(construct, annotationFqn).isPresent();
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> findValue(AnnotationMirror annotation, String name) {
        return annotation.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().contentEquals(name))
                .findAny().map(entry -> (T) entry.getValue().getValue());
    }

    public static <T> Optional<T> findValue(AnnotatedConstruct construct, String annotationFqn, String name) {
        return find(construct, annotationFqn).flatMap(ann -> findValue(ann, name));
    }

    public static <T> Optional<T> findValue(AnnotatedConstruct construct, String annotationFqn) {
        return findValue(construct, annotationFqn, "value");
    }
}
