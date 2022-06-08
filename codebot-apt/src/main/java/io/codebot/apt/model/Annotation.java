package io.codebot.apt.model;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public interface Annotation {
    AnnotationMirror getMirror();

    String getString(String name);

    List<String> getStringArray(String name);

    boolean getBoolean(String name);

    TypeMirror getType(String name);
}
