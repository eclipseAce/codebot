package io.codebot.apt.code;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Annotations {
    private final Elements elementUtils;

    public static Annotations instance(ProcessingEnvironment processingEnv) {
        return new Annotations(processingEnv.getElementUtils());
    }

    public Annotation of(AnnotationMirror mirror) {
        return new AnnotationImpl(elementUtils, mirror);
    }

    public Annotation find(AnnotatedConstruct construct,
                           Class<? extends java.lang.annotation.Annotation> annotationClass) {
        return find(construct, annotationClass.getName());
    }

    public Annotation find(AnnotatedConstruct construct, String annotationFqn) {
        return construct.getAnnotationMirrors().stream()
                .filter(it -> nameEquals(it, annotationFqn))
                .findFirst().map(this::of).orElse(null);
    }

    public boolean isPresent(AnnotatedConstruct construct, String annotationFqn) {
        return construct.getAnnotationMirrors().stream()
                .anyMatch(it -> nameEquals(it, annotationFqn));
    }

    public boolean isPresent(AnnotatedConstruct construct,
                             Class<? extends java.lang.annotation.Annotation> annotationClass) {
        return isPresent(construct, annotationClass.getName());
    }

    private static boolean nameEquals(AnnotationMirror mirror, String annotationFqn) {
        int lastSep = annotationFqn.lastIndexOf('.');
        if (lastSep > -1) {
            annotationFqn = annotationFqn.substring(0, lastSep)
                    + annotationFqn.substring(lastSep).replace('$', '.');
        }
        return ((TypeElement) mirror.getAnnotationType().asElement())
                .getQualifiedName().contentEquals(annotationFqn);
    }

    private static class AnnotationImpl implements Annotation {
        private final Elements elementUtils;
        private final AnnotationMirror mirror;
        private final Map<String, AnnotationValue> values;

        public AnnotationImpl(Elements elementUtils, AnnotationMirror mirror) {
            this.elementUtils = elementUtils;
            this.mirror = mirror;
            this.values = elementUtils
                    .getElementValuesWithDefaults(mirror).entrySet().stream()
                    .collect(Collectors.toMap(
                            it -> it.getKey().getSimpleName().toString(),
                            Map.Entry::getValue
                    ));
        }

        @Override
        public AnnotationMirror getMirror() {
            return mirror;
        }

        @Override
        public String getString(String name) {
            return visitValue(name, new SimpleAnnotationValueVisitor8<String, Void>() {
                @Override
                public String visitString(String s, Void unused) {
                    return s;
                }
            });
        }

        @Override
        public boolean getBoolean(String name) {
            return visitValue(name, new SimpleAnnotationValueVisitor8<Boolean, Void>() {
                @Override
                public Boolean visitBoolean(boolean b, Void unused) {
                    return b;
                }
            });
        }

        private <T> T visitValue(String name, AnnotationValueVisitor<T, Void> visitor) {
            AnnotationValue value = values.get(name);
            if (value == null) {
                throw new IllegalArgumentException("annotation value '" + name + "' not present");
            }
            T obj = value.accept(visitor, null);
            if (obj == null) {
                throw new IllegalArgumentException("annotation value type error");
            }
            return obj;
        }
    }
}
