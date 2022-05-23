package io.codebot.apt.code;

import io.codebot.apt.type.Type;

public interface WriteMethod extends Method {
    String getWriteName();

    Type getWriteType();
}
