package io.codebot.apt.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Annotated {
    List<Annotation> annotations();

    default Optional<Annotation> findAnnotation(String qualifiedName) {
        return annotations().stream()
                .filter(it -> it.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    default List<Annotation> findAnnotations(String qualifiedName) {
        return annotations().stream()
                .filter(it -> it.qualifiedName().equals(qualifiedName))
                .collect(Collectors.toList());
    }

    default boolean isAnnotationPresent(String qualifiedName) {
        return findAnnotation(qualifiedName).isPresent();
    }
}
