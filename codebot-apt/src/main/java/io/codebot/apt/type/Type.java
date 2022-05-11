package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import io.codebot.apt.util.Lazy;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> Variable.fieldsOf(this));
    }

    private Lazy<List<Executable>> lazyMethods() {
        return !isDeclared() ? Lazy.constant(ImmutableList.of()) : Lazy.of(() -> Executable.methodsOf(this));
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


}
