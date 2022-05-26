package io.codebot.apt.processor.crud;

import com.google.common.collect.Lists;
import io.codebot.apt.code.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Mapping {
    private final @Getter Expression source;
    private final @Getter WriteMethod targetWriteMethod;

    public void writeTo(CodeWriter writer, Expression target) {
        writer.write("$L.$N($L);\n",
                target.getCode(),
                getTargetWriteMethod().getSimpleName(),
                getSource().getCode()
        );
    }

    public static List<Mapping> map(ProcessingEnvironment processingEnv, TypeMirror targetType,
                                    Expression source, Collection<? extends ReadMethod> readers) {
        return map(processingEnv, targetType, readers.stream()
                .collect(Collectors.toMap(ReadMethod::getReadName, it -> it.toExpression(source))));
    }

    public static List<Mapping> map(ProcessingEnvironment processingEnv, TypeMirror targetType,
                                    List<? extends Variable> variables) {
        return map(processingEnv, targetType, variables.stream()
                .collect(Collectors.toMap(Variable::getName, Variable::asExpression)));
    }

    public static List<Mapping> map(ProcessingEnvironment processingEnv, TypeMirror targetType,
                                    Map<String, Expression> sources) {
        TypeOps typeOps = TypeOps.instanceOf(processingEnv);
        if (!typeOps.isDeclared(targetType)) {
            return Collections.emptyList();
        }

        Methods methodUtils = Methods.instanceOf(processingEnv);
        MethodCollection methods = methodUtils.allOf((DeclaredType) targetType);

        List<Mapping> mappings = Lists.newArrayList();
        for (Map.Entry<String, Expression> entry : sources.entrySet()) {
            String name = entry.getKey();
            Expression expr = entry.getValue();

            WriteMethod toWriter = methods.findWriter(name, expr.getType());
            if (toWriter != null) {
                mappings.add(new Mapping(expr, toWriter));
                continue;
            }
            if (typeOps.isDeclared(expr.getType())) {
                for (ReadMethod fromReader : methodUtils.allOf((DeclaredType) expr.getType()).readers()) {
                    toWriter = methods.findWriter(fromReader.getReadName(), fromReader.getReadType());
                    if (toWriter != null) {
                        mappings.add(new Mapping(fromReader.toExpression(expr), toWriter));
                    }
                }
            }
        }
        return mappings;
    }
}
