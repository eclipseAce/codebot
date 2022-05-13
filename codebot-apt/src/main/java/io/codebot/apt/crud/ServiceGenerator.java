package io.codebot.apt.crud;

import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.type.*;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;

public class ServiceGenerator {

    public static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    public static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    public static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    public static final String LIST_FQN = "java.util.List";
    public static final String ITERABLE_FQN = "java.lang.Iterable";
    public static final String ARRAY_LIST_FQN = "java.util.ArrayList";
    public static final String PAGE_FQN = "org.springframework.data.domain.Page";
    public static final String STREAM_SUPPORT_FQN = "java.util.stream.StreamSupport";
    public static final String COLLECTORS_FQN = "java.util.stream.Collectors";

    public JavaFile generate(Service service, Entity entity) {
        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder(service.getImplTypeName());
        serviceBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        serviceBuilder.addAnnotation(ClassName.bestGuess("org.springframework.stereotype.Service"));
        if (service.getType().isInterface()) {
            serviceBuilder.addSuperinterface(service.getTypeName());
        } else {
            serviceBuilder.superclass(service.getTypeName());
        }
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess("org.springframework.data.jpa.repository.JpaRepository"),
                        entity.getTypeName(),
                        entity.getIdTypeName().box()
                ), "repository", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess("org.springframework.beans.factory.annotation.Autowired"))
                .build()
        );
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess("org.springframework.data.jpa.repository.JpaSpecificationExecutor"),
                        entity.getTypeName()
                ), "specificationExecutor", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess("org.springframework.beans.factory.annotation.Autowired"))
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
                serviceBuilder.addMethod(buildReadMethod(service, entity, method));
            }
        }
        return JavaFile.builder(service.getImplTypeName().packageName(), serviceBuilder.build()).build();
    }

    private CodeBlock returns(String sourceVar, Type sourceType, Type returnType,
                              Entity entity, NameAllocator names) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (returnType.erasure().isAssignableFrom(PAGE_FQN)
                && sourceType.erasure().isAssignableFrom(PAGE_FQN)) {
            String itVar = names.newName("it", "it");
            builder.add("return $1N.map($2N -> {\n$>", sourceVar, itVar);
            builder.add(returns(
                    itVar,
                    sourceType.getTypeArguments().get(0),
                    returnType.getTypeArguments().get(0),
                    entity, names.clone()
            ));
            builder.add("$<});\n");
        } //
        else if (returnType.erasure().isAssignableFrom(LIST_FQN)
                && sourceType.erasure().isAssignableTo(ITERABLE_FQN)) {
            String itVar = names.newName("it", "it");
            builder.add("return $1T.stream($2N.spliterator(), false).map($3N -> {\n$>",
                    ClassName.bestGuess(STREAM_SUPPORT_FQN), sourceVar, itVar);
            builder.add(returns(
                    itVar,
                    sourceType.getTypeArguments().get(0),
                    returnType.getTypeArguments().get(0),
                    entity, names.clone()
            ));
            builder.add("$<}).collect($1T.toList());\n", ClassName.bestGuess(COLLECTORS_FQN));
        } //
        else if (sourceType.equals(entity.getType())
                && returnType.isAssignableFrom(entity.getIdType())) {
            builder.add("return $1N.$2N();\n",
                    sourceVar, entity.getIdGetter().getExecutableName()
            );
        } //
        else {
            String tempVar = names.newName("temp", "temp");
            builder.add("$1T $2N = new $1T();\n",
                    returnType.getTypeMirror(), tempVar
            );
            for (SetAccessor setter : returnType.getSetters()) {
                sourceType.findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                        builder.add("$1N.$2N($3N.$4N());\n",
                                tempVar, setter.getExecutableName(), sourceVar, it.getExecutableName()
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

        builder.addAnnotation(ClassName.bestGuess("org.springframework.transaction.annotation.Transactional"));
        builder.addCode("$1T $2N = new $1T();\n",
                entity.getType().getTypeMirror(), names.newName("entity", "entity")
        );
        for (Variable param : method.getParameters()) {
            Optional<SetAccessor> setter = entity.getType().findSetter(
                    param.getSimpleName(), param.getType()
            );
            if (setter.isPresent()) {
                builder.addCode("$1N.$2N($3N);\n",
                        names.get("entity"), setter.get().getExecutableName(), names.get(param)
                );
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                entity.getType().findSetter(
                        getter.getAccessedName(), getter.getAccessedType()
                ).ifPresent(it -> builder.addCode("$1N.$2N($3N.$4N());\n",
                        names.get("entity"), it.getExecutableName(),
                        names.get(param), getter.getExecutableName()
                ));
            }
        }
        builder.addCode("this.repository.save($1N);\n", names.get("entity"));
        if (!method.getReturnType().isVoid()) {
            builder.addCode(returns(
                    names.get("entity"), entity.getType(), method.getReturnType(), entity, names
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

        String entityVar = names.newName("entity", "entity");
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
                        names.get("entity"), setter.get().getExecutableName(), names.get(param)
                );
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                if (entity.getIdName().equals(getter.getAccessedName())
                        && entity.getIdType().isAssignableFrom(getter.getAccessedType())) {
                    if (entityLoad == null) {
                        entityLoad = CodeBlock.of("$1T $2N = this.repository.getById($3N.$4N());\n",
                                entity.getType().getTypeMirror(), entityVar,
                                names.get(param), getter.getExecutableName()
                        );
                    }
                    continue;
                }
                entity.getType().findSetter(
                        getter.getAccessedName(), getter.getAccessedType()
                ).ifPresent(it -> propertySets.add("$1N.$2N($3N.$4N());\n",
                        names.get("entity"), it.getExecutableName(),
                        names.get(param), getter.getExecutableName()
                ));
            }
        }
        if (entityLoad == null) {
            throw new IllegalArgumentException("Can't find a way to load entity");
        }
        builder.addCode(entityLoad);
        builder.addCode(propertySets.build());
        builder.addCode("this.repository.save($1N);\n", names.get("entity"));
        if (!method.getReturnType().isVoid()) {
            builder.addCode(returns(
                    names.get("entity"), entity.getType(), method.getReturnType(), entity, names
            ));
        }
        return builder.build();
    }

    public MethodSpec buildReadMethod(Service service, Entity entity, Executable method) {
        NameAllocator names = new NameAllocator();
        method.getParameters().forEach(it -> names.newName(it.getSimpleName(), it));
        MethodSpec.Builder builder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                service.getType().getFactory().getTypeUtils()
        );

        Variable pageable = null;
        List<PredicateCandicate> predicates = Lists.newArrayList();
        for (Variable param : method.getParameters()) {
            String paramVar = names.get(param);
            if (pageable == null && param.getType().isAssignableTo(PAGEABLE_FQN)) {
                pageable = param;
                continue;
            }
            if (param.getType().isAssignableTo(SPECIFICATION_FQN, entity.getType().getTypeMirror())) {
                predicates.add(new PredicateCandicate(
                        param, null, null, true,
                        CodeBlock.of("$1N", paramVar),
                        CodeBlock.of("$1N != null", paramVar)
                ));
                continue;
            }
            Optional<GetAccessor> directGetter = entity.getType().findGetter(paramVar, param.getType());
            if (directGetter.isPresent()) {
                predicates.add(new PredicateCandicate(
                        param, paramVar, param.getType(), false,
                        CodeBlock.of("$1N", paramVar),
                        CodeBlock.of("$1N != null", paramVar)
                ));
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).ifPresent(it ->
                        predicates.add(new PredicateCandicate(
                                param, getter.getAccessedName(), getter.getAccessedType(), false,
                                CodeBlock.of("$1N.$2N()", paramVar, getter.getExecutableName()),
                                CodeBlock.of("$1N != null && $1N.$2N() != null", paramVar, getter.getExecutableName())
                        ))
                );
            }
        }

        Type resultType;
        String resultVar = names.newName("result");
        if (predicates.size() == 1 && predicates.get(0).isIdPredicate(entity)) {
            resultType = entity.getType();
            builder.addCode("$1T $2N = this.repository.getById($3L);\n",
                    entity.getType().getTypeMirror(), resultVar, predicates.get(0).expression
            );
        } else {
            String rootVar = names.newName("root");
            String queryVar = names.newName("query");
            String builderVar = names.newName("builder");
            String predicatesVar = names.newName("predicates");

            CodeBlock.Builder specification = CodeBlock.builder();
            specification.add("($1N, $2N, $3N) -> {$>\n", rootVar, queryVar, builderVar);
            specification.add("$1T $2N = new $1T($3L);\n",
                    ParameterizedTypeName.get(ClassName.bestGuess(ARRAY_LIST_FQN), ClassName.bestGuess(PREDICATE_FQN)),
                    predicatesVar, predicates.size()
            );
            for (PredicateCandicate predicate : predicates) {
                specification.beginControlFlow("if ($L)", predicate.precheck);
                if (predicate.predicate) {
                    specification.add("$1N.add($2L.toPredicate($3N, $4N, $5N));\n",
                            predicatesVar, predicate.expression, rootVar, queryVar, builderVar
                    );
                } else {
                    specification.add("$1N.add($2N.equal($3N.get($4S), $5L));\n",
                            predicatesVar, builderVar, rootVar, predicate.name, predicate.expression
                    );
                }
                specification.endControlFlow();
            }
            specification.add("return $1N.and($2N.toArray(new $3T[0]));\n",
                    builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN)
            );
            specification.add("$<}");

            if (pageable != null) {
                resultType = service.getType().getFactory().getType(PAGE_FQN, entity.getType().getTypeMirror());
                builder.addCode("$1T $2N = this.specificationExecutor.findAll($3L, $4N);\n",
                        resultType.getTypeMirror(), resultVar, specification.build(), names.get(pageable)
                );
            } else {
                resultType = service.getType().getFactory().getType(LIST_FQN, entity.getType().getTypeMirror());
                builder.addCode("$1T $2N = this.specificationExecutor.findAll($3L);\n",
                        resultType.getTypeMirror(), resultVar, specification.build()
                );
            }
        }
        builder.addCode(returns(
                resultVar, resultType, method.getReturnType(), entity, names
        ));
        return builder.build();
    }

    private static class PredicateCandicate {
        final Variable parameter;
        final String name;
        final Type type;
        final boolean predicate;
        final CodeBlock expression;
        final CodeBlock precheck;

        public PredicateCandicate(Variable parameter,
                                  String name,
                                  Type type,
                                  boolean predicate,
                                  CodeBlock expression,
                                  CodeBlock precheck) {
            this.parameter = parameter;
            this.name = name;
            this.type = type;
            this.predicate = predicate;
            this.expression = expression;
            this.precheck = precheck;
        }

        boolean isIdPredicate(Entity entity) {
            return name != null && type != null
                    && name.equals(entity.getIdName())
                    && type.isAssignableTo(entity.getIdType());
        }
    }
}
