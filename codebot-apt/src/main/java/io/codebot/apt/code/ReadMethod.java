package io.codebot.apt.code;

import io.codebot.apt.type.Type;

public interface ReadMethod extends Method {
    String getReadName();

    Type getReadType();
}
