package io.codebot.apt.model.processor;

import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;
import io.codebot.apt.model.*;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.SetAccessor;

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
        for (Parameter param : method.getParameters()) {
            if (idExpr == null
                    && entity.getIdName().equals(param.getName())
                    && entity.getIdType().isAssignableFrom(param.getType())) {
                idExpr = CodeBlock.of("$1N", param.getName());
                continue;
            }

            Optional<SetAccessor> directSetter = entity.getType().findSetter(param.getName(), param.getType());
            if (directSetter.isPresent()) {
                mappingExprs.add(CodeBlock.of(
                        "$1N.$2N($3N)",
                        entityVar, directSetter.get().simpleName(), param.getName()
                ));
                continue;
            }

            for (GetAccessor paramGetter : param.getType().getters()) {
                if (idExpr == null
                        && entity.getIdName().equals(paramGetter.accessedName())
                        && entity.getIdType().isAssignableFrom(paramGetter.accessedType())) {
                    idExpr = CodeBlock.of("$1N.$2N()", param.getName(), paramGetter.simpleName());
                } else {
                    entity.getType()
                            .findSetter(paramGetter.accessedName(), paramGetter.accessedType())
                            .ifPresent(entitySetter -> mappingExprs.add(
                                    CodeUtils.map(param.getName(), paramGetter, entityVar, entitySetter)
                            ));
                }
            }
        }
        if (idExpr == null) {
            throw new IllegalArgumentException("Can't find a way to load entity");
        }
        methodBuilder.addStatement(
                "$1T $2N = repository.getById($3L)",
                entity.getType().typeMirror(), entityVar, idExpr
        );
        mappingExprs.forEach(methodBuilder::addCode);
        methodBuilder.addStatement("repository.save($N)", entityVar);
        CodeUtils.mapFromEntityAndReturn(
                entity.getType(), entityVar, method.getReturnType(), entity, nameAlloc
        ).forEach(methodBuilder::addCode);
    }
}
