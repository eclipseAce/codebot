package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.coding.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EntityServiceProcessor extends AbstractProcessor {
    private TypeOps typeOps;
    private Annotations annotationUtils;
    private Methods methodUtils;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(EntityService.class.getName());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeOps = TypeOps.instanceOf(processingEnv);
        this.annotationUtils = Annotations.instanceOf(processingEnv);
        this.methodUtils = Methods.instanceOf(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(EntityService.class))) {
            Annotation annotation = annotationUtils.find(element, EntityService.class);
            try {
                process(element, annotation);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        Throwables.getStackTraceAsString(e),
                        element, annotation.getMirror()
                );
            }
        }
        return false;
    }

    protected void process(TypeElement element, Annotation annotation) throws IOException {
        Entity entity = Entity.of(processingEnv, annotation.getType("value"));
        MethodCollection serviceMethods = methodUtils.allOf(typeOps.getDeclared(element));
        MethodCollection entityMethods = methodUtils.allOf(entity.getType());

        ClassName serviceName = ClassName.get(element);
        ClassName implementationName = ClassName.get(serviceName.packageName(), serviceName.simpleName() + "Impl");
        TypeSpec.Builder implementationBuilder = TypeSpec.classBuilder(implementationName)
                .addModifiers(Modifier.PUBLIC);
        if (element.getKind() == ElementKind.CLASS) {
            implementationBuilder.superclass(serviceName);
        } else if (element.getKind() == ElementKind.INTERFACE) {
            implementationBuilder.addSuperinterface(serviceName);
        } else {
            throw new IllegalArgumentException("EntityService must be class or interface");
        }

        for (Method serviceMethod : serviceMethods) {
            if (!serviceMethod.getModifiers().contains(Modifier.ABSTRACT)) {
                continue;
            }

            CrudType crudType = CrudType.match(serviceMethod);
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                    serviceMethod.getElement(),
                    serviceMethod.getContainingType(),
                    processingEnv.getTypeUtils()
            );
            NameAllocator names = new NameAllocator();
            methodBuilder.parameters.forEach(it -> names.newName(it.name));

            injectIfAbsent(implementationBuilder, "jpaRepository", ParameterizedTypeName.get(
                    ClassName.bestGuess("org.springframework.data.jpa.repository.JpaRepository"),
                    TypeName.get(entity.getType()),
                    TypeName.get(entity.getIdAttributeType()).box()
            ));

            injectIfAbsent(implementationBuilder, "querydslPredicateExecutor", ParameterizedTypeName.get(
                    ClassName.bestGuess("org.springframework.data.querydsl.QuerydslPredicateExecutor"),
                    TypeName.get(entity.getType())
            ));

            if (crudType == CrudType.CREATE || crudType == CrudType.UPDATE) {
                Variable entityVar = Variable.of(entity.getType(), names.newName("entity"));
                if (crudType == CrudType.CREATE) {
                    methodBuilder.addStatement("$1T $2N = new $1T()", entityVar.getType(), entityVar.getName());
                } else {
                    QueryMapping idSource = serviceMethod.getParameters().stream()
                            .flatMap(it -> QueryMapping.find(processingEnv, entity.getType(), it).stream())
                            .filter(it -> it.attribute.equals(entity.getIdAttribute()))
                            .findFirst().orElseThrow(() ->
                                    new IllegalArgumentException("Can't find a way to load entity with id")
                            );

                    methodBuilder.addStatement("$1T $2N = this.jpaRepository.getById($3L)",
                            entityVar.getType(), entityVar.getName(), idSource.value.getCode());
                }

                WriteMapping.find(processingEnv, entity.getType(), serviceMethod.getParameters()).forEach(source -> {
                    SetAttributeHandler handler = SetAttributeHandler.find(
                            processingEnv, entity, serviceMethods,
                            source.targetSetter.getWriteName(),
                            source.targetSetter.getWriteType()
                    );
                    if (handler != null) {
                        methodBuilder.addStatement(handler.invoke(entityVar, source.value));
                    } else {
                        methodBuilder.addStatement("$N.$N($L)",
                                entityVar.getName(),
                                source.targetSetter.getSimpleName(),
                                source.value.getCode());
                    }
                });

                methodBuilder.addStatement("this.jpaRepository.save($N)", entityVar.getName());
                convertAndReturn(methodBuilder, names, entity, entityVar, serviceMethod.getReturnType());
            } //
            else if (crudType == CrudType.QUERY) {
                List<Parameter> filterParams = Lists.newArrayList();
                List<Parameter> pageableParams = Lists.newArrayList();
                for (Parameter param : serviceMethod.getParameters()) {
                    if (typeOps.isAssignable(param.getType(), PAGEABLE_FQN)) {
                        pageableParams.add(param);
                    } else {
                        filterParams.add(param);
                    }
                }
                Variable filterVar = getFilterVariable(
                        methodBuilder, names, entity.getType(), serviceMethods, filterParams);
                Variable resultVar;
                if (typeOps.isAssignableToList(serviceMethod.getReturnType())) {
                    resultVar = Variable.of(
                            typeOps.getDeclared(Iterable.class.getName(), entity.getType()), names.newName("result"));
                    methodBuilder.addStatement(
                            "$T $N = this.querydslPredicateExecutor.findAll($N)",
                            resultVar.getType(), resultVar.getName(), filterVar.getName());
                } //
                else if (typeOps.isAssignable(serviceMethod.getReturnType(), PAGE_FQN) && !pageableParams.isEmpty()) {
                    resultVar = Variable.of(
                            typeOps.getDeclared(PAGE_FQN, entity.getType()), names.newName("result"));
                    methodBuilder.addStatement(
                            "$T $N = this.querydslPredicateExecutor.findAll($N, $N)",
                            resultVar.getType(), resultVar.getName(), filterVar.getName(),
                            pageableParams.get(0).getName());
                } //
                else {
                    resultVar = Variable.of(entity.getType(), names.newName("result"));
                    methodBuilder.addStatement(
                            "$T $N = this.querydslPredicateExecutor.findOne($N).orElse(null)",
                            resultVar.getType(), resultVar.getName(), filterVar.getName());
                }

                convertAndReturn(methodBuilder, names, entity, resultVar, serviceMethod.getReturnType());
            }

            implementationBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(implementationName.packageName(), implementationBuilder.build()).build()
                .writeTo(processingEnv.getFiler());
    }

    private Variable getFilterVariable(MethodSpec.Builder builder,
                                       NameAllocator names,
                                       DeclaredType entityType,
                                       MethodCollection siblingMethods,
                                       List<? extends Variable> sourceVars) {
        DeclaredType predicateType = typeOps.getDeclared(PREDICATE_FQN);
        DeclaredType booleanBuilderType = typeOps.getDeclared(BOOLEAN_BUILDER_FQN);

        Variable predicateVar = Variable.of(booleanBuilderType, names.newName("predicate"));
        builder.addStatement("$1T $2N = new $1T()", predicateVar.getType(), predicateVar.getName());

        Expression queryExpr = getQueryExpression(typeOps, entityType);
        sourceVars.stream()
                .flatMap(it -> QueryMapping.find(processingEnv, entityType, it).stream())
                .forEach(mapping -> {
                    CodeBlock stmt = CodeBlock.of("$N.and($L.$N.eq($L))",
                            predicateVar.getName(), queryExpr.getCode(),
                            mapping.attribute, mapping.value.getCode()
                    );
                    if (mapping.precondition != null) {
                        builder.beginControlFlow("if ($L)", mapping.precondition.getCode());
                        builder.addStatement(stmt);
                        builder.endControlFlow();
                    } else {
                        builder.addStatement(stmt);
                    }
                });

        Variable filterVar = Variable.of(predicateType, names.newName("filter"));
        builder.addStatement("$T $N = $N", filterVar.getType(), filterVar.getName(), predicateVar.getName());
        for (FilterHandler filterHandler : FilterHandler.findAll(processingEnv, siblingMethods)) {
            builder.addStatement("$N = $L", filterVar.getName(), filterHandler.invoke(filterVar).getCode());
        }
        return filterVar;
    }

    private void convertAndReturn(MethodSpec.Builder methodBuilder, NameAllocator names,
                                  Entity entity, Variable sourceVar, TypeMirror returnType) {
        if (typeOps.isVoid(returnType)) {
            return;
        }
        CodeBlock.Builder code = CodeBlock.builder();
        Expression expression = convert(code, names, entity, sourceVar, returnType);
        methodBuilder.addCode(code.build());
        methodBuilder.addCode("return $L;\n", expression.getCode());
    }

    private Expression convert(CodeBlock.Builder code, NameAllocator names,
                               Entity entity, Variable sourceVar, TypeMirror targetType) {
        if (typeOps.isAssignable(sourceVar.getType(), PAGE_FQN)
                && typeOps.isAssignable(typeOps.getDeclared(PAGE_FQN), typeOps.erasure(targetType))) {
            CodeBlock.Builder lambdaCode = CodeBlock.builder();
            NameAllocator lambdaNames = names.clone();

            Variable itVar = Variable.of(
                    typeOps.resolveTypeParameter((DeclaredType) sourceVar.getType(), PAGE_FQN, 0),
                    lambdaNames.newName("it")
            );
            lambdaCode.add("return $L;\n", convert(
                    lambdaCode, lambdaNames, entity, itVar,
                    typeOps.resolveTypeParameter((DeclaredType) targetType, PAGE_FQN, 0)
            ).getCode());
            return Expression.of(targetType, "$L.map($N -> {\n$>$L$<})",
                    sourceVar.getName(), itVar.getName(), lambdaCode.build());
        }
        if (typeOps.isAssignableToList(targetType) && typeOps.isAssignableToIterable(sourceVar.getType())) {
            CodeBlock.Builder lambdaCode = CodeBlock.builder();
            NameAllocator lambdaNames = names.clone();

            CodeBlock stream;
            if (typeOps.isAssignableToCollection(sourceVar.getType())) {
                stream = CodeBlock.of("$N.stream()", sourceVar.getName());
            } else {
                stream = CodeBlock.of("$T.stream($N.spliterator(), false)", StreamSupport.class, sourceVar.getName());
            }

            Variable itVar = Variable.of(
                    typeOps.resolveIterableElementType((DeclaredType) sourceVar.getType()),
                    lambdaNames.newName("it")
            );
            lambdaCode.add("return $L;\n", convert(
                    lambdaCode, lambdaNames, entity, itVar,
                    typeOps.resolveListElementType((DeclaredType) targetType)
            ).getCode());
            return Expression.of(targetType,
                    "$L.map($N -> {\n$>$L$<}).collect($T.toList())",
                    stream, itVar.getName(), lambdaCode.build(), Collectors.class
            );
        }
        if (typeOps.isSame(sourceVar.getType(), entity.getType())
                && typeOps.isAssignable(entity.getIdAttributeType(), targetType)) {
            return Expression.of(targetType, "$N.$N()",
                    sourceVar.getName(), entity.getIdReadMethod().getSimpleName());
        }
        if (typeOps.isDeclared(targetType)) {
            if (!typeOps.isPrimitive(targetType)) {
                code.beginControlFlow("if ($N == null)", sourceVar.getName());
                code.addStatement("return null");
                code.endControlFlow();
            }

            Variable resultVar = Variable.of(targetType, names.newName("result"));
            code.addStatement("$1T $2N = new $1T()", resultVar.getType(), resultVar.getName());

            WriteMapping.find(
                    processingEnv,
                    (DeclaredType) targetType,
                    Collections.singletonList(sourceVar)
            ).forEach(mapping -> {
                code.addStatement("$N.$N($L)",
                        resultVar.getName(),
                        mapping.targetSetter.getSimpleName(),
                        mapping.value.getCode());
            });

            return resultVar;
        }
        throw new IllegalArgumentException("Can't convert to type " + targetType);
    }

    private void injectIfAbsent(TypeSpec.Builder typeBuilder, String name, TypeName typeName) {
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

    private static Expression getQueryExpression(TypeOps typeOps, DeclaredType entityType) {
        ClassName entityTypeName = ClassName.get((TypeElement) entityType.asElement());
        ClassName queryTypeName = ClassName.get(entityTypeName.packageName(), "Q" + entityTypeName.simpleName());
        return Expression.of(typeOps.getDeclared(queryTypeName.canonicalName()),
                "$T.$N", queryTypeName, StringUtils.uncapitalize(entityTypeName.simpleName()));
    }

    private enum CrudType {
        CREATE, UPDATE, DELETE, QUERY, UNKNOWN;

        public static CrudType match(Method method) {
            if (method.getSimpleName().startsWith("create")) {
                return CREATE;
            }
            if (method.getSimpleName().startsWith("update")) {
                return UPDATE;
            }
            if (method.getSimpleName().startsWith("delete")) {
                return DELETE;
            }
            if (method.getSimpleName().startsWith("find")) {
                return QUERY;
            }
            return UNKNOWN;
        }
    }

    private interface SetAttributeHandler {
        CodeBlock invoke(Expression entityExpr, Expression valueExpr);

        static SetAttributeHandler find(ProcessingEnvironment processingEnv,
                                        Entity entity, MethodCollection methods,
                                        String attribute, TypeMirror attributeType) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);

            for (Method method : methods) {
                List<? extends Parameter> parameters = method.getParameters();
                Set<Modifier> modifiers = method.getModifiers();
                if (modifiers.contains(Modifier.ABSTRACT)
                        || modifiers.contains(Modifier.PRIVATE)
                        || !method.getSimpleName().equals("set" + StringUtils.capitalize(attribute))
                        || parameters.size() != 2
                        || !typeOps.isSame(parameters.get(0).getType(), entity.getType())
                        || !typeOps.isAssignable(parameters.get(1).getType(), attributeType)) {
                    continue;
                }
                return (entityExpr, valueExpr) -> CodeBlock.of("$N($L, $L)",
                        method.getSimpleName(), entityExpr.getCode(), valueExpr.getCode());
            }
            return null;
        }
    }

    private static final String PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_BASE_FQN = "com.querydsl.core.types.dsl.EntityPathBase";

    private interface FilterHandler {
        Expression invoke(Expression valueExpr);

        static List<FilterHandler> findAll(ProcessingEnvironment processingEnv,
                                           MethodCollection methods) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            List<FilterHandler> handlers = Lists.newArrayList();

            outer:
            for (Method method : methods) {
                List<? extends Parameter> params = method.getParameters();
                Set<Modifier> modifiers = method.getModifiers();
                if (modifiers.contains(Modifier.PRIVATE)
                        || !typeOps.isAssignable(method.getReturnType(), PREDICATE_FQN)
                        || params.size() < 1
                        || !typeOps.isAssignable(params.get(0).getType(), PREDICATE_FQN)) {
                    continue;
                }
                List<Expression> extraParams = Lists.newArrayList();
                for (int i = 1; i < params.size(); i++) {
                    Parameter param = params.get(i);
                    if (typeOps.isAssignable(param.getType(), ENTITY_PATH_BASE_FQN)) {
                        DeclaredType entityType = (DeclaredType) typeOps.resolveTypeParameter(
                                (DeclaredType) param.getType(), ENTITY_PATH_BASE_FQN, 0);
                        extraParams.add(getQueryExpression(typeOps, entityType));
                    } else {
                        continue outer;
                    }
                }
                handlers.add(valueExpr -> Expression.of(
                        params.get(0).getType(),
                        "$N($L, $L)",
                        method.getSimpleName(), valueExpr.getCode(),
                        extraParams.stream().map(Expression::getCode).collect(CodeBlock.joining(", "))
                ));
            }
            return handlers;
        }
    }

    @RequiredArgsConstructor
    private static class WriteMapping {
        public final WriteMethod targetSetter;
        public final Expression value;

        public static List<WriteMapping> find(ProcessingEnvironment processingEnv,
                                              DeclaredType targetType,
                                              List<? extends Variable> variables) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            Methods methodUtils = Methods.instanceOf(processingEnv);
            MethodCollection entityMethods = methodUtils.allOf(targetType);

            List<WriteMapping> writeMappings = Lists.newArrayList();
            for (Variable variable : variables) {
                WriteMethod entitySetter = entityMethods.findWriter(variable.getName(), variable.getType());
                if (entitySetter != null) {
                    writeMappings.add(new WriteMapping(entitySetter, variable));
                    continue;
                }
                if (typeOps.isDeclared(variable.getType())) {
                    for (ReadMethod paramGetter : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                        entitySetter = entityMethods.findWriter(paramGetter.getReadName(), paramGetter.getReadType());
                        if (entitySetter != null) {
                            writeMappings.add(new WriteMapping(entitySetter, paramGetter.toExpression(variable)));
                        }
                    }
                }
            }
            return writeMappings;
        }
    }

    @RequiredArgsConstructor
    private static class QueryMapping {
        public final String attribute;
        public final Expression value;
        public final Expression precondition;

        public static List<QueryMapping> find(ProcessingEnvironment processingEnv,
                                              DeclaredType entityType, Variable variable) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            Methods methodUtils = Methods.instanceOf(processingEnv);
            MethodCollection entityMethods = methodUtils.allOf(entityType);

            ReadMethod entityGetter = entityMethods.findReader(variable.getName(), variable.getType());
            if (entityGetter != null) {
                Expression precondition = typeOps.isPrimitive(variable.getType())
                        ? null : Expression.of(typeOps.getBooleanType(), "$N != null", variable.getName());
                return Collections.singletonList(new QueryMapping(entityGetter.getReadName(), variable, precondition));
            }
            if (typeOps.isDeclared(variable.getType())) {
                List<QueryMapping> queryMappings = Lists.newArrayList();
                for (ReadMethod paramGetter : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                    entityGetter = entityMethods.findReader(paramGetter.getReadName(), paramGetter.getReadType());
                    if (entityGetter != null) {
                        Expression precondition;
                        if (typeOps.isPrimitive(paramGetter.getReadType())) {
                            precondition = Expression.of(typeOps.getBooleanType(), "$N != null", variable.getName());
                        } else {
                            precondition = Expression.of(typeOps.getBooleanType(), "$N != null && $L != null",
                                    variable.getName(), paramGetter.toExpression(variable));
                        }
                        queryMappings.add(new QueryMapping(
                                entityGetter.getReadName(), paramGetter.toExpression(variable), precondition
                        ));
                    }
                }
                return Collections.unmodifiableList(queryMappings);
            }
            return Collections.emptyList();
        }
    }
}
