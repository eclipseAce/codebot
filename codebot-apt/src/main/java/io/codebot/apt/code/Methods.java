package io.codebot.apt.code;

import com.google.common.collect.Lists;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import java.util.List;

public final class Methods {
    private Methods() {
    }

    public static Method of(Type containingType, ExecutableElement element) {
        TypeFactory typeFactory = containingType.getFactory();
        ExecutableType type = containingType.asMember(element);
        List<Parameter> parameters = Lists.newArrayList();
        for (int i = 0; i < element.getParameters().size(); i++) {
            parameters.add(new ParameterImpl(
                    element.getParameters().get(i),
                    typeFactory.getType(type.getParameterTypes().get(i))
            ));
        }
        return new MethodImpl(
                element,
                containingType,
                typeFactory.getType(type.getReturnType()),
                parameters
        );
    }

    private static class MethodImpl implements Method {
        private final ExecutableElement element;
        private final Type containingType;
        private final Type returnType;
        private final List<Parameter> parameters;

        MethodImpl(ExecutableElement element,
                   Type containingType,
                   Type returnType,
                   List<Parameter> parameters) {
            this.element = element;
            this.containingType = containingType;
            this.returnType = returnType;
            this.parameters = parameters;
        }

        @Override
        public ExecutableElement getElement() {
            return element;
        }

        @Override
        public String getSimpleName() {
            return element.getSimpleName().toString();
        }

        @Override
        public Type getContainingType() {
            return containingType;
        }

        @Override
        public Type getReturnType() {
            return returnType;
        }

        @Override
        public List<Parameter> getParameters() {
            return parameters;
        }
    }

    private static class ParameterImpl implements Parameter {
        private final VariableElement element;
        private final Type type;

        ParameterImpl(VariableElement element, Type type) {
            this.element = element;
            this.type = type;
        }

        @Override
        public VariableElement getElement() {
            return element;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String getName() {
            return element.getSimpleName().toString();
        }
    }
}
