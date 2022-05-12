package io.codebot.apt.type;

import com.google.common.collect.ImmutableMap;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.QualifiedNameable;
import java.util.Map;
import java.util.stream.Collectors;

public class Annotation {
    private final AnnotationMirror annotationMirror;
    private final Lazy<Map<String, AnnotationValue>> lazyAnnotationValues;

    Annotation(AnnotationMirror annotationMirror) {
        this.annotationMirror = annotationMirror;
        this.lazyAnnotationValues = Lazy.of(() -> ImmutableMap.copyOf(
                annotationMirror.getElementValues().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().getSimpleName().toString(),
                        Map.Entry::getValue
                ))
        ));
    }

    public AnnotationMirror getAnnotationMirror() {
        return annotationMirror;
    }

    public String getQualifiedName() {
        return ((QualifiedNameable) annotationMirror.getAnnotationType().asElement())
                .getQualifiedName().toString();
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name) {
        AnnotationValue val = lazyAnnotationValues.get().get(name);
        if (val == null) {
            throw new IllegalArgumentException("No value '" + name + "' present on " + annotationMirror);
        }
        return (T) val.getValue();
    }
}
