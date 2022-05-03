package io.cruder.autoservice;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class PropertyDescriptor {
    private final ProcessingContext ctx;
    private final @Getter VariableElement fieldElement;
    private final @Getter ExecutableElement getterElement;
    private final @Getter ExecutableElement setterElement;

    public PropertyDescriptor(ProcessingContext ctx, VariableElement field) {
        this.ctx = ctx;
        this.fieldElement = field;

        String capFieldName = StringUtils.capitalize(field.getSimpleName().toString());
        List<ExecutableElement> methods = ElementFilter
                .methodsIn(field.getEnclosingElement().getEnclosedElements()).stream()
                .filter(method -> !method.getModifiers().contains(Modifier.STATIC))
                .collect(Collectors.toList());
        ExecutableElement getterElement = null;
        ExecutableElement setterElement = null;
        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();
            if (getterElement == null
                    && method.getParameters().isEmpty()) {
                if (field.asType().getKind() == TypeKind.BOOLEAN
                        && methodName.equals("is" + capFieldName)
                        && method.getReturnType().getKind() == TypeKind.BOOLEAN) {
                    getterElement = method;
                } //
                else if (methodName.equals("get" + capFieldName)
                        && ctx.utils.types.isSameType(method.getReturnType(), field.asType())) {
                    getterElement = method;
                }
            } //
            else if (setterElement == null
                    && methodName.equals("set" + capFieldName)
                    && method.getParameters().size() == 1
                    && ctx.utils.types.isSameType(method.getParameters().get(0).asType(), field.asType())) {
                setterElement = method;
            }
        }
        this.getterElement = getterElement;
        this.setterElement = setterElement;
    }

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

    public Optional<? extends AnnotationMirror> findAnnotation(String annotaionFqn) {
        return ctx.utils.findAnnotation(fieldElement, annotaionFqn);
    }

    public boolean isAnnotationPresent(String annotaionFqn) {
        return findAnnotation(annotaionFqn).isPresent();
    }

    public boolean couldBeIdentifierOf(EntityDescriptor entity) {
        return entity.getIdProperty().getName().equals(getName())
                && ctx.utils.isSameType(getType(), entity.getIdProperty().getType(), true);
    }
}
