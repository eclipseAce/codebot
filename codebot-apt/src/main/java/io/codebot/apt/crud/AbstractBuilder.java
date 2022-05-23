package io.codebot.apt.crud;

import com.google.common.collect.Maps;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import io.codebot.apt.code.*;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.SetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractBuilder {
    private Entity entity;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public MethodSpec create(Method overridden) {
        MethodWriter methodWriter = MethodWriters.overriding(overridden);
        CodeWriter bodyWriter = methodWriter.body();

        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Parameter param : overridden.getParameters()) {
            Optional<SetAccessor> setter = getEntity().getType().findSetter(param.getName(), param.getType());
            if (setter.isPresent()) {
                sources.put(param.getName(), param.asExpression());
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                setter = getEntity().getType().findSetter(getter.getAccessedName(), getter.getAccessedType());
                if (setter.isPresent()) {
                    sources.put(getter.getAccessedName(), Expressions.of(
                            getter.getAccessedType(),
                            CodeBlock.of("$1N.$2N()", param.getName(), getter.getSimpleName())
                    ));
                }
            }
        }
        Variable result = doCreate(methodWriter, sources);
        if (!overridden.getReturnType().isVoid()) {
            bodyWriter.add("return $L;\n", doMappings(bodyWriter, result, overridden.getReturnType()));
        }
        return methodWriter.getMethod();
    }

    protected abstract Variable doCreate(MethodWriter methodWriter, Map<String, Expression> sources);

    public MethodSpec update(Method overridden) {
        MethodWriter methodWriter = MethodWriters.overriding(overridden);
        CodeWriter bodyWriter = methodWriter.body();

        Expression entityId = null;
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Parameter variable : overridden.getParameters()) {
            if (variable.getName().equals(getEntity().getIdName())
                    && variable.getType().isAssignableTo(getEntity().getIdType())) {
                if (entityId == null) {
                    entityId = variable.asExpression();
                }
                continue;
            }
            Optional<SetAccessor> setter = getEntity().getType()
                    .findSetter(variable.getName(), variable.getType());
            if (setter.isPresent()) {
                sources.put(variable.getName(), variable.asExpression());
                continue;
            }
            for (GetAccessor getter : variable.getType().getGetters()) {
                if (getter.getAccessedName().equals(getEntity().getIdName())
                        && getter.getAccessedType().isAssignableTo(getEntity().getIdType())) {
                    if (entityId == null) {
                        entityId = Expressions.of(
                                getter.getAccessedType(),
                                CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                        );
                    }
                    continue;
                }
                setter = getEntity().getType()
                        .findSetter(getter.getAccessedName(), getter.getAccessedType());
                if (setter.isPresent()) {
                    sources.put(getter.getAccessedName(), Expressions.of(
                            getter.getAccessedType(),
                            CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                    ));
                }
            }
        }
        if (entityId != null) {
            Variable result = doUpdate(methodWriter, entityId, sources);
            if (!overridden.getReturnType().isVoid()) {
                bodyWriter.add("return $L;\n", doMappings(bodyWriter, result, overridden.getReturnType()));
            }
        }
        return methodWriter.getMethod();
    }

    protected abstract Variable doUpdate(MethodWriter methodWriter, Expression targetId, Map<String, Expression> sources);

    public MethodSpec query(Method overridden) {
        MethodWriter methodWriter = MethodWriters.overriding(overridden);
        CodeWriter bodyWriter = methodWriter.body();

        Variable result;
        List<Parameter> queryParams = getQueryParameters(overridden);
        if (queryParams.isEmpty()) {
            result = doQuery(overridden, methodWriter);
        } else if (queryParams.size() == 1
                && getEntity().getIdName().equals(queryParams.get(0).getName())
                && getEntity().getIdType().isAssignableFrom(queryParams.get(0).getType())) {
            result = doQuery(overridden, methodWriter, queryParams.get(0));
        } else {
            result = doQuery(overridden, methodWriter, queryParams);
        }
        bodyWriter.add("return $L;\n", doMappings(bodyWriter, result, overridden.getReturnType()));
        return methodWriter.getMethod();
    }

    protected abstract List<Parameter> getQueryParameters(Method overridden);

    protected abstract Variable doQuery(Method overridden, MethodWriter methodWriter);

    protected abstract Variable doQuery(Method overridden, MethodWriter methodWriter, Parameter idParam);

    protected abstract Variable doQuery(Method overridden, MethodWriter methodWriter, List<Parameter> params);

    protected CodeBlock doMappings(CodeWriter codeWriter, Variable source, Type targetType) {
        Type sourceType = source.getType();

        if (targetType.erasure().isAssignableFrom(List.class.getName())
                && sourceType.erasure().isAssignableTo(Iterable.class.getName())) {

            TypeFactory typeFactory = getEntity().getType().getFactory();
            TypeElement iterableElement = typeFactory.getElementUtils().getTypeElement(Iterable.class.getName());

            CodeWriter mappingBuilder = codeWriter.fork();
            if (sourceType.erasure().isAssignableTo(Collection.class.getName())) {
                mappingBuilder.add("$N.stream()", source.getName());
            } else {
                mappingBuilder.add("$1T.stream($2N.spliterator(), false)", StreamSupport.class, source.getName());
            }

            Variable itVar = Variables.of(
                    typeFactory.getType(sourceType.asMember(iterableElement.getTypeParameters().get(0))),
                    mappingBuilder.allocateName("it")
            );
            mappingBuilder.add(".map($1N -> {\n$>", itVar.getName());
            mappingBuilder.add("return $L;\n", doMappings(
                    mappingBuilder, itVar, targetType.getTypeArguments().get(0)
            ));
            mappingBuilder.add("$<}).collect($T.toList())", Collectors.class);

            return mappingBuilder.getCode();
        }

        if (sourceType.equals(getEntity().getType())
                && targetType.isAssignableFrom(getEntity().getIdType())) {
            return CodeBlock.of("$1N.$2N()", source.getName(), getEntity().getIdGetter().getSimpleName());
        }

        String tempVar = codeWriter.allocateName("temp");
        codeWriter.add("$1T $2N = new $1T();\n", targetType.getTypeMirror(), tempVar);
        for (SetAccessor setter : targetType.getSetters()) {
            sourceType.findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                    codeWriter.add("$1N.$2N($3L.$4N());\n",
                            tempVar, setter.getSimpleName(), source.getName(), it.getSimpleName()
                    )
            );
        }
        return CodeBlock.of("$N", tempVar);
    }
}
