package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.cruder.apt.util.TypeIterator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanModel {

    private static final String SETTER_PREFIX = "set";

    private final TypeElement element;
    private final List<FieldModel> fields;
    private final List<GetterModel> getters;
    private final List<SetterModel> setters;

    public Optional<FieldModel> findField(Predicate<FieldModel> filter) {
        return fields.stream().filter(filter).findFirst();
    }

    public Optional<GetterModel> findGetter(Predicate<GetterModel> filter) {
        return getters.stream().filter(filter).findFirst();
    }

    public Optional<SetterModel> findSetter(Predicate<SetterModel> filter) {
        return setters.stream().filter(filter).findFirst();
    }

    public Optional<GetterModel> findGetter(String name, TypeMirror type) {
        return findGetter(getter -> getter.getName().equals(name) && getter.isAssignableTo(type));
    }

    public Optional<SetterModel> findSetter(String name, TypeMirror type) {
        return findSetter(setter -> setter.getName().equals(name) && setter.isAssignableFrom(type));
    }

    public static BeanModel beanOf(ModelContext ctx, DeclaredType beanType) {
        List<FieldModel> fields = Lists.newArrayList();
        List<GetterModel> getters = Lists.newArrayList();
        List<SetterModel> setters = Lists.newArrayList();
        TypeIterator.from(beanType).forEachRemaining(i -> {
            fields.addAll(FieldModel.fieldsOf(ctx, i.typeResolver(), i.element()));
            getters.addAll(GetterModel.gettersOf(ctx, i.typeResolver(), i.element()));
            setters.addAll(SetterModel.settersOf(ctx, i.typeResolver(), i.element()));
        });
        return new BeanModel(
                (TypeElement) beanType.asElement(),
                ImmutableList.copyOf(fields),
                ImmutableList.copyOf(getters),
                ImmutableList.copyOf(setters)
        );
    }
}
