package io.codebot.apt.processor.crud;

import io.codebot.apt.code.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Entity {
    private static final String ID_FQN = "javax.persistence.Id";
    private static final String JPA_REPOSITORY_FQN = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String JPA_SPECIFICATION_EXECUTOR_FQN = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";
    private static final String QUERYDSL_PREDICATE_EXECUTOR_FQN = "org.springframework.data.querydsl.QuerydslPredicateExecutor";
    private static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";

    private final @Getter DeclaredType type;
    private final @Getter String idName;
    private final @Getter TypeMirror idType;
    private final @Getter ReadMethod idReadMethod;
    private final @Getter MethodCollection methods;

    private final @Getter DeclaredType jpaRepositoryType;
    private final @Getter DeclaredType jpaSpecificationExecutorType;
    private final @Getter DeclaredType querydslPredicateExecutorType;
    private final @Getter DeclaredType specificationType;

    public static Entity resolve(ProcessingEnvironment processingEnv, TypeMirror type) {
        Annotations annotationUtils = Annotations.instanceOf(processingEnv);
        Fields fieldUtils = Fields.instanceOf(processingEnv);
        Methods methodUtils = Methods.instanceOf(processingEnv);
        TypeOps typeOps = TypeOps.instanceOf(processingEnv);

        if (!typeOps.isDeclared(type)) {
            throw new IllegalArgumentException("Entity type not declared type");
        }
        DeclaredType entityType = (DeclaredType) type;
        Field idField = fieldUtils.allOf(entityType).stream()
                .filter(it -> annotationUtils.isPresent(it.getElement(), ID_FQN))
                .findFirst().orElse(null);
        if (idField == null) {
            throw new IllegalArgumentException("Can't determine entity id");
        }
        MethodCollection methods = methodUtils.allOf(entityType);
        ReadMethod idReadMethod = methods.readers().stream()
                .filter(it -> it.getReadName().equals(idField.getName())
                        && typeOps.isSame(idField.getType(), it.getReadType())
                )
                .findFirst().orElse(null);
        if (idReadMethod == null) {
            throw new IllegalArgumentException("Entity id not readable");
        }
        DeclaredType jpaRepositoryType = typeOps.getDeclared(
                JPA_REPOSITORY_FQN, entityType, typeOps.boxed(idField.getType())
        );

        DeclaredType jpaSpecificationExecutorType = typeOps.getDeclared(
                JPA_SPECIFICATION_EXECUTOR_FQN, entityType
        );

        DeclaredType querydslPredicateExecutorType = typeOps.getDeclared(
                QUERYDSL_PREDICATE_EXECUTOR_FQN, entityType
        );
        DeclaredType specificationType = typeOps.getDeclared(
                SPECIFICATION_FQN, entityType
        );
        return new Entity(
                entityType,
                idField.getName(),
                idField.getType(),
                idReadMethod,
                methods,
                jpaRepositoryType,
                jpaSpecificationExecutorType,
                querydslPredicateExecutorType,
                specificationType
        );
    }
}