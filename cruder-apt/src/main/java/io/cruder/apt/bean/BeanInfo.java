package io.cruder.apt.bean;

import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Collections;
import java.util.Map;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanInfo {
    private final TypeElement typeElement;

    private final Map<String, Property> readableProperties;

    private final Map<String, Property> writableProperties;

    public static BeanInfo introspect(TypeElement typeElement) {
        Map<String, Property> readables = Maps.newHashMap();
        Map<String, Property> writables = Maps.newHashMap();
        for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            String methodName = method.getSimpleName().toString();
            if (methodName.length() > 3
                    && methodName.startsWith("get")
                    && method.getParameters().isEmpty()) {
                readables.computeIfAbsent(
                        StringUtils.uncapitalize(methodName.substring(3)),
                        name -> new Property(name, method.getReturnType()));
            } //
            else if (methodName.length() > 2
                    && methodName.startsWith("is")
                    && method.getReturnType().getKind() == TypeKind.BOOLEAN
                    && method.getParameters().isEmpty()) {
                readables.computeIfAbsent(
                        StringUtils.uncapitalize(methodName.substring(2)),
                        name -> new Property(name, method.getReturnType()));
            } //
            else if (methodName.length() > 3
                    && methodName.startsWith("set")
                    && method.getParameters().size() == 1) {
                writables.computeIfAbsent(
                        StringUtils.uncapitalize(methodName.substring(3)),
                        name -> new Property(name, method.getParameters().get(0).asType()));
            }
        }
        return new BeanInfo(typeElement,
                Collections.unmodifiableMap(readables),
                Collections.unmodifiableMap(writables));
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Property {
        private final String name;

        private final TypeMirror type;
    }

}
