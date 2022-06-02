package io.codebot.apt;

import com.squareup.javapoet.*;
import io.codebot.apt.coding.CodeWriter;
import io.codebot.apt.coding.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.List;
import java.util.function.Predicate;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuerydslCodes {
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_BASE_FQN = "com.querydsl.core.types.dsl.EntityPathBase";

    private static final String JPA_REPOSITORY_FIELD = "jpaRepository";
    private static final String QUERYDSL_PREDICATE_EXECUTOR_FIELD = "querydslPredicateExecutor";

    private final TypeOps typeOps;
    private final Methods methodUtils;
    private final TypeSpec.Builder typeBuilder;
    private final Entity entity;
    private final MethodCollection contextMethods;

    public static QuerydslCodes instanceOf(ProcessingEnvironment processingEnv,
                                           TypeSpec.Builder typeBuilder, Entity entity, MethodCollection contextMethods) {
        return new QuerydslCodes(
                TypeOps.instanceOf(processingEnv),
                Methods.instanceOf(processingEnv),
                typeBuilder, entity, contextMethods
        );
    }

    public Variable createPredicate(CodeWriter writer, List<? extends Variable> sources) {
        return createPredicate(writer, sources, it -> true);
    }

    public Variable createPredicate(CodeWriter writer, List<? extends Variable> sources,
                                    Predicate<ReadMethod> propertyFilter) {
        MethodCollection entityMethods = methodUtils.allOf(entity.getType());
        DeclaredType booleanBuilderType = typeOps.getDeclared(BOOLEAN_BUILDER_FQN);
        DeclaredType predicateType = typeOps.getDeclared(PREDICATE_FQN);

        Variable builderVar = writer.writeNewVariable("builder", booleanBuilderType,
                CodeBlock.of("new $T()", booleanBuilderType));
        for (Variable source : sources) {
            ReadMethod entityGetter = entityMethods.findReader(source.getName(), source.getType());
            if (entityGetter != null) {
                if (propertyFilter.test(entityGetter)) {
                    addPredicate(writer, builderVar, entityGetter.getReadName(), source);
                }
                continue;
            }
            if (typeOps.isDeclared(source.getType())) {
                CodeWriter tempWriter = writer.newWriter();
                for (ReadMethod getter : methodUtils.allOf((DeclaredType) source.getType()).readers()) {
                    entityGetter = entityMethods.findReader(getter.getReadName(), getter.getReadType());
                    if (entityGetter != null && propertyFilter.test(entityGetter)) {
                        addPredicate(tempWriter, builderVar, entityGetter.getReadName(), getter.toExpression(source));
                    }
                }
                if (!tempWriter.isEmpty()) {
                    writer.beginControlFlow("if ($N != null)", source.getName());
                    writer.write(tempWriter.toCode());
                    writer.endControlFlow();
                }
            }
        }
        Variable predicateVar = null;
        for (Method contextMethod : contextMethods) {
            if (!typeOps.isAssignable(contextMethod.getReturnType(), PREDICATE_FQN)) {
                continue;
            }
            List<? extends Parameter> params = contextMethod.getParameters();
            if (params.size() != 1 || !typeOps.isAssignable(predicateType, params.get(0).getType())) {
                continue;
            }
            if (predicateVar == null) {
                predicateVar = writer.writeNewVariable("predicate", predicateType,
                        CodeBlock.of("super.$N($N)", contextMethod.getSimpleName(), builderVar.getName()));
            } else {
                writer.write("$N = super.$N($N);\n",
                        predicateVar.getName(), contextMethod.getSimpleName(), predicateVar.getName());
            }
        }
        return predicateVar != null ? predicateVar : builderVar;
    }

    private void addPredicate(CodeWriter writer, Variable builder, String property, Expression value) {
        CodeBlock code = CodeBlock.of("$N.and($L.$N.eq($L));\n",
                builder.getName(), getQueryExpression(entity.getType()).getCode(), property, value.getCode());
        if (!typeOps.isPrimitive(value.getType())) {
            writer.beginControlFlow("if ($L != null)", value.getCode());
            writer.write(code);
            writer.endControlFlow();
        } else {
            writer.write(code);
        }
    }

    public void saveEntity(CodeWriter writer, Expression entity) {
        writer.write("this.$N.save($L);\n", jpaRepositoryField(), entity.getCode());
    }

    public Variable findAllEntities(CodeWriter writer, Expression predicate, Expression pageable) {
        if (pageable != null) {
            DeclaredType entityPageType = typeOps.getDeclared(PAGE_FQN, entity.getType());
            if (predicate == null) {
                return writer.writeNewVariable("result", entityPageType,
                        CodeBlock.of("this.$N.findAll($L)", jpaRepositoryField(), pageable.getCode()));
            }
            return writer.writeNewVariable("result", entityPageType,
                    CodeBlock.of("this.$N.findAll($L, $L)",
                            querydslPredicateExecutorField(), predicate.getCode(), pageable.getCode()));
        }
        if (predicate != null) {
            return writer.writeNewVariable("result", typeOps.getDeclared(Iterable.class.getName(), entity.getType()),
                    CodeBlock.of("this.$N.findAll($L)", querydslPredicateExecutorField(), predicate.getCode()));
        }
        return writer.writeNewVariable("result", typeOps.getDeclared(List.class.getName(), entity.getType()),
                CodeBlock.of("this.$N.findAll()", jpaRepositoryField()));
    }

    public Variable findOneEntity(CodeWriter writer, Expression predicate) {
        return writer.writeNewVariable("entity", entity.getType(),
                CodeBlock.of("this.$N.findOne($L).orElse(null)",
                        querydslPredicateExecutorField(), predicate.getCode()));
    }

    private Expression getQueryExpression(DeclaredType entityType) {
        ClassName entityName = ClassName.get((TypeElement) entityType.asElement());
        ClassName queryName = ClassName.get(entityName.packageName(), "Q" + entityName.simpleName());
        return Expression.of(typeOps.getDeclared(queryName.canonicalName()),
                "$T.$N", queryName, StringUtils.uncapitalize(entityName.simpleName()));
    }

    private String jpaRepositoryField() {
        injectIfAbsent(JPA_REPOSITORY_FIELD, ParameterizedTypeName.get(
                ClassName.bestGuess("org.springframework.data.jpa.repository.JpaRepository"),
                TypeName.get(entity.getType()),
                TypeName.get(entity.getIdAttributeType()).box()
        ));
        return JPA_REPOSITORY_FIELD;
    }

    private String querydslPredicateExecutorField() {
        injectIfAbsent(QUERYDSL_PREDICATE_EXECUTOR_FIELD, ParameterizedTypeName.get(
                ClassName.bestGuess("org.springframework.data.querydsl.QuerydslPredicateExecutor"),
                TypeName.get(entity.getType())
        ));
        return QUERYDSL_PREDICATE_EXECUTOR_FIELD;
    }

    private void injectIfAbsent(String name, TypeName typeName) {
        for (FieldSpec field : typeBuilder.fieldSpecs) {
            if (field.name.equals(name)) {
                return;
            }
        }
        typeBuilder.addField(FieldSpec
                .builder(typeName, name, Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess("org.springframework.beans.factory.annotation.Autowired"))
                .build()
        );
    }
}
