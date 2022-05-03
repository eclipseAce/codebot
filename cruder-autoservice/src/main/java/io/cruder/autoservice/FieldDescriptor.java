package io.cruder.autoservice;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldDescriptor {
    private VariableElement fieldElement;
    private ExecutableElement getterElement;
    private ExecutableElement setterElement;

    public String getName() {
        return fieldElement.getSimpleName().toString();
    }

    public TypeMirror getType() {
        return fieldElement.asType();
    }

    public String getGetterName() {
        return getterElement.getSimpleName().toString();
    }

    public String getSetterName() {
        return setterElement.getSimpleName().toString();
    }

    public boolean isReadable() {
        return getterElement != null;
    }

    public boolean isWritable() {
        return setterElement != null;
    }

    public Optional<? extends AnnotationMirror> findAnnotation(ProcessingContext ctx, String annotaionFqn) {
        return ctx.utils.findAnnotation(fieldElement, annotaionFqn);
    }

    public boolean isAnnotationPresent(ProcessingContext ctx, String annotaionFqn) {
        return findAnnotation(ctx, annotaionFqn).isPresent();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static FieldDescriptor of(ProcessingContext ctx, VariableElement field) {
        FieldDescriptor descriptor = new FieldDescriptor();
        descriptor.fieldElement = field;

        String capFieldName = StringUtils.capitalize(field.getSimpleName().toString());
        ElementFilter.methodsIn(field.getEnclosingElement().getEnclosedElements()).stream()
                .filter(method -> !method.getModifiers().contains(Modifier.STATIC))
                .forEach(method -> {
                    String methodName = method.getSimpleName().toString();
                    if (descriptor.getterElement == null
                            && method.getParameters().isEmpty()) {
                        if (field.asType().getKind() == TypeKind.BOOLEAN
                                && methodName.equals("is" + capFieldName)
                                && method.getReturnType().getKind() == TypeKind.BOOLEAN) {
                            descriptor.getterElement = method;
                        } //
                        else if (methodName.equals("get" + capFieldName)
                                && ctx.utils.types.isSameType(method.getReturnType(), field.asType())) {
                            descriptor.getterElement = method;
                        }
                    } //
                    else if (descriptor.setterElement == null
                            && methodName.equals("set" + capFieldName)
                            && method.getParameters().size() == 1
                            && ctx.utils.types.isSameType(method.getParameters().get(0).asType(), field.asType())) {
                        descriptor.setterElement = method;
                    }
                });

        return descriptor;
    }
}
