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
    public MethodSpec create(Method overridden, Entity entity) {
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Parameter param : overridden.getParameters()) {
            Optional<SetAccessor> setter = entity.getType().findSetter(param.getName(), param.getType());
            if (setter.isPresent()) {
                sources.put(param.getName(), param.asExpression());
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                setter = entity.getType().findSetter(getter.getAccessedName(), getter.getAccessedType());
                if (setter.isPresent()) {
                    sources.put(getter.getAccessedName(), Expressions.of(
                            getter.getAccessedType(),
                            CodeBlock.of("$1N.$2N()", param.getName(), getter.getSimpleName())
                    ));
                }
            }
        }

        MethodCreator creator = MethodCreators.overriding(overridden);
        Variable result = doCreate(overridden, entity, creator, sources);
        if (!overridden.getReturnType().isVoid()) {
            creator.body().add("return $L;\n",
                    doMappings(entity, creator.body(), result, overridden.getReturnType())
            );
        }
        return creator.create();
    }

    protected abstract Variable doCreate(Method overridden,
                                         Entity entity,
                                         MethodCreator creator,
                                         Map<String, Expression> sources);

    public MethodSpec update(Method overridden, Entity entity) {
        Expression entityId = null;
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Parameter variable : overridden.getParameters()) {
            if (variable.getName().equals(entity.getIdName())
                    && variable.getType().isAssignableTo(entity.getIdType())) {
                if (entityId == null) {
                    entityId = variable.asExpression();
                }
                continue;
            }
            Optional<SetAccessor> setter = entity.getType()
                    .findSetter(variable.getName(), variable.getType());
            if (setter.isPresent()) {
                sources.put(variable.getName(), variable.asExpression());
                continue;
            }
            for (GetAccessor getter : variable.getType().getGetters()) {
                if (getter.getAccessedName().equals(entity.getIdName())
                        && getter.getAccessedType().isAssignableTo(entity.getIdType())) {
                    if (entityId == null) {
                        entityId = Expressions.of(
                                getter.getAccessedType(),
                                CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                        );
                    }
                    continue;
                }
                setter = entity.getType()
                        .findSetter(getter.getAccessedName(), getter.getAccessedType());
                if (setter.isPresent()) {
                    sources.put(getter.getAccessedName(), Expressions.of(
                            getter.getAccessedType(),
                            CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                    ));
                }
            }
        }

        MethodCreator creator = MethodCreators.overriding(overridden);
        if (entityId != null) {
            Variable result = doUpdate(overridden, entity, creator, entityId, sources);
            if (!overridden.getReturnType().isVoid()) {
                creator.body().add("return $L;\n",
                        doMappings(entity, creator.body(), result, overridden.getReturnType())
                );
            }
        }
        return creator.create();
    }

    protected abstract Variable doUpdate(Method overridden,
                                         Entity entity,
                                         MethodCreator creator,
                                         Expression entityId,
                                         Map<String, Expression> sources);

    public MethodSpec query(Method overridden, Entity entity) {
        MethodCreator creator = MethodCreators.overriding(overridden);

        Variable result;
        List<? extends Parameter> queryParams = getQueryParameters(overridden.getParameters());
        if (queryParams.isEmpty()) {
            result = doQuery(overridden, entity, creator);
        } else if (queryParams.size() == 1
                && entity.getIdName().equals(queryParams.get(0).getName())
                && entity.getIdType().isAssignableFrom(queryParams.get(0).getType())) {
            result = doQuery(overridden, entity, creator, queryParams.get(0));
        } else {
            result = doQuery(overridden, entity, creator, queryParams);
        }
        creator.body().add("return $L;\n", doMappings(entity, creator.body(), result, overridden.getReturnType()));
        return creator.create();
    }

    protected abstract List<? extends Parameter> getQueryParameters(List<? extends Parameter> params);

    protected abstract Variable doQuery(Method overridden,
                                        Entity entity,
                                        MethodCreator creator);

    protected abstract Variable doQuery(Method overridden,
                                        Entity entity,
                                        MethodCreator creator,
                                        Parameter idParam);

    protected abstract Variable doQuery(Method overridden,
                                        Entity entity,
                                        MethodCreator creator,
                                        List<? extends Parameter> params);

    protected CodeBlock doMappings(Entity entity,
                                   CodeWriter writer,
                                   Variable source,
                                   Type targetType) {
        Type sourceType = source.getType();

        if (targetType.erasure().isAssignableFrom(List.class.getName())
                && sourceType.erasure().isAssignableTo(Iterable.class.getName())) {

            TypeFactory typeFactory = entity.getType().getFactory();
            TypeElement iterableElement = typeFactory.getElementUtils().getTypeElement(Iterable.class.getName());

            CodeWriter mappingBuilder = writer.fork();
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
                    entity, mappingBuilder, itVar, targetType.getTypeArguments().get(0)
            ));
            mappingBuilder.add("$<}).collect($T.toList())", Collectors.class);

            return mappingBuilder.getCode();
        }

        if (sourceType.equals(entity.getType())
                && targetType.isAssignableFrom(entity.getIdType())) {
            return CodeBlock.of("$1N.$2N()", source.getName(), entity.getIdGetter().getSimpleName());
        }

        String tempVar = writer.allocateName("temp");
        writer.add("$1T $2N = new $1T();\n", targetType.getTypeMirror(), tempVar);
        for (SetAccessor setter : targetType.getSetters()) {
            sourceType.findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                    writer.add("$1N.$2N($3L.$4N());\n",
                            tempVar, setter.getSimpleName(), source.getName(), it.getSimpleName()
                    )
            );
        }
        return CodeBlock.of("$N", tempVar);
    }
}
