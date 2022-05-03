package io.cruder.autoservice;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityDescriptor {
    private TypeElement entityElement;
    private FieldDescriptor idField;
    private Map<String, FieldDescriptor> fields;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static EntityDescriptor of(ProcessingContext ctx, TypeElement entity) {
        EntityDescriptor info = new EntityDescriptor();
        info.entityElement = entity;

        TypeElement e = entity;
        while (!ctx.utils.isTypeOfName(e.asType(), "java.lang.Object")) {
            info.fields = ElementFilter.fieldsIn(e.getEnclosedElements()).stream()
                    .filter(field -> !field.getModifiers().contains(Modifier.STATIC))
                    .map(field -> {
                        FieldDescriptor fd = FieldDescriptor.of(ctx, field);
                        if (fd.isAnnotationPresent(ctx, "javax.persistence.Id")) {
                            info.idField = fd;
                        }
                        return fd;
                    })
                    .collect(Collectors.toMap(fd -> fd.getName(), Function.identity()));
            e = ctx.utils.asTypeElement(e.getSuperclass());
        }
        return info;
    }
}
