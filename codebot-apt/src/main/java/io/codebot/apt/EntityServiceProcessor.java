package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.coding.CodeWriter;
import io.codebot.apt.coding.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

            CodeWriter writer = CodeWriter.create(methodBuilder);
            BeanCodes beanCodes = BeanCodes.instanceOf(processingEnv, serviceMethods);
            ConversionCodes conversionCodes = ConversionCodes.instanceOf(processingEnv, beanCodes);

            if (crudType == CrudType.CREATE || crudType == CrudType.UPDATE) {
                Variable entityVar = Variable.of(entity.getType(), names.newName("entity"));
                if (crudType == CrudType.CREATE) {
                    writer.write("$1T $2N = new $1T();\n", entityVar.getType(), entityVar.getName());
                } else {
                    QueryMapping idSource = serviceMethod.getParameters().stream()
                            .flatMap(it -> QueryMapping.find(processingEnv, entity.getType(), it).stream())
                            .filter(it -> it.attribute.equals(entity.getIdAttribute()))
                            .findFirst().orElseThrow(() ->
                                    new IllegalArgumentException("Can't find a way to load entity with id")
                            );

                    writer.write("$1T $2N = this.jpaRepository.getById($3L);\n",
                            entityVar.getType(), entityVar.getName(), idSource.value.getCode());
                }
                beanCodes.setProperties(writer, entityVar, serviceMethod.getParameters());
                writer.write("this.jpaRepository.save($N);\n", entityVar.getName());
                conversionCodes.convertAndReturn(writer, entity, entityVar, serviceMethod.getReturnType());
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
                Variable filterVar = getFilterVariable(writer, entity.getType(), serviceMethods, filterParams);
                Variable resultVar;
                if (typeOps.isAssignableToList(serviceMethod.getReturnType())) {
                    resultVar = Variable.of(
                            typeOps.getDeclared(Iterable.class.getName(), entity.getType()), names.newName("result"));
                    writer.write(
                            "$T $N = this.querydslPredicateExecutor.findAll($N);\n",
                            resultVar.getType(), resultVar.getName(), filterVar.getName());
                } //
                else if (typeOps.isAssignable(serviceMethod.getReturnType(), PAGE_FQN) && !pageableParams.isEmpty()) {
                    resultVar = Variable.of(
                            typeOps.getDeclared(PAGE_FQN, entity.getType()), names.newName("result"));
                    writer.write(
                            "$T $N = this.querydslPredicateExecutor.findAll($N, $N);\n",
                            resultVar.getType(), resultVar.getName(), filterVar.getName(),
                            pageableParams.get(0).getName());
                } //
                else {
                    resultVar = Variable.of(entity.getType(), names.newName("result"));
                    writer.write(
                            "$T $N = this.querydslPredicateExecutor.findOne($N).orElse(null);\n",
                            resultVar.getType(), resultVar.getName(), filterVar.getName());
                }
                conversionCodes.convertAndReturn(writer, entity, resultVar, serviceMethod.getReturnType());
            }

            methodBuilder.addCode(writer.toCode());
            implementationBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(implementationName.packageName(), implementationBuilder.build()).build()
                .writeTo(processingEnv.getFiler());
    }

    private Variable getFilterVariable(CodeWriter writer,
                                       DeclaredType entityType,
                                       MethodCollection siblingMethods,
                                       List<? extends Variable> sourceVars) {
        DeclaredType predicateType = typeOps.getDeclared(PREDICATE_FQN);
        DeclaredType booleanBuilderType = typeOps.getDeclared(BOOLEAN_BUILDER_FQN);

        Variable predicateVar = Variable.of(booleanBuilderType, writer.newName("predicate"));
        writer.write("$1T $2N = new $1T();\n", predicateVar.getType(), predicateVar.getName());

        Expression queryExpr = getQueryExpression(typeOps, entityType);
        sourceVars.stream()
                .flatMap(it -> QueryMapping.find(processingEnv, entityType, it).stream())
                .forEach(mapping -> {
                    CodeBlock stmt = CodeBlock.of("$N.and($L.$N.eq($L));\n",
                            predicateVar.getName(), queryExpr.getCode(),
                            mapping.attribute, mapping.value.getCode()
                    );
                    if (mapping.precondition != null) {
                        writer.beginControlFlow("if ($L)", mapping.precondition.getCode());
                        writer.write(stmt);
                        writer.endControlFlow();
                    } else {
                        writer.write(stmt);
                    }
                });

        Variable filterVar = Variable.of(predicateType, writer.newName("filter"));
        writer.write("$T $N = $N;\n", filterVar.getType(), filterVar.getName(), predicateVar.getName());
        for (FilterHandler filterHandler : FilterHandler.findAll(processingEnv, siblingMethods)) {
            writer.write("$N = $L;\n", filterVar.getName(), filterHandler.invoke(filterVar).getCode());
        }
        return filterVar;
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
