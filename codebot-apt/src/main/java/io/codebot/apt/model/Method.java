package io.codebot.apt.model;

import com.google.common.collect.ImmutableList;
import io.codebot.apt.type.Type;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Method {
    private final ExecutableElement executableElement;
    private final Type containingType;
    private final Type returnType;
    private final List<Parameter> parameters;

    public Method(Type containingType, ExecutableElement executableElement) {
        ExecutableType executableType = containingType.asMember(executableElement);

        this.containingType = containingType;
        this.executableElement = executableElement;
        this.returnType = containingType.factory().getType(executableType.getReturnType());
        this.parameters = IntStream.range(0, executableElement.getParameters().size()).boxed()
                .map(i -> new Parameter(
                        executableElement.getParameters().get(i).getSimpleName().toString(),
                        containingType.factory().getType(executableType.getParameterTypes().get(i))
                ))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    public ExecutableElement getExecutableElement() {
        return executableElement;
    }

    public Type getContainingType() {
        return containingType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public String getSimpleName() {
        return executableElement.getSimpleName().toString();
    }
}
