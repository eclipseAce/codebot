package io.codebot.apt.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Annotated {
    List<Annotation> getAnnotations();

    default Optional<Annotation> findAnnotation(String qualifiedName) {
        return getAnnotations().stream()
                .filter(it -> it.getQualifiedName().equals(qualifiedName))
                .findFirst();
    }

    default List<Annotation> findAnnotations(String qualifiedName) {
        return getAnnotations().stream()
                .filter(it -> it.getQualifiedName().equals(qualifiedName))
                .collect(Collectors.toList());
    }

    default boolean isAnnotationPresent(String qualifiedName) {
        return findAnnotation(qualifiedName).isPresent();
    }
}
