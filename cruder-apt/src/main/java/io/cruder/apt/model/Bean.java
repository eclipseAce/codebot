package io.cruder.apt.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;

public class Bean {
    private final TypeElement beanElement;
    private final Map<String, Accessor> getters;
    private final Map<String, Accessor> setters;

    private Bean(TypeElement beanElement,
                 Map<String, Accessor> getters,
                 Map<String, Accessor> setters) {
        this.beanElement = beanElement;
        this.getters = getters;
        this.setters = setters;
    }

    public TypeElement getBeanElement() {
        return beanElement;
    }

    public Map<String, Accessor> getGetters() {
        return getters;
    }

    public Map<String, Accessor> getSetters() {
        return setters;
    }

    public static class Accessor {
        private final ExecutableElement methodElement;
        private final String propertyName;
        private final TypeMirror propertyType;

        private Accessor(ExecutableElement methodElement,
                         String propertyName,
                         TypeMirror propertyType) {
            this.methodElement = methodElement;
            this.propertyName = propertyName;
            this.propertyType = propertyType;
        }

        public ExecutableElement getMethodElement() {
            return methodElement;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public TypeMirror getPropertyType() {
            return propertyType;
        }
    }

    public static class Factory {
        private final Elements elementUtils;
        private final Types typeUtils;

        public Factory(ProcessingEnvironment processingEnv) {
            this.elementUtils = processingEnv.getElementUtils();
            this.typeUtils = processingEnv.getTypeUtils();
        }

        public Bean getBean(DeclaredType declaredType) {
            Map<String, Accessor> getters = Maps.newLinkedHashMap();
            Map<String, Accessor> setters = Maps.newLinkedHashMap();

            DeclaredType type = declaredType;
            TypeElement element = (TypeElement) type.asElement();
            while (element.getKind() == ElementKind.CLASS
                    && !element.getQualifiedName().contentEquals("java.lang.Object")) {
                TypeResolver typeResolver = new TypeResolver(type);
                for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
                    String name = method.getSimpleName().toString();
                    TypeMirror returnType = typeResolver.resolve(method.getReturnType());

                    if (name.length() > 3
                            && name.startsWith("get")
                            && returnType.getKind() != TypeKind.VOID
                            && method.getParameters().isEmpty()) {
                        String propName = StringUtils.uncapitalize(name.substring(3));
                        getters.put(propName, new Accessor(method, propName, returnType));
                    } //
                    else if (name.length() > 2
                            && name.startsWith("is")
                            && returnType.getKind() == TypeKind.BOOLEAN
                            && method.getParameters().isEmpty()) {
                        String propName = StringUtils.uncapitalize(name.substring(2));
                        getters.put(propName, new Accessor(method, propName, returnType));
                    } //
                    else if (name.length() > 3
                            && name.startsWith("set")
                            && method.getParameters().size() == 1) {
                        String propName = StringUtils.uncapitalize(name.substring(3));
                        TypeMirror paramType = typeResolver.resolve(method.getParameters().get(0).asType());
                        setters.put(propName, new Accessor(method, propName, paramType));
                    }
                }

                type = (DeclaredType) element.getSuperclass();
                element = (TypeElement) type.asElement();
            }
            return new Bean(
                    (TypeElement) declaredType.asElement(),
                    ImmutableMap.copyOf(getters),
                    ImmutableMap.copyOf(setters)
            );
        }
    }

    private static class TypeResolver {
        private final Map<TypeVariable, TypeMirror> typeVars = Maps.newHashMap();

        public TypeResolver(DeclaredType type) {
            TypeElement element = (TypeElement) type.asElement();
            for (int i = 0; i < element.getTypeParameters().size(); i++) {
                typeVars.put(
                        (TypeVariable) element.getTypeParameters().get(i).asType(),
                        type.getTypeArguments().get(i)
                );
            }
        }

        public TypeMirror resolve(TypeMirror type) {
            if (type.getKind() == TypeKind.TYPEVAR) {
                return typeVars.getOrDefault((TypeVariable) type, type);
            }
            return type;
        }
    }
}
