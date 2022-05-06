package io.cruder.apt.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JpaEntityModel {
    private final BeanModel bean;
    private final FieldModel idField;

    public static JpaEntityModel entityOf(ModelContext ctx, DeclaredType entityType) {
        BeanModel bean = BeanModel.beanOf(ctx, entityType);
        FieldModel idField = bean
                .findField(field -> ctx.isAnnotationPresent(field.getElement(), "javax.persistence.Id"))
                .orElseThrow(() -> new IllegalStateException("No @Id field found"));
        return new JpaEntityModel(bean, idField);
    }
}
