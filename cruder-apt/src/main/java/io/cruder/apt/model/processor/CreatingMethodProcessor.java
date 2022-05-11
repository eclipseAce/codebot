package io.cruder.apt.model.processor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;
import io.cruder.apt.model.*;
import io.cruder.apt.type.SetAccessor;

import java.util.Optional;

public class CreatingMethodProcessor implements MethodProcessor {
    @Override
    public void process(Service service, TypeSpec.Builder serviceBuilder,
                        Method method, MethodSpec.Builder methodBuilder,
                        NameAllocator nameAlloc) {
        if (!method.getSimpleName().startsWith("create")) {
            return;
        }

        Entity entity = service.getEntity();
        String entityVar = nameAlloc.newName("entity");
        methodBuilder.addStatement(
                "$1T $2N = new $1T()",
                entity.getTypeName(), entityVar
        );
        for (Parameter param : method.getParameters()) {
            Optional<SetAccessor> directSetter = entity.getType().findSetter(param.getName(), param.getType());
            if (directSetter.isPresent()) {
                methodBuilder.addStatement(
                        "$1N.$2N($3N)",
                        entityVar, directSetter.get().simpleName(), param.getName()
                );
            } //
            else if (param.getType().isDeclared()) {
                CodeUtils.map(param.getType(), param.getName(), entity.getType(), entityVar)
                        .forEach(methodBuilder::addCode);
            } //
            else {
                throw new IllegalArgumentException("Can't handle parameter type " + param.getType().asTypeMirror());
            }
        }
        methodBuilder.addStatement("repository.save($N)", entityVar);
        CodeUtils.mapFromEntityAndReturn(entity.getType(), entityVar, method.getReturnType(), entity, nameAlloc)
                .forEach(methodBuilder::addCode);
    }
}
