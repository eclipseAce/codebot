package io.cruder.apt.util;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;

public abstract class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static <A extends Annotation> List<? extends TypeMirror> getClassValues(
            A annotation, Function<A, Class<?>[]> call) {
        try {
            call.apply(annotation);
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
        return Collections.emptyList();
    }

    public static <A extends Annotation> TypeMirror getClassValue(
            A annotation, Function<A, Class<?>> call) {
        try {
            call.apply(annotation);
        } catch (MirroredTypeException ex) {
            return ex.getTypeMirror();
        }
        return null;
    }
}
