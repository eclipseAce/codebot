package io.cruder.autoservice;

import lombok.Getter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

public class ParameterDescriptor {
    private final ProcessingContext ctx;
    private final @Getter VariableElement parameterElement;
    private final @Getter BeanDescriptor bean;

    public ParameterDescriptor(ProcessingContext ctx, VariableElement parameter) {
        this.ctx = ctx;
        this.parameterElement = parameter;

        if (isDeclaredType()) {
            TypeElement te = ctx.utils.asTypeElement(parameterElement.asType());
            if (te == null) {
                System.out.println(parameterElement.asType());
            }
            this.bean = new BeanDescriptor(ctx, te);
        } else {
            this.bean = null;
        }
    }

    public String getName() {
        return parameterElement.getSimpleName().toString();
    }

    public boolean isDeclaredType() {
        return parameterElement.asType().getKind() == TypeKind.DECLARED;
    }

    public boolean isPageable() {
        return ctx.utils.isAssignable(parameterElement.asType(),
                "org.springframework.data.domain.Pageable");
    }

    public boolean isJpaSpecification(EntityDescriptor entity) {
        return ctx.utils.isAssignable(parameterElement.asType(),
                "org.springframework.data.jpa.domain.Specification",
                entity.getBeanElement().asType());
    }

    public boolean couldBeIdentifierOf(EntityDescriptor entity) {
        return entity.getIdProperty().getName().equals(getName())
                && ctx.utils.types.isSameType(parameterElement.asType(), entity.getIdProperty().getType());
    }
}
