package io.codebot.apt.crud.code;

import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.Service;
import io.codebot.apt.type.Executable;

public interface Query {
    Snippet query(Entity entity, Service service, Executable method, NameAllocator names);
}
