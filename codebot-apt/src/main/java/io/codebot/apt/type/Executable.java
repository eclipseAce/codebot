package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Set;

public class Executable implements Annotated, Modified {
    private final ExecutableElement executableElement;
    private final Lazy<List<Annotation>> lazyAnnotations;
    private final Lazy<Type> lazyReturnType;
    private final Lazy<List<Variable>> lazyParameters;
    private final Lazy<List<Type>> lazyThrownTypes;

    Executable(Type enclosingType, ExecutableElement executableElement) {
        this.executableElement = executableElement;
        this.lazyAnnotations = Lazy.of(() -> ImmutableList.copyOf(
                executableElement.getAnnotationMirrors().stream().map(Annotation::new).iterator()
        ));
        Lazy<ExecutableType> lazyExecutableType = Lazy.of(() ->
                enclosingType.asMember(executableElement)
        );
        this.lazyReturnType = Lazy.of(() ->
                enclosingType.getFactory().getType(lazyExecutableType.get().getReturnType())
        );
        this.lazyParameters = Lazy.of(() ->
                Variable.parametersOf(enclosingType, executableElement)
        );
        this.lazyThrownTypes = Lazy.of(() -> ImmutableList.copyOf(
                lazyExecutableType.get().getThrownTypes().stream().map(it ->
                        enclosingType.getFactory().getType(it)
                ).iterator()
        ));
    }

    @Override
    public List<Annotation> getAnnotations() {
        return lazyAnnotations.get();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return executableElement.getModifiers();
    }

    public ExecutableElement getElement() {
        return executableElement;
    }

    public String getSimpleName() {
        return executableElement.getSimpleName().toString();
    }

    public Type getReturnType() {
        return lazyReturnType.get();
    }

    public List<Variable> getParameters() {
        return lazyParameters.get();
    }

    public List<Type> getThrownTypes() {
        return lazyThrownTypes.get();
    }

    public static List<Executable> methodsOf(Type type) {
        List<ExecutableElement> methods = Lists.newArrayList();
        collectMethodsInHierarchy(type.asDeclaredType(), type.getFactory().getElementUtils(), methods, Sets.newHashSet());
        return ImmutableList.copyOf(methods.stream().map(it -> new Executable(type, it)).iterator());
    }

    private static void collectMethodsInHierarchy(DeclaredType declaredType,
                                                  Elements elementUtils,
                                                  List<ExecutableElement> collected,
                                                  Set<TypeElement> visited) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName()) && visited.add(element)) {
            ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                    .filter(method -> {
                        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
                        boolean isPrivate = method.getModifiers().contains(Modifier.PRIVATE);
                        boolean isOverridden = collected.stream().anyMatch(it ->
                                elementUtils.overrides(it, method, (TypeElement) it.getEnclosingElement())
                        );
                        return !isStatic && !isPrivate && !isOverridden;
                    })
                    .forEach(collected::add);

            element.getInterfaces().forEach(it -> {
                collectMethodsInHierarchy((DeclaredType) it, elementUtils, collected, visited);
            });

            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectMethodsInHierarchy((DeclaredType) element.getSuperclass(), elementUtils, collected, visited);
            }
        }
    }
}
