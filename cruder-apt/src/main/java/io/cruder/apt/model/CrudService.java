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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CrudService {
    public static final String ANNOTATION_FQN = "io.cruder.CrudService";

    private final Type serviceType;
    private final Entity entity;
    private final Type repositoryType;

    public CrudService(TypeFactory typeFactory, TypeElement typeElement) {
        this.serviceType = typeFactory.getType(typeElement.asType());

        AnnotationMirror annotation = AnnotationUtils.findAnnotation(typeElement, ANNOTATION_FQN)
                .orElseThrow(() -> new IllegalArgumentException("No @CrudService present"));
        this.entity = new Entity(
                typeFactory.getType(AnnotationUtils.<TypeMirror>findValue(annotation, "entity").get())
        );
        this.repositoryType = typeFactory.getType(AnnotationUtils
                .<TypeMirror>findValue(annotation, "repository").get());
    }

    public JavaFile createJavaFile() {
        ClassName className = ClassName.get(serviceType.getTypeElement());
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
                        ClassName.get(repositoryType.getTypeElement()),
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
                    }
                    builder.addMethod(method.builder.build());
                });

        return JavaFile.builder(implementedClassName.packageName(), builder.build()).build();
    }

    CodeBlock buildCreateCode(Method method) {
        String entityVar = method.nameAllocator.newName("entity");
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement("$1T $2N = new $1T()", entity.type.getTypeMirror(), entityVar);
        for (MethodParameter param : method.parameters) {
            Accessor directSetter = entity.type
                    .findWriteAccessor(param.name, param.type.getTypeMirror())
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
        for (MethodParameter param : method.parameters) {
            if (idExpression == null && param.name.equals(entity.idName)
                    && param.type.isAssignableTo(entity.idType.getTypeMirror())) {
                idExpression = CodeBlock.of("$1N", param.name);
                continue;
            }
            Optional<Accessor> directSetter = entity.type.findWriteAccessor(param.name, param.type.getTypeMirror());
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
                    continue;
                }
                entity.type.findWriteAccessor(getter.getAccessedName(), getter.getAccessedType()).ifPresent(setter -> {
                    mappingExpressions.add(buildMappingCode(param.name, getter, entityVar, setter));
                });
            }
        }
        if (idExpression == null) {
            throw new IllegalArgumentException("Can't find a way to load entity");
        }
        builder.addStatement("$1T $2N = repository.getById($3L)",
                entity.type.getTypeMirror(),
                entityVar,
                idExpression
        );
        mappingExpressions.forEach(builder::addStatement);

        buildReturnCodes(method, entityVar)
                .forEach(builder::addStatement);

        return builder.build();
    }

    List<CodeBlock> buildReturnCodes(Method method, String entityVar) {
        List<CodeBlock> codes = Lists.newArrayList();
        codes.add(CodeBlock.of("repository.save($N)", entityVar));
        if (method.returnType.isAssignableFrom(entity.idType.getTypeMirror())
                && entity.idReadAccessor != null) {
            codes.add(CodeBlock.of("return $1N.$2N()", entityVar, entity.idReadAccessor.getSimpleName()));
        } //
        else if (method.returnType.isDeclared()) {
            String resultVar = method.nameAllocator.newName("result");
            codes.add(CodeBlock.of("$1T $2N = new $1T()", method.returnType.getTypeMirror(), resultVar));
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

    static class Entity {
        final Type type;
        final Type idType;
        final String idName;
        final Accessor idReadAccessor;

        Entity(Type type) {
            this.type = type;
            VariableElement idField = type
                    .findFields(it -> AnnotationUtils.isAnnotationPresent(it, "javax.persistence.Id"))
                    .stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Can't determin ID of entity"));
            this.idType = type.getTypeFactory().getType(type.asMember(idField));
            this.idName = idField.getSimpleName().toString();
            this.idReadAccessor = type.findReadAccessor(idName, idType.getTypeMirror()).orElse(null);
        }
    }

    static class Method {
        final ExecutableElement element;
        final ExecutableType type;
        final Type containingType;
        final Type returnType;
        final List<MethodParameter> parameters;

        final NameAllocator nameAllocator;
        final MethodSpec.Builder builder;

        Method(Type containingType, ExecutableElement element) {
            this.element = element;
            this.type = containingType.asMember(element);
            this.containingType = containingType;
            this.returnType = containingType.getTypeFactory().getType(type.getReturnType());
            this.parameters = MethodParameter.fromMethod(containingType, element, type);

            this.nameAllocator = new NameAllocator();
            parameters.forEach(param -> nameAllocator.newName(param.name));
            this.builder = MethodSpec.overriding(
                    element,
                    containingType.getDeclaredType(),
                    containingType.getTypeUtils()
            );
        }
    }

    static class MethodParameter {
        final VariableElement element;
        final String name;
        final Type type;

        MethodParameter(VariableElement element, Type type) {
            this.element = element;
            this.name = element.getSimpleName().toString();
            this.type = type;
        }

        static List<MethodParameter> fromMethod(Type containing,
                                                ExecutableElement method,
                                                ExecutableType methodType) {
            return IntStream.range(0, method.getParameters().size()).boxed()
                    .map(i -> new MethodParameter(
                            method.getParameters().get(i),
                            containing.getTypeFactory().getType(methodType.getParameterTypes().get(i))
                    ))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        }
    }
}
