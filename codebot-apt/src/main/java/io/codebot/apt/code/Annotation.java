package io.codebot.apt.code;

import javax.lang.model.element.AnnotationMirror;

public interface Annotation {
    AnnotationMirror getMirror();

    String getString(String name);

    boolean getBoolean(String name);
}
