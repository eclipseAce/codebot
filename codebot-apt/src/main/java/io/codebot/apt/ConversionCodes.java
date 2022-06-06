package io.codebot.apt;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.coding.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionCodes {
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private final TypeOps typeOps;
    private final Methods methodUtils;
    private final MethodCollection contextMethods;

    public static ConversionCodes instanceOf(ProcessingEnvironment processingEnv, MethodCollection contextMethods) {
        return new ConversionCodes(TypeOps.instanceOf(processingEnv), Methods.instanceOf(processingEnv), contextMethods);
    }

    public void convertAndReturn(CodeWriter writer, Entity entity, Variable sourceVar, TypeMirror returnType) {
        if (typeOps.isVoid(returnType)) {
            return;
        }
        if (!typeOps.isPrimitive(returnType)) {
            writer.beginControlFlow("if ($N == null)", sourceVar.getName());
            writer.write("return null;\n");
            writer.endControlFlow();
        }
        writer.write("return $L;\n", convert(writer, entity, sourceVar, returnType).getCode());
    }

    public Expression convert(CodeWriter writer, Entity entity, Variable sourceVar, TypeMirror targetType) {
        if (typeOps.isAssignable(sourceVar.getType(), PAGE_FQN)
                && typeOps.isAssignable(typeOps.getDeclared(PAGE_FQN), typeOps.erasure(targetType))) {
            CodeWriter lambdaWriter = writer.newWriter();

            Variable itVar = Variable.of(
                    typeOps.resolveTypeParameter((DeclaredType) sourceVar.getType(), PAGE_FQN, 0),
                    lambdaWriter.newName("it")
            );
            TypeMirror elementType = typeOps.resolveTypeParameter((DeclaredType) targetType, PAGE_FQN, 0);
            lambdaWriter.write("return $L;\n", convert(lambdaWriter, entity, itVar, elementType).getCode());

            return Expression.of(targetType, "$L.map($N -> {\n$>$L$<})",
                    sourceVar.getName(), itVar.getName(), lambdaWriter.toCode());
        }
        if (typeOps.isAssignableToList(targetType) && typeOps.isAssignableToIterable(sourceVar.getType())) {
            CodeWriter lambdaWriter = writer.newWriter();

            CodeBlock stream;
            if (typeOps.isAssignableToCollection(sourceVar.getType())) {
                stream = CodeBlock.of("$N.stream()", sourceVar.getName());
            } else {
                stream = CodeBlock.of("$T.stream($N.spliterator(), false)", StreamSupport.class, sourceVar.getName());
            }

            Variable itVar = Variable.of(
                    typeOps.resolveIterableElementType((DeclaredType) sourceVar.getType()),
                    lambdaWriter.newName("it")
            );
            TypeMirror elementType = typeOps.resolveListElementType((DeclaredType) targetType);
            lambdaWriter.write("return $L;\n", convert(lambdaWriter, entity, itVar, elementType).getCode());

            return Expression.of(targetType,
                    "$L.map($N -> {\n$>$L$<}).collect($T.toList())",
                    stream, itVar.getName(), lambdaWriter.toCode(), Collectors.class
            );
        }
        if (typeOps.isSame(sourceVar.getType(), entity.getType())
                && typeOps.isAssignable(entity.getIdAttributeType(), targetType)) {
            return Expression.of(targetType, "$N.$N()",
                    sourceVar.getName(), entity.getIdReadMethod().getSimpleName());
        }
        if (typeOps.isDeclared(targetType)) {
            Variable resultVar = writer.writeNewVariable("temp", targetType,
                    CodeBlock.of("new $T()", targetType));
            copyProperties(writer, resultVar, Collections.singletonList(sourceVar));
            return resultVar;
        }
        throw new IllegalArgumentException("Can't convert to type " + targetType);
    }

    public void copyProperties(CodeWriter writer, Variable target, List<? extends Variable> sources) {
        if (!typeOps.isDeclared(target.getType())) {
            return;
        }
        MethodCollection targetMethods = methodUtils.allOf((DeclaredType) target.getType());
        for (Variable source : sources) {
            WriteMethod targetSetter = targetMethods.findWriter(source.getName(), source.getType());
            if (targetSetter != null) {
                setProperty(writer, target, source.getName(), targetSetter, source);
                continue;
            }
            if (typeOps.isDeclared(source.getType())) {
                for (ReadMethod sourceGetter : methodUtils.allOf((DeclaredType) source.getType()).readers()) {
                    targetSetter = targetMethods.findWriter(sourceGetter.getReadName(), sourceGetter.getReadType());
                    if (targetSetter != null) {
                        setProperty(writer, target, sourceGetter.getReadName(), targetSetter,
                                sourceGetter.toExpression(source));
                    }
                }
            }
        }
    }

    private void setProperty(CodeWriter writer, Variable target, String property,
                             WriteMethod propertySetter, Expression propertyValue) {
        for (Method contextMethod : contextMethods) {
            Set<Modifier> modifiers = contextMethod.getModifiers();
            if (modifiers.contains(Modifier.ABSTRACT) || modifiers.contains(Modifier.PRIVATE)) {
                continue;
            }
            if (!contextMethod.getSimpleName().equals("set" + StringUtils.capitalize(property))) {
                continue;
            }
            List<? extends Parameter> params = contextMethod.getParameters();
            if (params.size() != 2
                    || !typeOps.isSame(params.get(0).getType(), target.getType())
                    || !typeOps.isSame(params.get(1).getType(), propertyValue.getType())) {
                continue;
            }
            writer.write("super.$N($N, $L);\n", contextMethod.getSimpleName(),
                    target.getName(), propertyValue.getCode());
            return;
        }
        writer.write("$N.$N($L);\n", target.getName(), propertySetter.getSimpleName(), propertyValue.getCode());
    }
}
