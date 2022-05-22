package io.codebot.apt.crud;

import com.squareup.javapoet.*;
import io.codebot.apt.code.*;
import io.codebot.apt.type.Executable;

import javax.lang.model.element.Modifier;
import java.util.stream.Collectors;

public class ServiceGenerator {
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

    public MethodSpec buildCreateMethod(Service service, Entity entity, Executable method) {
        MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );
        CodeWriter methodBody = CodeWriters.create(method.getElement());
        JpaCreateBuilder create = new JpaCreateBuilder();
        create.setEntity(entity);
        create.setJpaRepository(CodeBlock.of("this.repository"));
        create.create(
                methodBody,
                method.getParameters().stream()
                        .map(it -> methodBody.newVariable(it.getSimpleName(), it.getType()))
                        .collect(Collectors.toList()),
                method.getReturnType()
        );
        methodBuilder.addCode(methodBody.getCode());
        return methodBuilder.build();
    }

    public MethodSpec buildUpdateMethod(Service service, Entity entity, Executable method) {
        MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );
        CodeWriter methodBody = CodeWriters.create(method.getElement());
        JpaUpdateBuilder update = new JpaUpdateBuilder();
        update.setEntity(entity);
        update.setJpaRepository(CodeBlock.of("this.repository"));
        update.update(
                methodBody,
                method.getParameters().stream()
                        .map(it -> methodBody.newVariable(it.getSimpleName(), it.getType()))
                        .collect(Collectors.toList()),
                method.getReturnType()
        );
        methodBuilder.addCode(methodBody.getCode());
        return methodBuilder.build();
    }

    public MethodSpec buildQueryMethod(Service service, Entity entity, Executable method) {
        MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );
        CodeWriter methodBody = CodeWriters.create(method.getElement());
        QuerydslJpaQueryBuilder query = new QuerydslJpaQueryBuilder();
        query.setEntity(entity);
        query.setJpaRepository(CodeBlock.of("this.repository"));
        query.setJpaSpecificationExecutor(CodeBlock.of("this.jpaSpecificationExecutor"));
        query.setQuerydslPredicateExecutor(CodeBlock.of("this.querydslPredicateExecutor"));
        query.find(
                methodBody,
                method.getParameters().stream()
                        .map(it -> methodBody.newVariable(it.getSimpleName(), it.getType()))
                        .collect(Collectors.toList()),
                method.getReturnType()
        );
        methodBuilder.addCode(methodBody.getCode());
        return methodBuilder.build();
    }
}
