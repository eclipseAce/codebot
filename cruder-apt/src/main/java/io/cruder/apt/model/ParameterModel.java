package io.cruder.apt.model;

import com.google.common.collect.Lists;
import io.cruder.apt.util.TypeResolver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ParameterModel {
    private final ModelContext ctx;
    private final VariableElement element;
    private final TypeMirror type;

    public static List<ParameterModel> parametersOf(ModelContext ctx,
                                                    TypeResolver typeResolver,
                                                    ExecutableElement method) {
        List<ParameterModel> parameters = Lists.newArrayList();
        for (VariableElement variable : method.getParameters()) {
            parameters.add(new ParameterModel(
                    ctx,
                    variable,
                    typeResolver.resolve(variable.asType())
            ));
        }
        return parameters;
    }
}