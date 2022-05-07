package io.cruder.apt.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;

public class TypeFactory {
    private final Elements elementUtils;
    private final Types typeUtils;

    public TypeFactory(ProcessingEnvironment processingEnv) {
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    public Elements getElementUtils() {
        return elementUtils;
    }

    public Types getTypeUtils() {
        return typeUtils;
    }

    public Type getType(String qualifiedName) {
        return getType(elementUtils.getTypeElement(qualifiedName).asType());
    }

    public Type getType(TypeMirror typeMirror) {
        return typeMirror.accept(new SimpleTypeVisitor6<Type, Void>() {
            @Override
            public Type visitPrimitive(PrimitiveType t, Void unused) {
                return new Type(TypeFactory.this, typeMirror, null, null, null, null);
            }

            @Override
            public Type visitDeclared(DeclaredType t, Void unused) {
                List<VariableElement> fields = Lists.newArrayList();
                collectFieldsInHierachy(fields, t);

                List<ExecutableElement> methods = Lists.newArrayList();
                collectMethodsInHierachy(methods, Sets.newHashSet(), t);

                return new Type(TypeFactory.this, typeMirror, (TypeElement) typeUtils.asElement(typeMirror), fields, methods, null);
            }
        }, null);
    }

    private void collectFieldsInHierachy(List<VariableElement> collected,
                                         DeclaredType declaredType) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            collected.addAll(ElementFilter.fieldsIn(element.getEnclosedElements()));
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectFieldsInHierachy(collected, (DeclaredType) element.getSuperclass());
            }
        }
    }

    private void collectMethodsInHierachy(List<ExecutableElement> collected,
                                          Set<TypeElement> visited,
                                          DeclaredType declaredType) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            visited.add(element);
            for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
                if (isPrivateOrStatic(method) || isAlreadyOverridded(collected, method)) {
                    continue;
                }
                collected.add(method);
            }
            for (TypeMirror iface : element.getInterfaces()) {
                collectMethodsInHierachy(collected, visited, (DeclaredType) iface);
            }
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectMethodsInHierachy(collected, visited, (DeclaredType) element.getSuperclass());
            }
        }
    }

    private boolean isAlreadyOverridded(List<ExecutableElement> collected, ExecutableElement method) {
        return collected.stream().anyMatch(it -> elementUtils
                .overrides(it, method, (TypeElement) it.getEnclosingElement()));
    }

    private boolean isPrivateOrStatic(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.STATIC)
                || method.getModifiers().contains(Modifier.PRIVATE);
    }
}
