package io.codebot.apt;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
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
    private static final String PAGE_IMPL_FQN = "org.springframework.data.domain.PageImpl";

    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_BASE_FQN = "com.querydsl.core.types.dsl.EntityPathBase";
    private static final String JPA_QUERY_FQN = "com.querydsl.jpa.impl.JPAQuery";
    private static final String QUERY_RESULTS_FQN = "com.querydsl.core.QueryResults";

    private static final String ENTITY_MANAGER_FQN = "javax.persistence.EntityManager";
    private static final String PERSISTENCE_CONTEXT_FQN = "javax.persistence.PersistenceContext";

    private static final String ENTITY_MANAGER_FIELD = "entityManager";

    private final TypeOps typeOps;
    private final Methods methodUtils;
    private final TypeSpec.Builder typeBuilder;
    private final MethodCollection contextMethods;

    public static QuerydslCodes instanceOf(ProcessingEnvironment processingEnv,
                                           TypeSpec.Builder typeBuilder, MethodCollection contextMethods) {
        return new QuerydslCodes(
                TypeOps.instanceOf(processingEnv),
                Methods.instanceOf(processingEnv),
                typeBuilder, contextMethods
        );
    }

    public Variable createPredicate(CodeWriter writer, Entity entity, List<? extends Variable> sources) {
        return createPredicate(writer, entity, sources, it -> true);
    }

    public Variable createPredicate(CodeWriter writer, Entity entity, List<? extends Variable> sources,
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
                    appendPredicate(writer, entity, builderVar, entityGetter.getReadName(), source);
                }
                continue;
            }
            if (typeOps.isDeclared(source.getType())) {
                CodeWriter tempWriter = writer.newWriter();
                for (ReadMethod getter : methodUtils.allOf((DeclaredType) source.getType()).readers()) {
                    entityGetter = entityMethods.findReader(getter.getReadName(), getter.getReadType());
                    if (entityGetter != null && propertyFilter.test(entityGetter)) {
                        appendPredicate(tempWriter, entity, builderVar,
                                entityGetter.getReadName(), getter.toExpression(source));
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

    private void appendPredicate(CodeWriter writer, Entity entity, Variable builder, String property, Expression value) {
        CodeBlock code = CodeBlock.of("$N.and($L.$N.eq($L));\n",
                builder.getName(), getQType(entity.getType()).getCode(), property, value.getCode());
        if (!typeOps.isPrimitive(value.getType())) {
            writer.beginControlFlow("if ($L != null)", value.getCode());
            writer.write(code);
            writer.endControlFlow();
        } else {
            writer.write(code);
        }
    }

    public void saveEntity(CodeWriter writer, Expression entity) {
        writer.write("this.$N.persist($L);\n", getEntityManagerField(), entity.getCode());
    }

    public Variable findAllEntities(CodeWriter writer, Entity entity, Expression predicate, Expression pageable) {
        DeclaredType queryResultsType = typeOps.getDeclared(QUERY_RESULTS_FQN, entity.getType());
        DeclaredType pageImplType = typeOps.getDeclared(PAGE_IMPL_FQN, entity.getType());

        Variable queryVar = createJPAQuery(writer, entity, predicate);
        writer.write("$1N.offset($2L.getOffset()).limit($2L.getPageSize());\n",
                queryVar.getName(), pageable.getCode());
        Variable resultsVar = writer.writeNewVariable("results", queryResultsType,
                CodeBlock.of("$N.fetchResults()", queryVar.getName()));
        return writer.writeNewVariable("page", pageImplType,
                CodeBlock.of("new $1T($2N.getResults(), $3L, $2N.getTotal())",
                        pageImplType, resultsVar.getName(), pageable.getCode()));
    }

    public Variable findAllEntities(CodeWriter writer, Entity entity, Expression predicate) {
        DeclaredType resultType = typeOps.getDeclared(List.class.getName(), entity.getType());
        Variable queryVar = createJPAQuery(writer, entity, predicate);
        return writer.writeNewVariable("list", resultType,
                CodeBlock.of("$N.fetch()", queryVar.getName()));
    }

    public Variable findOneEntity(CodeWriter writer, Entity entity, Expression predicate) {
        Variable queryVar = createJPAQuery(writer, entity, predicate);

        return writer.writeNewVariable("entity", entity.getType(),
                CodeBlock.of("$N.fetchOne()", queryVar.getName()));
    }

    private Variable createJPAQuery(CodeWriter writer, Entity entity, Expression predicate) {
        DeclaredType jpaQueryType = typeOps.getDeclared(JPA_QUERY_FQN, entity.getType());
        Variable queryVar = writer.writeNewVariable("query", jpaQueryType,
                CodeBlock.of("new $T($N)", jpaQueryType, getEntityManagerField()));
        writer.write("$1N.select($2L).from($2L);\n",
                queryVar.getName(), getQType(entity.getType()).getCode());
        if (predicate != null) {
            writer.write("$N.where($L);\n", queryVar.getName(), predicate.getCode());
        }
        return queryVar;
    }

    private Expression getQType(DeclaredType entityType) {
        ClassName entityName = ClassName.get((TypeElement) entityType.asElement());
        ClassName queryName = ClassName.get(entityName.packageName(), "Q" + entityName.simpleName());
        return Expression.of(typeOps.getDeclared(queryName.canonicalName()),
                "$T.$N", queryName, StringUtils.uncapitalize(entityName.simpleName()));
    }

    private String getEntityManagerField() {
        ClassName entityManagerName = ClassName.bestGuess(ENTITY_MANAGER_FQN);
        for (FieldSpec field : typeBuilder.fieldSpecs) {
            if (field.type.equals(entityManagerName)) {
                return field.name;
            }
        }
        typeBuilder.addField(FieldSpec
                .builder(entityManagerName, ENTITY_MANAGER_FIELD, Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(PERSISTENCE_CONTEXT_FQN))
                .build()
        );
        return ENTITY_MANAGER_FIELD;
    }
}
