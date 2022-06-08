package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.Entity;
import io.codebot.apt.model.*;
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

public class CodeFactory {
    private final ProcessingEnvironment processingEnv;
    private final TypeOps typeOps;
    private final Fields fieldUtils;
    private final Methods methodUtils;
    private final Annotations annotationUtils;

    public CodeFactory(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.typeOps = TypeOps.instanceOf(processingEnv);
        this.fieldUtils = Fields.instanceOf(processingEnv);
        this.methodUtils = Methods.instanceOf(processingEnv);
        this.annotationUtils = Annotations.instanceOf(processingEnv);
    }

    public CodeSnippet<Variable> newVariable(String nameSuggestion, TypeMirror type, CodeBlock initial) {
        return writer -> {
            Variable variable = Variable.of(type, writer.newName(nameSuggestion));
            if (initial != null) {
                writer.write("$T $N = $L;\n", variable.getType(), variable.getName(), initial);
            } else {
                writer.write("$T $N;\n", variable.getType(), variable.getName());
            }
            return variable;
        };
    }

    public CodeSnippet<Void> setProperty(Variable target, WriteMethod setter, Expression value,
                                         MethodCollection contextMethods) {
        return writer -> {
            for (Method contextMethod : contextMethods) {
                Set<Modifier> modifiers = contextMethod.getModifiers();
                if (modifiers.contains(Modifier.ABSTRACT) || modifiers.contains(Modifier.PRIVATE)) {
                    continue;
                }
                if (!contextMethod.getSimpleName().equals("set" + StringUtils.capitalize(setter.getWriteName()))) {
                    continue;
                }
                List<? extends Parameter> params = contextMethod.getParameters();
                if (params.size() != 2
                        || !typeOps.isSame(params.get(0).getType(), target.getType())
                        || !typeOps.isSame(params.get(1).getType(), value.getType())) {
                    continue;
                }
                writer.write("super.$N($N, $L);\n", contextMethod.getSimpleName(),
                        target.getName(), value.getCode());
                return null;
            }
            writer.write("$N.$N($L);\n", target.getName(), setter.getSimpleName(), value.getCode());
            return null;
        };
    }

    public CodeSnippet<Void> copyProperties(Variable target, List<? extends Variable> sources,
                                            MethodCollection contextMethods) {
        return writer -> {
            if (!typeOps.isDeclared(target.getType())) {
                return null;
            }
            MethodCollection targetMethods = methodUtils.allOf((DeclaredType) target.getType());
            for (Variable source : sources) {
                WriteMethod targetSetter = targetMethods.findWriter(source.getName(), source.getType());
                if (targetSetter != null) {
                    writer.write(setProperty(target, targetSetter, source, contextMethods));
                    continue;
                }
                if (typeOps.isDeclared(source.getType())) {
                    for (ReadMethod sourceGetter : methodUtils.allOf((DeclaredType) source.getType()).readers()) {
                        targetSetter = targetMethods.findWriter(sourceGetter.getReadName(), sourceGetter.getReadType());
                        if (targetSetter != null) {
                            writer.write(setProperty(target, targetSetter, sourceGetter.toExpression(source), contextMethods));
                        }
                    }
                }
            }
            return null;
        };
    }

    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    public CodeSnippet<Void> mapAndReturn(Entity entity, Variable source, TypeMirror returnType,
                                          MethodCollection contextMethods) {
        return writer -> {
            if (typeOps.isVoid(returnType)) {
                return null;
            }
            if (!typeOps.isPrimitive(returnType)) {
                writer.beginControlFlow("if ($N == null)", source.getName());
                writer.write("return null;\n");
                writer.endControlFlow();
            }
            Expression mapResult = writer.write(map(entity, source, returnType, contextMethods));
            writer.write("return $L;\n", mapResult.getCode());
            return null;
        };
    }

    public CodeSnippet<Expression> map(Entity entity, Variable source, TypeMirror targetType,
                                       MethodCollection contextMethods) {
        return writer -> {
            if (typeOps.isAssignable(source.getType(), PAGE_FQN)
                    && typeOps.isAssignable(typeOps.getDeclared(PAGE_FQN), typeOps.erasure(targetType))) {
                CodeWriter lambdaWriter = writer.newWriter();

                Variable itVar = Variable.of(
                        typeOps.resolveTypeParameter((DeclaredType) source.getType(), PAGE_FQN, 0),
                        lambdaWriter.newName("it")
                );
                TypeMirror elementType = typeOps.resolveTypeParameter((DeclaredType) targetType, PAGE_FQN, 0);
                lambdaWriter.write(mapAndReturn(entity, itVar, elementType, contextMethods));
                return Expression.of(targetType, "$L.map($N -> {\n$>$L$<})",
                        source.getName(), itVar.getName(), lambdaWriter.toCode());
            }
            if (typeOps.isAssignableToList(targetType) && typeOps.isAssignableToIterable(source.getType())) {
                CodeWriter lambdaWriter = writer.newWriter();

                CodeBlock stream;
                if (typeOps.isAssignableToCollection(source.getType())) {
                    stream = CodeBlock.of("$N.stream()", source.getName());
                } else {
                    stream = CodeBlock.of("$T.stream($N.spliterator(), false)", StreamSupport.class, source.getName());
                }

                Variable itVar = Variable.of(
                        typeOps.resolveIterableElementType((DeclaredType) source.getType()),
                        lambdaWriter.newName("it")
                );
                TypeMirror elementType = typeOps.resolveListElementType((DeclaredType) targetType);
                lambdaWriter.write(mapAndReturn(entity, itVar, elementType, contextMethods));
                return Expression.of(targetType,
                        "$L.map($N -> {\n$>$L$<}).collect($T.toList())",
                        stream, itVar.getName(), lambdaWriter.toCode(), Collectors.class
                );
            }
            if (typeOps.isSame(source.getType(), entity.getType())
                    && typeOps.isAssignable(entity.getIdAttributeType(), targetType)) {
                return Expression.of(targetType, "$N.$N()",
                        source.getName(), entity.getIdReadMethod().getSimpleName());
            }
            if (typeOps.isDeclared(targetType)) {
                Variable resultVar = writer.write(newVariable("temp", targetType, CodeBlock.of("new $T()", targetType)));
                writer.write(copyProperties(resultVar, Collections.singletonList(source), contextMethods));
                return resultVar;
            }
            throw new IllegalArgumentException("Can't convert to type " + targetType);
        };
    }
}
