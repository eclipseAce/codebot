package io.codebot.apt.crud;

import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.type.*;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ServiceGenerator {

    public static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    public static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    public static final String PAGE_FQN = "org.springframework.data.domain.Page";
    public static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    public static final String TRANSACTIONAL_FQN = "org.springframework.transaction.annotation.Transactional";
    public static final String JPA_REPOSITORY_FQN = "org.springframework.data.jpa.repository.JpaRepository";
    public static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";
    public static final String JPA_SPECIFICATION_EXECUTOR_FQN = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";
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
                ), "specificationExecutor", Modifier.PRIVATE)
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
            builder.add("return $1N.$2N();\n", sourceVar, entity.getIdGetter().getExecutableName());
        } //
        else {
            String tempVar = names.newName("temp");
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

        String entityVar = names.newName("entity");
        builder.addAnnotation(ClassName.bestGuess(TRANSACTIONAL_FQN));
        builder.addCode("$1T $2N = new $1T();\n", entity.getType().getTypeMirror(), entityVar);
        for (Variable param : method.getParameters()) {
            Optional<SetAccessor> setter = entity.getType().findSetter(
                    param.getSimpleName(), param.getType()
            );
            if (setter.isPresent()) {
                builder.addCode("$1N.$2N($3N);\n",
                        entityVar, setter.get().getExecutableName(), names.get(param)
                );
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                entity.getType().findSetter(
                        getter.getAccessedName(), getter.getAccessedType()
                ).ifPresent(it -> builder.addCode("$1N.$2N($3N.$4N());\n",
                        entityVar, it.getExecutableName(),
                        names.get(param), getter.getExecutableName()
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
                        entityVar, setter.get().getExecutableName(), names.get(param)
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
                        entityVar, it.getExecutableName(),
                        names.get(param), getter.getExecutableName()
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

    public MethodSpec buildReadMethod(Service service, Entity entity, Executable method) {
        TypeFactory typeFactory = service.getType().getFactory();
        NameAllocator names = new NameAllocator();
        method.getParameters().forEach(it -> names.newName(it.getSimpleName(), it));
        MethodSpec.Builder builder = MethodSpec.overriding(
                method.getElement(),
                service.getType().asDeclaredType(),
                typeFactory.getTypeUtils()
        );

        Variable pageable = null;
        List<Predicate> predicates = Lists.newArrayList();
        for (Variable param : method.getParameters()) {
            if (pageable == null && param.getType().isAssignableTo(PAGEABLE_FQN)) {
                pageable = param;
            } else {
                collectPredicates(
                        predicates, Collections.emptyList(), CodeBlock.of("$N", param.getSimpleName()),
                        param.getSimpleName(), param.getType(), entity
                );
            }
        }

        Type resultType;
        String resultVar = names.newName("result");
        if (predicates.size() == 1 && predicates.get(0).kind == PredicateKind.ID) {
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
                    ParameterizedTypeName.get(ClassName.get(ArrayList.class), ClassName.bestGuess(PREDICATE_FQN)),
                    predicatesVar, predicates.size()
            );
            for (Predicate predicate : predicates) {
                specification.beginControlFlow("if ($L)", predicate.precondition);
                if (predicate.kind == PredicateKind.SPECIFICATION) {
                    specification.add("$1N.add($2L.toPredicate($3N, $4N, $5N));\n",
                            predicatesVar, predicate.expression, rootVar, queryVar, builderVar
                    );
                } //
                else {
                    specification.add("$1N.add($2N.equal($3N.get($4S), $5L));\n",
                            predicatesVar, builderVar, rootVar,
                            predicate.name, predicate.expression
                    );
                }
                specification.endControlFlow();
            }
            specification.add("return $1N.and($2N.toArray(new $3T[0]));\n",
                    builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN)
            );
            specification.add("$<}");

            Type returnType = method.getReturnType();
            if (pageable != null && returnType.erasure().isAssignableFrom(PAGE_FQN)) {
                resultType = typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror());
                builder.addCode("$1T $2N = this.specificationExecutor.findAll($3L, $4N);\n",
                        resultType.getTypeMirror(), resultVar, specification.build(), names.get(pageable)
                );
            } else if (returnType.erasure().isAssignableTo(Iterable.class.getName())) {
                resultType = typeFactory.getType(List.class.getName(), entity.getType().getTypeMirror());
                builder.addCode("$1T $2N = this.specificationExecutor.findAll($3L);\n",
                        resultType.getTypeMirror(), resultVar, specification.build()
                );
            } else {
                resultType = entity.getType();
                builder.addCode("$1T $2N = this.specificationExecutor" +
                                ".findAll($3L, $4T.ofSize(1))" +
                                ".stream().findFirst().orElse(null);\n",
                        resultType.getTypeMirror(), resultVar, specification.build(),
                        ClassName.bestGuess(PAGEABLE_FQN)
                );
                builder.beginControlFlow("if ($1N == null)", resultVar)
                        .addCode("return null;\n")
                        .endControlFlow();
            }
        }
        builder.addCode(returns(resultVar, resultType, method.getReturnType(), entity, names));
        return builder.build();
    }

    private void collectPredicates(List<Predicate> collected,
                                   List<CodeBlock> paths, CodeBlock path,
                                   String name, Type type, Entity entity) {
        if (type.isAssignableTo(SPECIFICATION_FQN, entity.getType().getTypeMirror())) {
            collected.add(new Predicate(name, PredicateKind.SPECIFICATION,
                    Stream.concat(paths.stream(), Stream.of(path)).collect(Collectors.toList())));
            return;
        }
        Optional<GetAccessor> directGetter = entity.getType().findGetter(name, type);
        if (directGetter.isPresent()) {
            boolean matchesId = entity.getIdName().equals(name)
                    && entity.getIdType().isAssignableTo(entity.getIdType());
            collected.add(new Predicate(name, matchesId ? PredicateKind.ID : PredicateKind.SIMPLE,
                    Stream.concat(paths.stream(), Stream.of(path)).collect(Collectors.toList())));
            return;
        }
        for (GetAccessor getter : type.getGetters()) {
            List<CodeBlock> nextPaths = Stream.concat(paths.stream(), Stream.of(path)).collect(Collectors.toList());
            CodeBlock nextPath = CodeBlock.of("$N()", getter.getExecutableName());
            collectPredicates(
                    collected, nextPaths, nextPath,
                    getter.getAccessedName(), getter.getAccessedType(), entity
            );
        }
    }

    private static class Predicate {
        final String name;
        final PredicateKind kind;
        final CodeBlock expression;
        final CodeBlock precondition;

        Predicate(String name, PredicateKind kind, List<CodeBlock> paths) {
            this.name = name;
            this.kind = kind;
            this.expression = CodeBlock.join(paths, ".");
            this.precondition = IntStream.range(0, paths.size()).boxed()
                    .map(i -> CodeBlock.of("$L != null", CodeBlock.join(paths.subList(0, i + 1), ".")))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> CodeBlock.join(list, " && ")
                    ));
        }
    }

    private enum PredicateKind {
        ID, SIMPLE, SPECIFICATION, QUERYDSL
    }
}
