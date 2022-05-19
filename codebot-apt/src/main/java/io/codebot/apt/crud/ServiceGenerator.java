package io.codebot.apt.crud;

import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.crud.coding.*;
import io.codebot.apt.type.*;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ServiceGenerator {

    private static final String PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    public static final String TRANSACTIONAL_FQN = "org.springframework.transaction.annotation.Transactional";
    public static final String JPA_REPOSITORY_FQN = "org.springframework.data.jpa.repository.JpaRepository";
    public static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";
    public static final String JPA_SPECIFICATION_EXECUTOR_FQN = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";
    public static final String QUERYDSL_PREDICATE_EXECUTOR_FQN = "org.springframework.data.querydsl.QuerydslPredicateExecutor";
    public static final String SERVICE_FQN = "org.springframework.stereotype.Service";

    public JavaFile generate(Service service, Entity entity) {
        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder(service.getImplTypeName());
        serviceBuilder.addModifiers(Modifier.PUBLIC);
        serviceBuilder.addAnnotation(ClassName.bestGuess(SERVICE_FQN));
        if (service.getType().isInterface()) {
            serviceBuilder.addSuperinterface(service.getTypeName());
        } else {
            serviceBuilder.superclass(service.getTypeName());
        }
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess(JPA_REPOSITORY_FQN),
                        entity.getTypeName(), entity.getIdTypeName().box()
                ), "repository", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess(JPA_SPECIFICATION_EXECUTOR_FQN),
                        entity.getTypeName()
                ), "jpaSpecificationExecutor", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess(QUERYDSL_PREDICATE_EXECUTOR_FQN),
                        entity.getTypeName()
                ), "querydslPredicateExecutor", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        for (Executable method : service.getType().getMethods()) {
            if (method.getSimpleName().startsWith("create")) {
                serviceBuilder.addMethod(buildCreateMethod(service, entity, method));
            } //
            else if (method.getSimpleName().startsWith("update")) {
                serviceBuilder.addMethod(buildUpdateMethod(service, entity, method));
            } //
            else if (method.getSimpleName().startsWith("find")) {
                serviceBuilder.addMethod(buildQueryMethod(service, entity, method));
            }
        }
        return JavaFile.builder(service.getImplTypeName().packageName(), serviceBuilder.build()).build();
    }

    private CodeBlock returns(String sourceVar, Type sourceType, Type returnType,
                              Entity entity, NameAllocator names) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (returnType.erasure().isAssignableFrom(PAGE_FQN)
                && sourceType.erasure().isAssignableFrom(PAGE_FQN)) {
            String itVar = names.newName("it");
            builder.add("return $1N.map($2N -> {\n$>", sourceVar, itVar);
            builder.add(returns(
                    itVar,
                    sourceType.getTypeArguments().get(0),
                    returnType.getTypeArguments().get(0),
                    entity, names.clone()
            ));
            builder.add("$<});\n");
        } //
        else if (returnType.erasure().isAssignableFrom(List.class.getName())
                && sourceType.erasure().isAssignableTo(Iterable.class.getName())) {
            String itVar = names.newName("it");
            CodeBlock stream = sourceType.erasure().isAssignableTo(Collection.class.getName())
                    ? CodeBlock.of("$1N.stream()", sourceVar)
                    : CodeBlock.of("$1T.stream($2N.spliterator(), false)", StreamSupport.class, sourceVar);
            builder.add("return $1L.map($2N -> {\n$>", stream, itVar);
            builder.add(returns(
                    itVar,
                    sourceType.getTypeArguments().get(0),
                    returnType.getTypeArguments().get(0),
                    entity, names.clone()
            ));
            builder.add("$<}).collect($1T.toList());\n", Collectors.class);
        } //
        else if (sourceType.equals(entity.getType())
                && returnType.isAssignableFrom(entity.getIdType())) {
            builder.add("return $1N.$2N();\n", sourceVar, entity.getIdGetter().getSimpleName());
        } //
        else {
            String tempVar = names.newName("temp");
            builder.add("$1T $2N = new $1T();\n",
                    returnType.getTypeMirror(), tempVar
            );
            for (SetAccessor setter : returnType.getSetters()) {
                sourceType.findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                        builder.add("$1N.$2N($3N.$4N());\n",
                                tempVar, setter.getSimpleName(), sourceVar, it.getSimpleName()
                        ));
            }
            builder.add("return $N;\n", tempVar);
        }
        return builder.build();
    }

    public MethodSpec buildCreateMethod(Service service, Entity entity, Executable method) {
        NameAllocator names = new NameAllocator();
        method.getParameters().forEach(it -> names.newName(it.getSimpleName(), it));
        MethodSpec.Builder builder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );

        String entityVar = names.newName("entity");
        builder.addAnnotation(ClassName.bestGuess(TRANSACTIONAL_FQN));
        builder.addCode("$1T $2N = new $1T();\n", entity.getType().getTypeMirror(), entityVar);
        for (Variable param : method.getParameters()) {
            Optional<SetAccessor> setter = entity.getType().findSetter(
                    param.getSimpleName(), param.getType()
            );
            if (setter.isPresent()) {
                builder.addCode("$1N.$2N($3N);\n",
                        entityVar, setter.get().getSimpleName(), names.get(param)
                );
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                entity.getType().findSetter(
                        getter.getAccessedName(), getter.getAccessedType()
                ).ifPresent(it -> builder.addCode("$1N.$2N($3N.$4N());\n",
                        entityVar, it.getSimpleName(),
                        names.get(param), getter.getSimpleName()
                ));
            }
        }
        builder.addCode("this.repository.save($1N);\n", entityVar);
        if (!method.getReturnType().isVoid()) {
            builder.addCode(returns(
                    entityVar, entity.getType(), method.getReturnType(), entity, names
            ));
        }
        return builder.build();
    }

    public MethodSpec buildUpdateMethod(Service service, Entity entity, Executable method) {
        NameAllocator names = new NameAllocator();
        method.getParameters().forEach(it -> names.newName(it.getSimpleName(), it));
        MethodSpec.Builder builder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );

        builder.addAnnotation(ClassName.bestGuess("org.springframework.transaction.annotation.Transactional"));

        String entityVar = names.newName("entity");
        CodeBlock entityLoad = null;
        CodeBlock.Builder propertySets = CodeBlock.builder();
        for (Variable param : method.getParameters()) {
            if (entity.getIdName().equals(param.getSimpleName())
                    && entity.getIdType().isAssignableFrom(param.getType())) {
                if (entityLoad == null) {
                    entityLoad = CodeBlock.of("$1T $2N = this.repository.getById($3N);\n",
                            entity.getType().getTypeMirror(), entityVar, names.get(param)
                    );
                }
                continue;
            }
            Optional<SetAccessor> setter = entity.getType().findSetter(
                    param.getSimpleName(), param.getType()
            );
            if (setter.isPresent()) {
                propertySets.add("$1N.$2N($3N);\n",
                        entityVar, setter.get().getSimpleName(), names.get(param)
                );
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                if (entity.getIdName().equals(getter.getAccessedName())
                        && entity.getIdType().isAssignableFrom(getter.getAccessedType())) {
                    if (entityLoad == null) {
                        entityLoad = CodeBlock.of("$1T $2N = this.repository.getById($3N.$4N());\n",
                                entity.getType().getTypeMirror(), entityVar,
                                names.get(param), getter.getSimpleName()
                        );
                    }
                    continue;
                }
                entity.getType().findSetter(
                        getter.getAccessedName(), getter.getAccessedType()
                ).ifPresent(it -> propertySets.add("$1N.$2N($3N.$4N());\n",
                        entityVar, it.getSimpleName(),
                        names.get(param), getter.getSimpleName()
                ));
            }
        }
        if (entityLoad == null) {
            throw new IllegalArgumentException("Can't find a way to load entity");
        }
        builder.addCode(entityLoad);
        builder.addCode(propertySets.build());
        builder.addCode("this.repository.save($1N);\n", entityVar);
        if (!method.getReturnType().isVoid()) {
            builder.addCode(returns(
                    entityVar, entity.getType(), method.getReturnType(), entity, names
            ));
        }
        return builder.build();
    }

    public MethodSpec buildQueryMethod(Service service, Entity entity, Executable method) {
        MethodBodyContext context = new MethodBodyContext(method);
        List<LocalVariable> queryVariables = Lists.newArrayList();
        List<LocalVariable> pageableVariables = Lists.newArrayList();
        for (LocalVariable variable : context.getLocalVariables()) {
            if (variable.getType().isAssignableTo(PAGEABLE_FQN)) {
                pageableVariables.add(variable);
            } else {
                queryVariables.add(variable);
            }
        }
        JpaSpecification jpaSpecification = new JpaSpecification(context, entity, queryVariables);
        Expression spec = jpaSpecification.createExpression();
        if (spec != null) {
            context.getCodeBuilder()
                    .add("$1T spec = $2L;\n", spec.getType().getTypeMirror(), spec.getCode());
        }
        QuerydslPredicate querydslPredicate = new QuerydslPredicate(context, entity, queryVariables);
        Expression pred = querydslPredicate.createExpression();
        if (pred != null) {
            context.getCodeBuilder()
                    .add("$1T pred = $2L;\n", pred.getType().getTypeMirror(), pred.getCode());
        }

        return MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        ).addCode(context.getCodeBuilder().build()).build();
    }
}
