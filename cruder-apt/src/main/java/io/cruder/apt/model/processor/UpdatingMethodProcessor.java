package io.cruder.apt.model.processor;

import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;
import io.cruder.apt.model.*;
import io.cruder.apt.type.Accessor;

import java.util.List;
import java.util.Optional;

public class UpdatingMethodProcessor implements MethodProcessor {
    @Override
    public void process(Service service, TypeSpec.Builder serviceBuilder,
                        Method method, MethodSpec.Builder methodBuilder,
                        NameAllocator nameAlloc) {
        if (!method.getSimpleName().startsWith("update")) {
            return;
        }
        Entity entity = service.getEntity();
        String entityVar = nameAlloc.newName("entity");
        CodeBlock idExpr = null;
        List<CodeBlock> mappingExprs = Lists.newArrayList();
        for (MethodParameter param : method.getParameters()) {
            if (idExpr == null
                    && entity.getIdName().equals(param.getName())
                    && entity.getIdType().isAssignableFrom(param.getType().asTypeMirror())) {
                idExpr = CodeBlock.of("$1N", param.getName());
                continue;
            }

            Optional<Accessor> directSetter = entity.getType()
                    .findWriteAccessor(param.getName(), param.getType().asTypeMirror());
            if (directSetter.isPresent()) {
                mappingExprs.add(CodeBlock.of(
                        "$1N.$2N($3N)",
                        entityVar, directSetter.get().getSimpleName(), param.getName()
                ));
                continue;
            }

            for (Accessor paramGetter : param.getType().findReadAccessors()) {
                if (idExpr == null
                        && entity.getIdName().equals(paramGetter.getAccessedName())
                        && entity.getIdType().isAssignableFrom(paramGetter.getAccessedType())) {
                    idExpr = CodeBlock.of("$1N.$2N()", param.getName(), paramGetter.getSimpleName());
                } else {
                    entity.getType()
                            .findWriteAccessor(paramGetter.getAccessedName(), paramGetter.getAccessedType())
                            .ifPresent(entitySetter -> CodeUtils.map(entityVar, entitySetter, param.getName(), paramGetter));
                }
            }
        }
        if (idExpr == null) {
            throw new IllegalArgumentException("Can't find a way to load entity");
        }
        methodBuilder.addStatement(
                "$1T $2N = repository.getById($3L)",
                entity.getType().asTypeMirror(), entityVar, idExpr
        );
        mappingExprs.forEach(methodBuilder::addStatement);
        methodBuilder.addStatement("repository.save($N)", entityVar);
        CodeUtils.mapFromEntityAndReturn(
                entity.getType(), entityVar, method.getReturnType(), entity, nameAlloc
        ).forEach(methodBuilder::addCode);
    }
}
