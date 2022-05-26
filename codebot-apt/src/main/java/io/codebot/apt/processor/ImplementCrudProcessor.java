package io.codebot.apt.processor;

import com.squareup.javapoet.*;
import io.codebot.apt.code.CodeWriter;
import io.codebot.apt.code.*;
import io.codebot.apt.processor.crud.Entity;
import io.codebot.apt.processor.crud.Mapping;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

public class ImplementCrudProcessor extends AbstractAnnotatedElementProcessor {
    private static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";
    private static final String COMPONENT_FQN = "org.springframework.stereotype.Component";

    @Override
    public void process(Element element, AnnotationMirror annotationMirror) throws Exception {
        Annotation annotation = annotationUtils.of(annotationMirror);
        Entity entity = Entity.resolve(processingEnv, annotation.getType("entity"));

        ClassName abstractName = ClassName.get((TypeElement) element);
        ClassName implementedName = ClassName.get(abstractName.packageName(), abstractName.simpleName() + "Impl");
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(implementedName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.bestGuess(COMPONENT_FQN));
        if (element.getKind() == ElementKind.INTERFACE) {
            classBuilder.addSuperinterface(element.asType());
        } else {
            classBuilder.superclass(element.asType());
        }

        List<Method> abstractMethods = methodUtils.allOf((DeclaredType) element.asType()).stream()
                .filter(it -> it.getModifiers().contains(Modifier.ABSTRACT))
                .collect(Collectors.toList());
        for (Method abstractMethod : abstractMethods) {
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                    abstractMethod.getElement(),
                    abstractMethod.getContainingType(),
                    processingEnv.getTypeUtils()
            );

            if (abstractMethod.getSimpleName().startsWith("create")) {
                processCreateMethod(entity, abstractMethod, methodBuilder, classBuilder);
            } //
            else if (abstractMethod.getSimpleName().startsWith("update")) {
                processUpdateMethod(entity, abstractMethod, methodBuilder, classBuilder);
            }

            classBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(implementedName.packageName(), classBuilder.build()).build()
                .writeTo(processingEnv.getFiler());
    }

    private CodeBlock getJpaRepository(Entity entity, TypeSpec.Builder classBuilder) {
        boolean exists = classBuilder.fieldSpecs.stream()
                .anyMatch(it -> it.name.equals("jpaRepository"));
        if (!exists) {
            classBuilder.addField(FieldSpec
                    .builder(TypeName.get(entity.getJpaRepositoryType()), "jpaRepository", Modifier.PRIVATE)
                    .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                    .build());
        }
        return CodeBlock.of("this.jpaRepository");
    }

    private Expression findIdExpression(Entity entity, List<? extends Variable> sources) {
        for (Variable variable : sources) {
            if (variable.getName().equals(entity.getIdName())
                    && typeOps.isAssignable(variable.getType(), entity.getIdType())) {
                return variable;
            }
            if (typeOps.isDeclared(variable.getType())) {
                for (ReadMethod reader : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                    if (reader.getReadName().equals(entity.getIdName())
                            && typeOps.isAssignable(reader.getReadType(), entity.getIdType())) {
                        return reader.toExpression(variable);
                    }
                }
            }
        }
        return null;
    }

    private void handleReturn(Entity entity, Method abstractMethod, CodeWriter code, Variable entityVar) {
        TypeMirror returnType = abstractMethod.getReturnType();
        if (typeOps.isAssignable(entity.getIdType(), returnType)) {
            code.write("return $L;\n", entity.getIdReadMethod().toExpression(entityVar).getCode());
        } //
        else if (!typeOps.isVoid(returnType)) {
            Variable resultVar = code.writeNewVariable("result",
                    Expression.of(returnType, "new $T()", returnType));
            Mapping.map(processingEnv, returnType, entityVar, methodUtils.allOf(entity.getType()).readers())
                    .forEach(it -> it.writeTo(code, resultVar));
            code.write("return $N;\n", resultVar.getName());
        }
    }

    private void processCreateMethod(Entity entity,
                                     Method abstractMethod,
                                     MethodSpec.Builder methodBuilder,
                                     TypeSpec.Builder classBuilder) {
        CodeWriter code = CodeWriter.create();
        Variable entityVar = code.writeNewVariable("entity",
                Expression.of(entity.getType(), "new $T()", entity.getType()));
        Mapping.map(processingEnv, entity.getType(), abstractMethod.getParameters())
                .forEach(it -> it.writeTo(code, entityVar));
        code.write("$L.save($N);\n", getJpaRepository(entity, classBuilder), entityVar.getName());
        handleReturn(entity, abstractMethod, code, entityVar);
        methodBuilder.addCode(code.getCode());
    }

    private void processUpdateMethod(Entity entity,
                                     Method abstractMethod,
                                     MethodSpec.Builder methodBuilder,
                                     TypeSpec.Builder classBuilder) {
        Expression idExpr = findIdExpression(entity, abstractMethod.getParameters());
        if (idExpr == null) {
            return;
        }
        CodeWriter code = CodeWriter.create();
        Variable entityVar = code.writeNewVariable("entity", Expression.of(entity.getType(),
                "$L.getById($L)", getJpaRepository(entity, classBuilder), idExpr.getCode()
        ));
        Mapping.map(processingEnv, entity.getType(), abstractMethod.getParameters()).stream()
                .filter(it -> !it.getTargetWriteMethod().getWriteName().equals(entity.getIdName()))
                .forEach(it -> it.writeTo(code, entityVar));
        code.write("$L.save($N);\n", getJpaRepository(entity, classBuilder), entityVar.getName());
        handleReturn(entity, abstractMethod, code, entityVar);
        methodBuilder.addCode(code.getCode());
    }
}
