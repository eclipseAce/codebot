package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.cruder.apt.util.TypeResolver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MethodModel {
    private final ExecutableElement element;
    private final List<ParameterModel> parameters;
    private final TypeMirror returnType;
    private final List<? extends TypeMirror> thrownTypes;

    public static List<MethodModel> methodsOf(ModelContext ctx, TypeResolver typeResolver, TypeElement type) {
        List<MethodModel> methods = Lists.newArrayList();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            methods.add(new MethodModel(
                    method,
                    ImmutableList.copyOf(ParameterModel.parametersOf(ctx, typeResolver, method)),
                    typeResolver.resolve(method.getReturnType()),
                    ImmutableList.copyOf(typeResolver.resolveAll(method.getThrownTypes()))
            ));
        }
        return methods;
    }
}