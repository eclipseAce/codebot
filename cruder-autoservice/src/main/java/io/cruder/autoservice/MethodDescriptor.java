package io.cruder.autoservice;

import lombok.Getter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MethodDescriptor {
    private static final String LIST_FQN = "java.util.List";
    private static final String SPRING_DATA_PAGE_FQN = "org.springframework.data.domain.Page";

    private final @Getter ExecutableElement methodElement;
    private final @Getter MethodKind methodKind;
    private final @Getter ResultKind resultKind;
    private final @Getter TypeMirror resultType;
    private final @Getter TypeElement resultElement;
    private final @Getter TypeElement resultContainerElement;
    private final @Getter Map<String, ParameterDescriptor> parameters;

    public enum ResultKind {
        NONE, IDENTIFIER, DATA_OBJECT, UNKNOWN
    }

    public enum MethodKind {
        CREATE, UPDATE, QUERY, UNKNOWN
    }

    public MethodDescriptor(ProcessingContext ctx, ServiceDescriptor service, ExecutableElement method) {
        this.methodElement = method;

        String methodName = method.getSimpleName().toString();
        if (methodName.startsWith("create")) {
            this.methodKind = MethodKind.CREATE;
        } else if (methodName.startsWith("update")) {
            this.methodKind = MethodKind.UPDATE;
        } else if (methodName.startsWith("find")) {
            this.methodKind = MethodKind.QUERY;
        } else {
            this.methodKind = MethodKind.UNKNOWN;
        }

        TypeMirror returnType = method.getReturnType();
        if (ctx.utils.isAssignable(returnType, LIST_FQN, ctx.utils.types.getWildcardType(null, null))) {
            this.resultContainerElement = ctx.utils.elements.getTypeElement(LIST_FQN);
            this.resultType = ((DeclaredType) returnType).getTypeArguments().get(0);
        } else if (ctx.utils.isAssignable(returnType, SPRING_DATA_PAGE_FQN, ctx.utils.types.getWildcardType(null, null))) {
            this.resultContainerElement = ctx.utils.elements.getTypeElement(SPRING_DATA_PAGE_FQN);
            this.resultType = ((DeclaredType) returnType).getTypeArguments().get(0);
        } else {
            this.resultContainerElement = null;
            this.resultType = returnType;
        }
        if (this.resultType.getKind() == TypeKind.VOID) {
            this.resultKind = ResultKind.NONE;
            this.resultElement = null;
        } else if (ctx.utils.types.isAssignable(this.resultType, service.getEntity().getIdProperty().getType())) {
            this.resultKind = ResultKind.IDENTIFIER;
            this.resultElement = null;
        } else if (this.resultType.getKind() == TypeKind.DECLARED) {
            this.resultKind = ResultKind.DATA_OBJECT;
            this.resultElement = ctx.utils.asTypeElement(this.resultType);
        } else {
            this.resultKind = ResultKind.UNKNOWN;
            this.resultElement = null;
        }

        this.parameters = method.getParameters().stream()
                .map(it -> new ParameterDescriptor(ctx, it))
                .collect(Collectors.toMap(ParameterDescriptor::getName, Function.identity()));
    }

    public String getName() {
        return methodElement.getSimpleName().toString();
    }

    public List<ParameterDescriptor> findParameters(Predicate<ParameterDescriptor> condition) {
        return parameters.values().stream().filter(condition).collect(Collectors.toList());
    }

    public Optional<ParameterDescriptor> findUniqueParameter(Predicate<ParameterDescriptor> condition) {
        List<ParameterDescriptor> all = findParameters(condition);
        return all.size() == 1 ? Optional.of(all.get(0)) : Optional.empty();
    }

    public Optional<ParameterDescriptor> findFirstParameter(Predicate<ParameterDescriptor> condition) {
        return findParameters(condition).stream().findFirst();
    }
}
