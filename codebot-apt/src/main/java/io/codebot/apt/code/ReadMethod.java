package io.codebot.apt.code;

import javax.lang.model.type.TypeMirror;

public interface ReadMethod extends Method {
    String getReadName();

    TypeMirror getReadType();
}
