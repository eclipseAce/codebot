package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.cruder.apt.type.Accessor;
import io.cruder.apt.type.Type;
import io.cruder.apt.type.TypeFactory;
import io.cruder.apt.util.AnnotationUtils;

import javax.lang.model.element.*;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CrudService {
    public static final String ANNOTATION_FQN = "io.cruder.CrudService";
    public static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    public static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    public static final String PAGE_FQN = "org.springframework.data.domain.Page";
    public static final String LIST_FQN = "java.util.List";

    private final Type serviceType;
    private final Entity entity;
    private final Type repositoryType;

    public CrudService(TypeFactory typeFactory, TypeElement typeElement) {
        this.serviceType = typeFactory.getType(typeElement.asType());

        AnnotationMirror annotation = AnnotationUtils.find(typeElement, ANNOTATION_FQN)
                .orElseThrow(() -> new IllegalArgumentException("No @CrudService present"));
        this.entity = new Entity(typeFactory.getType(
                AnnotationUtils.<TypeMirror>findValue(annotation, "entity").get()
        ));
        this.repositoryType = typeFactory.getType(
                AnnotationUtils.<TypeMirror>findValue(annotation, "repository").get()
        );
    }

    public JavaFile createJavaFile() {
        ClassName className = ClassName.get(serviceType.asTypeElement());
        ClassName implementedClassName = ClassName.get(className.packageName(), className.simpleName() + "Impl");

        TypeSpec.Builder builder = TypeSpec.classBuilder(implementedClassName);
        builder.addModifiers(Modifier.PUBLIC);
        if (serviceType.isInterface()) {
            builder.addSuperinterface(className);
        } else {
            builder.superclass(className);
        }
        builder.addField(FieldSpec
                .builder(
                        ClassName.get(repositoryType.asTypeElement()),
                        "repository",
                        Modifier.PRIVATE
                )
                .addAnnotation(ClassName.get("javax.annotation", "Resource"))
                .build());

        serviceType.getMethods().stream()
                .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                .map(method -> new Method(serviceType, method))
                .forEach(method -> {
                    String name = method.element.getSimpleName().toString();
                    if (name.startsWith("create")) {
                        method.builder.addCode(buildCreateCode(method));
                    } else if (name.startsWith("update")) {
                        method.builder.addCode(buildUpdateCode(method));
                    } else if (name.startsWith("find")) {
                        method.builder.addCode(buildReadCode(method));
                    }
                    builder.addMethod(method.builder.build());
                });

        return JavaFile.builder(implementedClassName.packageName(), builder.build()).build();
    }

    CodeBlock buildCreateCode(Method method) {
        String entityVar = method.nameAllocator.newName("entity");
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement("$1T $2N = new $1T()", entity.type.asTypeMirror(), entityVar);
        for (Parameter param : method.parameters) {
            Accessor directSetter = entity.type
                    .findWriteAccessor(param.name, param.type.asTypeMirror())
                    .orElse(null);
            if (directSetter != null) {
                builder.addStatement("$1N.$2N($3N)",
                        entityVar,
                        directSetter.getSimpleName(),
                        param.name
                );
            } else {
                buildMappingCodes(param.name, param.type, entityVar, entity.type)
                        .forEach(builder::addStatement);
            }
        }
        buildReturnCodes(method, entityVar)
                .forEach(builder::addStatement);
        return builder.build();
    }

    CodeBlock buildUpdateCode(Method method) {
        String entityVar = method.nameAllocator.newName("entity");
        CodeBlock.Builder builder = CodeBlock.builder();
        CodeBlock idExpression = null;
        List<CodeBlock> mappingExpressions = Lists.newArrayList();
        for (Parameter param : method.parameters) {
            if (idExpression == null && param.name.equals(entity.idName)
                    && param.type.isAssignableTo(entity.idType.asTypeMirror())) {
                idExpression = CodeBlock.of("$1N", param.name);
                continue;
            }
            Optional<Accessor> directSetter = entity.type.findWriteAccessor(param.name, param.type.asTypeMirror());
            if (directSetter.isPresent()) {
                mappingExpressions.add(CodeBlock.of("$1N.$2N($3N)",
                        entityVar,
                        directSetter.get().getSimpleName(),
                        param.name
                ));
                continue;
            }
            for (Accessor getter : param.type.findReadAccessors()) {
                if (idExpression == null
                        && entity.idName.equals(getter.getAccessedName())
                        && entity.idType.isAssignableFrom(getter.getAccessedType())) {
                    idExpression = CodeBlock.of("$1N.$2N()", param.name, getter.getSimpleName());
                } else {
                    entity.type.findWriteAccessor(
                            getter.getAccessedName(),
                            getter.getAccessedType()
                    ).ifPresent(setter -> {
                        mappingExpressions.add(buildMappingCode(param.name, getter, entityVar, setter));
                    });
                }
            }
        }
        if (idExpression == null) {
            throw new IllegalArgumentException("Can't find a way to load entity");
        }
        builder.addStatement("$1T $2N = repository.getById($3L)",
                entity.type.asTypeMirror(),
                entityVar,
                idExpression
        );
        mappingExpressions.forEach(builder::addStatement);

        buildReturnCodes(method, entityVar)
                .forEach(builder::addStatement);

        return builder.build();
    }

    CodeBlock buildReadCode(Method method) {
        ReadingContext ctx = ReadingContext.fromMethod(entity, method);
        if (ctx.returnsPage && !ctx.findWithPageable) {
            throw new IllegalArgumentException("Method returns Page but no Pageable parameter present");
        }

        CodeBlock.Builder methodBody = CodeBlock.builder();
        String resultVar = method.nameAllocator.newName("result");
        if (ctx.findByIdFromDirectValue || ctx.findByIdFromNestedValue) {
            CodeBlock idExpression = ctx.findByIdFromDirectValue
                    ? CodeBlock.of("$N", ctx.directValues.get(0).name)
                    : CodeBlock.of("$1N.$2N()", ctx.nestedValues.get(0).getKey(), ctx.nestedValues.get(0).getValue());
            methodBody.addStatement(
                    "$1T $2N = repository.getById($3L)",
                    entity.typeName, resultVar, idExpression
            );
        } else {
            String specVar = method.nameAllocator.newName("spec");
            String specRootVar = method.nameAllocator.newName("root");
            String specQueryVar = method.nameAllocator.newName("query");
            String specBuilderVar = method.nameAllocator.newName("builder");

            List<CodeBlock> predicates = Stream.of(
                    ctx.directValues.stream().map(param -> CodeBlock.of(
                            "$1N.equal($2N.get($3S), $3L)",
                            specBuilderVar, specRootVar, param.name
                    )),
                    ctx.nestedValues.stream().map(entry -> CodeBlock.of(
                            "$1N.equal($2N.get($3S), $3N.$4N())",
                            specBuilderVar, specRootVar, entry.getKey().name, entry.getValue().getSimpleName()
                    )),
                    ctx.specifications.stream().map(spec -> CodeBlock.of(
                            "$1N.toPredicate($2N, $3N, $4N)",
                            spec.name, specRootVar, specQueryVar, specBuilderVar
                    ))
            ).reduce(Stream::concat).map(s -> s.collect(Collectors.toList())).orElseGet(Collections::emptyList);

            methodBody.add(
                    "$1T $2N = ($3N, $4N, $5N) -> {\n$>return $5N.and(\n$>$6L\n$<);\n$<};\n",
                    ParameterizedTypeName.get(ClassName.bestGuess(SPECIFICATION_FQN), entity.typeName),
                    specVar, specRootVar, specQueryVar, specBuilderVar,
                    CodeBlock.join(predicates, ",\n")
            );

            if (ctx.returnsPage) {
                methodBody.addStatement(
                        "$1T $2N = repository.findAll($3N, $4N)",
                        ParameterizedTypeName.get(
                                ClassName.bestGuess(PAGE_FQN),
                                TypeName.get(entity.type.asTypeMirror())
                        ),
                        resultVar, specVar, ctx.pageables.get(0).name
                );
            } else if (ctx.returnsList) {
                methodBody.addStatement(
                        "$1T $2N = repository.findAll($3N)",
                        ParameterizedTypeName.get(
                                ClassName.bestGuess(LIST_FQN),
                                TypeName.get(entity.type.asTypeMirror())
                        ),
                        resultVar, specVar
                );
            } else {
                methodBody.addStatement(
                        "$1T $2N = repository.findOne($3N).orElse(null)",
                        entity.typeName, resultVar, specVar
                );
            }
        }

        if (ctx.returnsPage) {
            Type componentType = method.returnType.getTypeArguments().get(0);
            String sourceVar = method.nameAllocator.newName("source");
            String targetVar = method.nameAllocator.newName("target");

            CodeBlock.Builder mapperBody = CodeBlock.builder();
            mapperBody.addStatement("$1T $2N = new $1T()", componentType.asTypeMirror(), targetVar);
            buildMappingCodes(sourceVar, entity.type, targetVar, componentType)
                    .forEach(mapperBody::addStatement);
            mapperBody.addStatement("return $1N", targetVar);

            methodBody.add("return $1N.map($2N -> {\n$>$3L$<});", resultVar, sourceVar, mapperBody.build());
        } else if (ctx.returnsList) {
            // TODO: handle list return value
        } else {
            String targetVar = method.nameAllocator.newName("target");
            methodBody.beginControlFlow("if ($N == null)", resultVar)
                    .addStatement("return null")
                    .endControlFlow();
            methodBody.addStatement("$1T $2N = new $1T()", method.returnType.asTypeMirror(), targetVar);
            buildMappingCodes(resultVar, entity.type, targetVar, method.returnType)
                    .forEach(methodBody::addStatement);
            methodBody.addStatement("return $1N", targetVar);
        }
        return methodBody.build();
    }

    List<CodeBlock> buildReturnCodes(Method method, String entityVar) {
        List<CodeBlock> codes = Lists.newArrayList();
        codes.add(CodeBlock.of("repository.save($N)", entityVar));
        if (method.returnType.isAssignableFrom(entity.idType.asTypeMirror())
                && entity.idReadAccessor != null) {
            codes.add(CodeBlock.of("return $1N.$2N()", entityVar, entity.idReadAccessor.getSimpleName()));
        } //
        else if (method.returnType.isDeclared()) {
            String resultVar = method.nameAllocator.newName("result");
            codes.add(CodeBlock.of("$1T $2N = new $1T()", method.returnType.asTypeMirror(), resultVar));
            codes.addAll(buildMappingCodes(entityVar, entity.type, resultVar, method.returnType));
            codes.add(CodeBlock.of("return $N", resultVar));
        }
        return codes;
    }

    List<CodeBlock> buildMappingCodes(String fromName, Type fromType, String toName, Type toType) {
        List<CodeBlock> codes = Lists.newArrayList();
        for (Accessor getter : fromType.findReadAccessors()) {
            toType.findWriteAccessor(getter.getAccessedName(), getter.getAccessedType()).ifPresent(setter -> {
                codes.add(buildMappingCode(fromName, getter, toName, setter));
            });
        }
        return codes;
    }

    private CodeBlock buildMappingCode(String fromName, Accessor getter, String toName, Accessor setter) {
        return CodeBlock.of("$1N.$2N($3N.$4N())",
                toName,
                setter.getSimpleName(),
                fromName,
                getter.getSimpleName()
        );
    }

    static class Method {
        final ExecutableElement element;
        final ExecutableType type;
        final Type containingType;
        final Type returnType;
        final List<Parameter> parameters;

        final NameAllocator nameAllocator;
        final MethodSpec.Builder builder;

        Method(Type containingType, ExecutableElement element) {
            this.element = element;
            this.type = containingType.asMember(element);
            this.containingType = containingType;
            this.returnType = containingType.getFactory().getType(type.getReturnType());
            this.parameters = Parameter.fromMethod(containingType, element, type);

            this.nameAllocator = new NameAllocator();
            parameters.forEach(param -> nameAllocator.newName(param.name));
            this.builder = MethodSpec.overriding(
                    element,
                    containingType.asDeclaredType(),
                    containingType.getFactory().getTypeUtils()
            );
        }
    }

    static class Parameter {
        final VariableElement element;
        final String name;
        final Type type;

        Parameter(VariableElement element, Type type) {
            this.element = element;
            this.name = element.getSimpleName().toString();
            this.type = type;
        }

        static List<Parameter> fromMethod(Type containing,
                                          ExecutableElement method,
                                          ExecutableType methodType) {
            return IntStream.range(0, method.getParameters().size()).boxed()
                    .map(i -> new Parameter(
                            method.getParameters().get(i),
                            containing.getFactory().getType(methodType.getParameterTypes().get(i))
                    ))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        }
    }

    static class ReadingContext {
        final List<Parameter> directValues;
        final List<Map.Entry<Parameter, Accessor>> nestedValues;
        final List<Parameter> specifications;
        final List<Parameter> pageables;
        final boolean findAll;
        final boolean findByIdFromDirectValue;
        final boolean findByIdFromNestedValue;
        final boolean findWithPageable;

        final Type returnType;
        final boolean returnsValue;
        final boolean returnsList;
        final boolean returnsPage;

        ReadingContext(List<Parameter> directValues,
                       List<Map.Entry<Parameter, Accessor>> nestedValues,
                       List<Parameter> specifications,
                       List<Parameter> pageables,
                       boolean findAll,
                       boolean findByIdFromDirectValue,
                       boolean findByIdFromNestedValue,
                       boolean findWithPageable,
                       Type returnType,
                       boolean returnsValue,
                       boolean returnsList,
                       boolean returnsPage) {
            this.directValues = directValues;
            this.nestedValues = nestedValues;
            this.specifications = specifications;
            this.pageables = pageables;
            this.findAll = findAll;
            this.findByIdFromDirectValue = findByIdFromDirectValue;
            this.findByIdFromNestedValue = findByIdFromNestedValue;
            this.findWithPageable = findWithPageable;
            this.returnType = returnType;
            this.returnsValue = returnsValue;
            this.returnsList = returnsList;
            this.returnsPage = returnsPage;
        }

        static ReadingContext fromMethod(Entity entity, Method method) {
            List<Parameter> directValues = Lists.newArrayList();
            List<Map.Entry<Parameter, Accessor>> nestedValues = Lists.newArrayList();
            List<Parameter> specifications = Lists.newArrayList();
            List<Parameter> pageables = Lists.newArrayList();
            for (Parameter param : method.parameters) {
                Optional<Accessor> directGetter = entity.type.findReadAccessor(param.name, param.type.asTypeMirror());
                if (directGetter.isPresent()) {
                    directValues.add(param);
                    continue;
                }
                if (param.type.isSubtype(SPECIFICATION_FQN, entity.type.asTypeMirror())) {
                    specifications.add(param);
                    continue;
                }
                if (param.type.isSubtype(PAGEABLE_FQN)) {
                    pageables.add(param);
                    continue;
                }
                for (Accessor getter : param.type.findReadAccessors()) {
                    Optional<Accessor> entityGetter = entity.type
                            .findReadAccessor(getter.getAccessedName(), getter.getAccessedType());
                    if (entityGetter.isPresent()) {
                        nestedValues.add(new AbstractMap.SimpleImmutableEntry<>(param, getter));
                    }
                }
            }

            Type returnType = method.returnType;
            boolean returnsList = returnType.erasure().isAssignableFrom(LIST_FQN);
            boolean returnsPage = returnType.erasure().isAssignableFrom(PAGE_FQN);
            boolean returnsValue = !returnsList && !returnsPage;

            boolean findAll = directValues.isEmpty() && nestedValues.isEmpty() && specifications.isEmpty();
            boolean findByIdFromDirectValue = returnsValue
                    && directValues.size() == 1
                    && specifications.isEmpty()
                    && nestedValues.isEmpty()
                    && entity.idName.equals(directValues.get(0).name);
            boolean findByIdFromNestedValue = returnsValue
                    && nestedValues.size() == 1
                    && specifications.isEmpty()
                    && directValues.isEmpty()
                    && entity.idName.equals(nestedValues.get(0).getValue().getAccessedName());
            boolean findWithPageable = !pageables.isEmpty();

            return new ReadingContext(
                    ImmutableList.copyOf(directValues),
                    ImmutableList.copyOf(nestedValues),
                    ImmutableList.copyOf(specifications),
                    ImmutableList.copyOf(pageables),
                    findAll, findByIdFromDirectValue, findByIdFromNestedValue, findWithPageable,
                    returnType,
                    returnsValue, returnsList, returnsPage
            );
        }
    }
}
