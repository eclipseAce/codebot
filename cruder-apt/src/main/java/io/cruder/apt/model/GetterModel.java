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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GetterModel {
    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";

    private final ModelContext ctx;
    private final ExecutableElement element;
    private final String name;
    private final TypeMirror type;

    public boolean isAssignableTo(TypeMirror type) {
        return ctx.typeUtils.isAssignable(getType(), type);
    }

    public static List<GetterModel> gettersOf(ModelContext ctx, TypeResolver typeResolver, TypeElement type) {
        List<GetterModel> getters = Lists.newArrayList();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }

            String name = method.getSimpleName().toString();
            TypeMirror returnType = typeResolver.resolve(method.getReturnType());
            if (name.length() > GETTER_PREFIX.length()
                    && name.startsWith(GETTER_PREFIX)
                    && returnType.getKind() != TypeKind.VOID
                    && method.getParameters().isEmpty()) {
                getters.add(new GetterModel(
                        ctx,
                        method,
                        StringUtils.uncapitalize(name.substring(GETTER_PREFIX.length())),
                        returnType));
            } //
            else if (name.length() > BOOLEAN_GETTER_PREFIX.length()
                    && name.startsWith(BOOLEAN_GETTER_PREFIX)
                    && returnType.getKind() == TypeKind.BOOLEAN
                    && method.getParameters().isEmpty()) {
                getters.add(new GetterModel(
                        ctx,
                        method,
                        StringUtils.uncapitalize(name.substring(GETTER_PREFIX.length())),
                        returnType
                ));
            }
        }
        return getters;
    }
}