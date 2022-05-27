package io.codebot.apt.coding;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Annotations {
    private final Elements elementUtils;

    public static Annotations instanceOf(ProcessingEnvironment processingEnv) {
        return new Annotations(processingEnv.getElementUtils());
    }

    public Annotation of(AnnotationMirror mirror) {
        Map<String, AnnotationValue> values = elementUtils.getElementValuesWithDefaults(mirror)
                .entrySet().stream().collect(Collectors.toMap(
                        it -> it.getKey().getSimpleName().toString(),
                        Map.Entry::getValue
                ));
        return new AnnotationImpl(mirror, values);
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
        private static final AnnotationValueVisitor<String, Void> STRING_VISITOR =
                new SimpleAnnotationValueVisitor8<String, Void>() {
                    @Override
                    public String visitString(String s, Void unused) {
                        return s;
                    }
                };
        private static final AnnotationValueVisitor<Boolean, Void> BOOLEAN_VISITOR =
                new SimpleAnnotationValueVisitor8<Boolean, Void>() {
                    @Override
                    public Boolean visitBoolean(boolean b, Void unused) {
                        return b;
                    }
                };
        private static final AnnotationValueVisitor<TypeMirror, Void> TYPE_VISITOR =
                new SimpleAnnotationValueVisitor8<TypeMirror, Void>() {
                    @Override
                    public TypeMirror visitType(TypeMirror t, Void unused) {
                        return t;
                    }
                };

        private final AnnotationMirror mirror;
        private final Map<String, AnnotationValue> values;

        AnnotationImpl(AnnotationMirror mirror, Map<String, AnnotationValue> values) {
            this.mirror = mirror;
            this.values = values;
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
        public List<String> getStringArray(String name) {
            return visitArrayValue(name, STRING_VISITOR);
        }

        @Override
        public boolean getBoolean(String name) {
            return visitValue(name, BOOLEAN_VISITOR);
        }

        @Override
        public TypeMirror getType(String name) {
            return visitValue(name, TYPE_VISITOR);
        }

        private AnnotationValue getValue(String name) {
            AnnotationValue value = values.get(name);
            if (value == null) {
                throw new IllegalArgumentException("annotation value '" + name + "' not present");
            }
            return value;
        }

        private <T> T visitValue(String name, AnnotationValueVisitor<T, Void> visitor) {
            T obj = getValue(name).accept(visitor, null);
            if (obj == null) {
                throw new IllegalArgumentException("annotation value '" + name + "' type error");
            }
            return obj;
        }

        private <T> List<T> visitArrayValue(String name, AnnotationValueVisitor<T, Void> visitor) {
            List<T> objList = getValue(name).accept(new SimpleAnnotationValueVisitor8<List<T>, Void>() {
                @Override
                public List<T> visitArray(List<? extends AnnotationValue> vals, Void unused) {
                    return vals.stream()
                            .map(it -> it.accept(visitor, null))
                            .collect(Collectors.toList());
                }
            }, null);
            if (objList == null || objList.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("annotation value '" + name + "' type error");
            }
            return objList;
        }
    }
}
