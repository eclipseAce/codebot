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
            }

            classBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(implementedName.packageName(), classBuilder.build()).build()
                .writeTo(processingEnv.getFiler());
    }

    private void processCreateMethod(Entity entity,
                                     Method abstractMethod,
                                     MethodSpec.Builder methodBuilder,
                                     TypeSpec.Builder classBuilder) {
        CodeWriter writer = CodeWriter.create();

        Variable entityVar = writer.writerNewVariable("entity",
                Expression.of(entity.getType(), "new $T()", entity.getType()));

        Mapping.map(
                processingEnv,
                entity.getType(),
                abstractMethod.getParameters()
        ).forEach(it -> it.writeTo(writer, entityVar.asExpression()));

        writer.write("$L.save($N);\n", getJpaRepository(entity, classBuilder), entityVar.getName());

        TypeMirror returnType = abstractMethod.getReturnType();
        if (typeOps.isAssignable(entity.getIdType(), returnType)) {
            writer.write("return $L;\n", entity.getIdReadMethod().toExpression(entityVar.asExpression()).getCode());
        } //
        else if (typeOps.isVoid(returnType)) {
            Variable resultVar = writer.writerNewVariable("result",
                    Expression.of(returnType, "new $T()", returnType));

            Mapping.map(
                    processingEnv,
                    returnType,
                    entityVar.asExpression(),
                    methodUtils.allOf(entity.getType()).readers()
            ).forEach(it -> it.writeTo(writer, resultVar.asExpression()));

            writer.write("return $N;\n", resultVar.getName());
        }
        methodBuilder.addCode(writer.getCode());
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
}
