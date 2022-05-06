package io.cruder.apt.model;

import com.google.common.collect.Lists;
import io.cruder.apt.util.TypeResolver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldModel {
    private final ModelContext ctx;
    private final VariableElement element;
    private final TypeMirror type;

    public static List<FieldModel> fieldsOf(ModelContext ctx, TypeResolver typeResolver, TypeElement type) {
        List<FieldModel> fields = Lists.newArrayList();
        for (VariableElement variable : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (variable.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            fields.add(new FieldModel(ctx, variable, typeResolver.resolve(variable.asType())));
        }
        return fields;
    }
}