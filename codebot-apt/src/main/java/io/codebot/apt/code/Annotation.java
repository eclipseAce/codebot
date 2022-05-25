package io.codebot.apt.code;

import javax.lang.model.element.AnnotationMirror;
import java.util.List;

public interface Annotation {
    AnnotationMirror getMirror();

    String getString(String name);

    List<String> getStringArray(String name);

    boolean getBoolean(String name);
}
