package io.codebot.apt.code;

import javax.lang.model.type.TypeMirror;
import java.util.Collection;

public interface MethodCollection extends Collection<Method> {
    Collection<ReadMethod> readers();

    Collection<WriteMethod> writers();

    WriteMethod findWriter(String name, TypeMirror assigningType);

    ReadMethod findReader(String name, TypeMirror acceptingType);
}
