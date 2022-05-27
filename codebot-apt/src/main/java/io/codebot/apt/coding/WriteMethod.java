package io.codebot.apt.coding;

import javax.lang.model.type.TypeMirror;

public interface WriteMethod extends Method {
    String getWriteName();

    TypeMirror getWriteType();
}
