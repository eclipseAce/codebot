package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.codebot.apt.util.Lazy;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Type implements TypeSupport {
    private final TypeFactory factory;
    private final Elements elementUtils;
    private final Types typeUtils;

    private final TypeMirror typeMirror;

    private final Lazy<List<Type>> lazyTypeArguments;
    private final Lazy<List<Variable>> lazyFields;
    private final Lazy<List<Executable>> lazyMethods;
    private final Lazy<List<GetAccessor>> lazyGetters;
    private final Lazy<List<SetAccessor>> lazySetters;

    protected Type(TypeFactory factory, TypeMirror typeMirror) {
        this.factory = factory;
        this.elementUtils = factory.elementUtils();
        this.typeUtils = factory.typeUtils();
        this.typeMirror = typeMirror;
        this.lazyTypeArguments = lazyTypeArguments();
        this.lazyFields = lazyFields();
        this.lazyMethods = lazyMethods();
        this.lazyGetters = lazyGetAccessors();
        this.lazySetters = lazySetAccessors();
    }

    @Override
    public TypeMirror typeMirror() {
        return typeMirror;
    }

    @Override
    public Types typeUtils() {
        return typeUtils;
    }

    @Override
    public Elements elementUtils() {
        return elementUtils;
    }

    public TypeFactory factory() {
        return factory;
    }

    public List<Type> typeArguments() {
        return lazyTypeArguments.get();
    }

    public List<Variable> fields() {
        return lazyFields.get();
    }

    public List<Executable> methods() {
        return lazyMethods.get();
    }

    public List<GetAccessor> getters() {
        return lazyGetters.get();
    }

    public List<SetAccessor> setters() {
        return lazySetters.get();
    }

    public Optional<GetAccessor> findGetter(String accessedName, TypeSupport accessedType) {
        return findGetter(accessedName, accessedType.typeMirror());
    }

    public Optional<GetAccessor> findGetter(String accessedName, TypeMirror accessedType) {
        return getters().stream()
                .filter(it -> it.accessedName().equals(accessedName)
                        && it.accessedType().isAssignableTo(accessedType))
                .findFirst();
    }

    public Optional<SetAccessor> findSetter(String accessedName, TypeSupport accessedType) {
        return findSetter(accessedName, accessedType.typeMirror());
    }

    public Optional<SetAccessor> findSetter(String accessedName, TypeMirror accessedType) {
        return setters().stream()
                .filter(it -> it.accessedName().equals(accessedName)
                        && it.accessedType().isAssignableFrom(accessedType))
                .findFirst();
    }

    public Type erasure() {
        return factory().getType(typeUtils().erasure(typeMirror()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return typeUtils.isSameType(typeMirror, ((Type) o).typeMirror);
    }

    @Override
    public int hashCode() {
        return typeMirror.hashCode();
    }

    @Override
    public String toString() {
        return typeMirror.toString();
    }

    private Lazy<List<Variable>> lazyFields() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> {
            List<VariableImpl> fields = Lists.newArrayList();
            collectFieldsInHierarchy(asDeclaredType(), fields);
            return ImmutableList.copyOf(fields);
        });
    }

    private Lazy<List<Executable>> lazyMethods() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> {
            List<ExecutableImpl> methods = Lists.newArrayList();
            collectMethodsInHierarchy(asDeclaredType(), methods, Sets.newHashSet());
            return ImmutableList.copyOf(methods);
        });
    }

    private Lazy<List<GetAccessor>> lazyGetAccessors() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> GetAccessor.from(this));
    }

    private Lazy<List<SetAccessor>> lazySetAccessors() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> SetAccessor.from(this));
    }

    private Lazy<List<Type>> lazyTypeArguments() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> ImmutableList.copyOf(
                asDeclaredType().getTypeArguments().stream().map(factory::getType).collect(Collectors.toList())
        ));
    }

    private void collectFieldsInHierarchy(DeclaredType declaredType,
                                          List<VariableImpl> collected) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
                    .filter(field -> {
                        return !field.getModifiers().contains(Modifier.STATIC);
                    })
                    .forEach(it -> collected.add(new VariableImpl(it)));
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectFieldsInHierarchy((DeclaredType) element.getSuperclass(), collected);
            }
        }
    }

    private void collectMethodsInHierarchy(DeclaredType declaredType,
                                           List<ExecutableImpl> collected,
                                           Set<TypeElement> visited) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName()) && visited.add(element)) {
            ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                    .filter(method -> {
                        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
                        boolean isPrivate = method.getModifiers().contains(Modifier.PRIVATE);
                        boolean isOverridden = collected.stream().anyMatch(it -> elementUtils.overrides(
                                it.executableElement, method, (TypeElement) it.executableElement.getEnclosingElement()
                        ));
                        return !isStatic && !isPrivate && !isOverridden;
                    })
                    .forEach(it -> collected.add(new ExecutableImpl(it)));

            element.getInterfaces().forEach(it -> {
                collectMethodsInHierarchy((DeclaredType) it, collected, visited);
            });

            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectMethodsInHierarchy((DeclaredType) element.getSuperclass(), collected, visited);
            }
        }
    }

    private class VariableImpl implements Variable {
        private final VariableElement variableElement;
        private final Type type;

        public VariableImpl(VariableElement variableElement) {
            this.variableElement = variableElement;
            this.type = factory().getType(asMember(variableElement));
        }

        @Override
        public VariableElement asElement() {
            return variableElement;
        }

        @Override
        public String simpleName() {
            return variableElement.getSimpleName().toString();
        }

        @Override
        public Type type() {
            return type;
        }
    }

    private class ExecutableImpl implements Executable {
        private final ExecutableElement executableElement;
        private final Lazy<ExecutableType> lazyExecutableType;
        private final Lazy<Type> lazyReturnType;
        private final Lazy<List<Variable>> lazyParameters;
        private final Lazy<List<Type>> lazyThrownTypes;

        public ExecutableImpl(ExecutableElement executableElement) {
            this.executableElement = executableElement;
            this.lazyExecutableType = Lazy.of(() -> asMember(executableElement));
            this.lazyReturnType = Lazy.of(() -> factory().getType(lazyExecutableType.get().getReturnType()));
            this.lazyParameters = Lazy.of(() -> ImmutableList.copyOf(
                    executableElement.getParameters().stream().map(VariableImpl::new).iterator()
            ));
            this.lazyThrownTypes = Lazy.of(() -> ImmutableList.copyOf(
                    lazyExecutableType.get().getThrownTypes().stream().map(it -> factory().getType(it)).iterator()
            ));
        }

        @Override
        public ExecutableElement asElement() {
            return executableElement;
        }

        @Override
        public String simpleName() {
            return executableElement.getSimpleName().toString();
        }

        @Override
        public Type returnType() {
            return lazyReturnType.get();
        }

        @Override
        public List<Variable> parameters() {
            return lazyParameters.get();
        }

        @Override
        public List<Type> thrownTypes() {
            return lazyThrownTypes.get();
        }
    }
}
