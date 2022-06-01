package io.codebot.apt;

import io.codebot.apt.coding.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import java.util.List;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanCodes {
    private final TypeOps typeOps;
    private final Methods methodUtils;
    private final MethodCollection contextMethods;

    public static BeanCodes instanceOf(ProcessingEnvironment processingEnv, MethodCollection contextMethods) {
        return new BeanCodes(TypeOps.instanceOf(processingEnv), Methods.instanceOf(processingEnv), contextMethods);
    }

    public void setProperties(CodeWriter writer, Variable target, List<? extends Variable> sources) {
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
            writer.write("$N($N, $L);\n", contextMethod.getSimpleName(),
                    target.getName(), propertyValue.getCode());
            return;
        }
        writer.write("$N.$N($L);\n", target.getName(), propertySetter.getSimpleName(), propertyValue.getCode());
    }
}
