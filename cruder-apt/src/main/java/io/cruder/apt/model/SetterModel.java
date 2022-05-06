package io.cruder.apt.model;

import com.google.common.collect.Lists;
import io.cruder.apt.util.TypeResolver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SetterModel {
    private static final String SETTER_PREFIX = "set";

    private final ModelContext ctx;
    private final ExecutableElement element;
    private final String name;
    private final TypeMirror type;

    public boolean isAssignableFrom(TypeMirror type) {
        return ctx.typeUtils.isAssignable(type, getType());
    }

    public static List<SetterModel> settersOf(ModelContext ctx, TypeResolver typeResolver, TypeElement type) {
        List<SetterModel> setters = Lists.newArrayList();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }

            String name = method.getSimpleName().toString();
            if (name.length() > SETTER_PREFIX.length()
                    && name.startsWith(SETTER_PREFIX)
                    && method.getParameters().size() == 1) {
                setters.add(new SetterModel(
                        ctx,
                        method,
                        StringUtils.uncapitalize(name.substring(3)),
                        typeResolver.resolve(method.getParameters().get(0).asType())
                ));
            }
        }
        return setters;
    }
}