package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.cruder.apt.util.TypeResolver;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class ModelFactory {
    private final Elements elementUtils;
    private final Types typeUtils;

    public ModelFactory(ProcessingEnvironment processingEnv) {
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    public Bean getBean(DeclaredType beanType) {
        List<Accessor> accessors = Lists.newArrayList();

        DeclaredType type = beanType;
        TypeElement element = (TypeElement) type.asElement();
        TypeResolver typeResolver = new TypeResolver(type, null);
        while (element.getSuperclass().getKind() != TypeKind.NONE) {

            for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
                String name = method.getSimpleName().toString();
                TypeMirror returnType = typeResolver.resolve(method.getReturnType());

                if (name.length() > 3
                        && name.startsWith("get")
                        && returnType.getKind() != TypeKind.VOID
                        && method.getParameters().isEmpty()) {
                    accessors.add(new Accessor(
                            method,
                            AccessorKind.GETTER,
                            StringUtils.uncapitalize(name.substring(3)),
                            returnType
                    ));
                } //
                else if (name.length() > 2
                        && name.startsWith("is")
                        && returnType.getKind() == TypeKind.BOOLEAN
                        && method.getParameters().isEmpty()) {
                    accessors.add(new Accessor(
                            method,
                            AccessorKind.GETTER,
                            StringUtils.uncapitalize(name.substring(2)),
                            returnType
                    ));
                } //
                else if (name.length() > 3
                        && name.startsWith("set")
                        && method.getParameters().size() == 1) {
                    accessors.add(new Accessor(
                            method,
                            AccessorKind.SETTER,
                            StringUtils.uncapitalize(name.substring(3)),
                            typeResolver.resolve(method.getParameters().get(0).asType())
                    ));
                }
            }

            type = (DeclaredType) element.getSuperclass();
            element = (TypeElement) type.asElement();
            typeResolver = new TypeResolver(type, typeResolver);
        }
        return new Bean(
                (TypeElement) beanType.asElement(),
                ImmutableList.copyOf(accessors)
        );
    }

    public Service getService(DeclaredType serviceType) {
        TypeResolver typeResolver = new TypeResolver(serviceType, null);
        for (ExecutableElement method : ElementFilter.methodsIn(serviceType.asElement().getEnclosedElements())) {

        }
        return null;
    }
}
