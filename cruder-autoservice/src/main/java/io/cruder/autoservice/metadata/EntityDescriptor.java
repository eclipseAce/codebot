package io.cruder.autoservice.metadata;

import io.cruder.autoservice.util.Models;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityDescriptor {
    private TypeElement entityElement;
    private VariableElement idFieldElement;

    public String getIdFieldName() {
        return idFieldElement.getSimpleName().toString();
    }

    public TypeMirror getIdFieldType() {
        return idFieldElement.asType();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static EntityDescriptor of(Models models, TypeElement entity) {
        EntityDescriptor info = new EntityDescriptor();
        info.entityElement = entity;
        TypeElement e = entity;
        while (!models.isTypeOfName(e.asType(), "java.lang.Object")) {
            for (VariableElement field : ElementFilter.fieldsIn(e.getEnclosedElements())) {
                if (models.isAnnotationPresent(field, "javax.persistence.Id")) {
                    info.idFieldElement = field;
                }
            }
            e = models.asTypeElement(e.getSuperclass());
        }
        return info;
    }
}
