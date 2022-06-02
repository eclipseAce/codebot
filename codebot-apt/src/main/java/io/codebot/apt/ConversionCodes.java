package io.codebot.apt;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.coding.CodeWriter;
import io.codebot.apt.coding.Expression;
import io.codebot.apt.coding.TypeOps;
import io.codebot.apt.coding.Variable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionCodes {
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private final TypeOps typeOps;
    private final MappingCodes mappingCodes;

    public static ConversionCodes instanceOf(ProcessingEnvironment processingEnv, MappingCodes mappingCodes) {
        return new ConversionCodes(TypeOps.instanceOf(processingEnv), mappingCodes);
    }

    public void convertAndReturn(CodeWriter writer, Entity entity, Variable sourceVar, TypeMirror returnType) {
        if (!typeOps.isVoid(returnType)) {
            if (!typeOps.isPrimitive(returnType)) {
                writer.beginControlFlow("if ($N == null)", sourceVar.getName());
                writer.write("return null;\n");
                writer.endControlFlow();
            }
            writer.write("return $L;\n", convert(writer, entity, sourceVar, returnType).getCode());
        }
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
            mappingCodes.copyProperties(writer, resultVar, Collections.singletonList(sourceVar));
            return resultVar;
        }
        throw new IllegalArgumentException("Can't convert to type " + targetType);
    }
}
