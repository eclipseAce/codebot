package io.codebot.apt.code;

import com.google.common.collect.Maps;
import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
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

    public void create(CodeWriter codeWriter, List<Variable> variables, Type returnType) {
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Variable variable : variables) {
            Optional<SetAccessor> setter = getEntity().getType()
                    .findSetter(variable.getName(), variable.getType());
            if (setter.isPresent()) {
                sources.put(variable.getName(), variable.asExpression());
                continue;
            }
            for (GetAccessor getter : variable.getType().getGetters()) {
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
        Variable result = doCreate(codeWriter, sources);

        if (returnType.isVoid()) {
            return;
        }

        codeWriter.add("return $L;\n", doMappings(codeWriter, result, returnType));
    }

    protected abstract Variable doCreate(CodeWriter codeWriter, Map<String, Expression> sources);

    public void update(CodeWriter codeWriter, List<Variable> variables, Type returnType) {
        Expression entityId = null;
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Variable variable : variables) {
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

        if (entityId == null) {
            // no id found
            return;
        }
        Variable result = doUpdate(codeWriter, entityId, sources);

        if (returnType.isVoid()) {
            return;
        }

        codeWriter.add("return $L;\n", doMappings(codeWriter, result, returnType));
    }

    protected abstract Variable doUpdate(CodeWriter codeWriter, Expression targetId,
                                         Map<String, Expression> sources);


    public void query(CodeWriter codeWriter, List<Variable> variables, Type returnType) {
        Variable result;
        if (variables.isEmpty()) {
            result = doFindAll(codeWriter);
        } else if (variables.size() == 1
                && getEntity().getIdName().equals(variables.get(0).getName())
                && getEntity().getIdType().isAssignableFrom(variables.get(0).getType())) {
            result = doFindById(codeWriter, variables.get(0));
        } else {
            result = doFind(codeWriter, variables);
        }
        codeWriter.add("return $L;\n", doMappings(
                codeWriter, result, returnType
        ));
    }

    protected abstract Variable doFindAll(CodeWriter codeWriter);

    protected abstract Variable doFindById(CodeWriter codeWriter, Variable idVariable);

    protected abstract Variable doFind(CodeWriter codeWriter, List<Variable> variables);

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

            Variable itVar = mappingBuilder.newVariable(
                    "it", typeFactory.getType(sourceType.asMember(iterableElement.getTypeParameters().get(0)))
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

        String tempVar = codeWriter.newName("temp");
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
