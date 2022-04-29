package io.cruder.autoservice.info;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.cruder.autoservice.util.Models;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodDescriptor {
    private ExecutableElement methodElement;
    private MethodKind methodKind;
    private ResultKind resultKind;
    private TypeMirror resultType;
    private TypeElement resultElement;
    private TypeElement resultContainerElement;
    private Map<String, VariableElement> pageableParameterElements;
    private Map<String, VariableElement> specificationParameterElements;
    private Map<String, VariableElement> entityIdParameterElements;
    private Map<String, VariableElement> unrecognizedParameterElements;

    public enum ResultKind {
        NONE, IDENTIFIER, DATA_OBJECT
    }

    public enum MethodKind {
        CREATE, UPDATE, QUERY, UNKNOWN;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    private static final String LIST_FQN = "java.util.List";
    private static final String SPRING_DATA_SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    private static final String SPRING_DATA_PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String SPRING_DATA_PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    public static MethodDescriptor of(Models models, ServiceDescriptor service, ExecutableElement method) {
        MethodDescriptor info = new MethodDescriptor();
        info.methodElement = method;

        String methodName = method.getSimpleName().toString();
        if (methodName.startsWith("create")) {
            info.methodKind = MethodKind.CREATE;
        } else if (methodName.startsWith("update")) {
            info.methodKind = MethodKind.UPDATE;
        } else if (methodName.startsWith("query")) {
            info.methodKind = MethodKind.QUERY;
        } else {
            info.methodKind = MethodKind.UNKNOWN;
        }

        TypeMirror returnType = method.getReturnType();
        if (models.isAssignable(returnType, LIST_FQN, models.types.getWildcardType(null, null))) {
            info.resultContainerElement = models.elements.getTypeElement(LIST_FQN);
            info.resultType = ((DeclaredType) returnType).getTypeArguments().get(0);
        } else if (models.isAssignable(returnType, SPRING_DATA_PAGE_FQN, models.types.getWildcardType(null, null))) {
            info.resultContainerElement = models.elements.getTypeElement(SPRING_DATA_PAGE_FQN);
            info.resultType = ((DeclaredType) returnType).getTypeArguments().get(0);
        } else {
            info.resultType = returnType;
        }
        if (info.resultType.getKind() == TypeKind.VOID) {
            info.resultKind = ResultKind.NONE;
        } else if (models.types.isAssignable(info.resultType, service.getEntity().getIdFieldType())) {
            info.resultKind = ResultKind.IDENTIFIER;
        } else if (info.resultType.getKind() == TypeKind.DECLARED) {
            info.resultKind = ResultKind.DATA_OBJECT;
            info.resultElement = models.asTypeElement(info.resultType);
        }

        List<VariableElement> parameters = Lists.newLinkedList(method.getParameters());
        info.pageableParameterElements = recognize(parameters,
                v -> models.isAssignable(v.asType(), SPRING_DATA_PAGEABLE_FQN));

        info.specificationParameterElements = recognize(parameters,
                v -> models.isAssignable(v.asType(), SPRING_DATA_SPECIFICATION_FQN,
                        service.getEntity().getEntityElement().asType()));

        info.entityIdParameterElements = recognize(parameters,
                v -> service.getEntity().getIdFieldName().contentEquals(v.getSimpleName()));

        info.unrecognizedParameterElements = recognize(parameters, v -> true);

        return info;
    }

    private static Map<String, VariableElement> recognize(List<VariableElement> parameters,
                                                          Function<VariableElement, Boolean> filter) {
        Map<String, VariableElement> recognized = Maps.newLinkedHashMap();
        parameters.removeIf(variable -> {
            if (filter.apply(variable)) {
                recognized.put(variable.getSimpleName().toString(), variable);
                return true;
            }
            return false;
        });
        return ImmutableMap.copyOf(recognized);
    }
}