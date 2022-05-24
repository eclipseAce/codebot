package io.codebot.apt.crud;

import com.google.common.collect.Maps;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.code.*;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.SetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractCrudProcessor {
    private static final String CRUD_SERVICE_FQN = "io.codebot.CrudService";
    private static final String SERVICE_FQN = "org.springframework.stereotype.Service";
    private static final String TRANSACTIONAL_FQN = "org.springframework.transaction.annotation.Transactional";

    protected ProcessingEnvironment processingEnv;
    protected TypeFactory typeFactory;

    protected Type superType;
    protected Entity entity;

    protected TypeCreator classCreator;

    public void init(ProcessingEnvironment processingEnv, TypeElement element) {
        this.processingEnv = processingEnv;
        this.typeFactory = new TypeFactory(processingEnv);

        this.superType = typeFactory.getType(element);

        ClassName superName = ClassName.get(element);
        ClassName className = ClassName.get(superName.packageName(), superName.simpleName() + "Impl");
        this.classCreator = TypeCreators
                .createClass(className.packageName(), className.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.bestGuess(SERVICE_FQN)).build());
        if (superType.isInterface()) {
            classCreator.addSuperinterface(superType);
        } else {
            classCreator.superclass(superType);
        }
    }

    public void process() throws IOException {
        Methods.allOf(superType).stream()
                .filter(it -> it.getModifiers().contains(Modifier.ABSTRACT))
                .forEach(method -> {
                    MethodCreator creator = MethodCreators.overriding(method);
                    if (method.getSimpleName().startsWith("create")) {
                        processCreateMethod(method, creator);
                    } //
                    else if (method.getSimpleName().startsWith("update")) {
                        processUpdateMethod(method, creator);
                    } //
                    else if (method.getSimpleName().startsWith("find")) {
                        processQueryMethod(method, creator);
                    }
                    classCreator.addMethod(creator.create());
                });
        classCreator.create().writeTo(processingEnv.getFiler());
    }

    protected void processCreateMethod(Method overridden, MethodCreator creator) {
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
        Variable entityVar = doCreateEntity(overridden, creator, sources);
        if (!overridden.getReturnType().isVoid()) {
            creator.addAnnotation(AnnotationSpec.builder(ClassName.bestGuess(TRANSACTIONAL_FQN)).build());
            creator.body().add("return $L;\n",
                    doTypeMapping(creator.body(), entityVar, overridden.getReturnType())
            );
        }
    }

    protected abstract Variable doCreateEntity(Method overridden, MethodCreator creator,
                                               Map<String, Expression> sources);

    protected void processUpdateMethod(Method overridden, MethodCreator creator) {
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

        if (entityId != null) {
            Variable result = doUpdateEntity(overridden, creator, entityId, sources);
            if (!overridden.getReturnType().isVoid()) {
                creator.addAnnotation(AnnotationSpec.builder(ClassName.bestGuess(TRANSACTIONAL_FQN)).build());
                creator.body().add("return $L;\n",
                        doTypeMapping(creator.body(), result, overridden.getReturnType())
                );
            }
        }
    }

    protected abstract Variable doUpdateEntity(Method overridden, MethodCreator creator,
                                               Expression entityId, Map<String, Expression> sources);

    protected void processQueryMethod(Method overridden, MethodCreator creator) {
        Variable result = doQueryEntities(overridden, creator);
        creator.body().add("return $L;\n", doTypeMapping(creator.body(), result, overridden.getReturnType()));
    }

    protected abstract Variable doQueryEntities(Method overridden, MethodCreator creator);

    protected CodeBlock doTypeMapping(CodeWriter writer, Variable source, Type targetType) {
        Type sourceType = source.getType();
        if (targetType.erasure().isAssignableFrom(List.class.getName()) && sourceType.isIterable()) {
            CodeWriter mappings = writer.fork();
            if (sourceType.erasure().isAssignableTo(Collection.class.getName())) {
                mappings.add("$N.stream()", source.getName());
            } else {
                mappings.add("$1T.stream($2N.spliterator(), false)", StreamSupport.class, source.getName());
            }
            Variable itVar = Variables.of(sourceType.getIterableElementType(), mappings.allocateName("it"));
            mappings.add(".map($1N -> {\n$>", itVar.getName());
            mappings.add("return $L;\n", doTypeMapping(mappings, itVar, targetType.getTypeArguments().get(0)));
            mappings.add("$<}).collect($T.toList())", Collectors.class);
            return mappings.getCode();
        }

        if (sourceType.equals(entity.getType()) && targetType.isAssignableFrom(entity.getIdType())) {
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
