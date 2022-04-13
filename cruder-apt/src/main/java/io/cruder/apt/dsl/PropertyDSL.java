package io.cruder.apt.dsl;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;
import java.util.Arrays;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyDSL {
    @Getter
    private final FieldSpec.Builder fieldBuilder;
    @Getter
    private final MethodSpec.Builder getterBuilder;

    @Getter
    private final MethodSpec.Builder setterBuilder;

    @Getter
    private boolean noGetter;

    @Getter
    private boolean noSetter;

    public static PropertyDSL property(TypeName type, String name,
                                       @DelegatesTo(PropertyDSL.class) Closure<?> cl) {
        PropertyDSL dsl = new PropertyDSL(
                FieldSpec.builder(type, name, Modifier.PRIVATE),
                getter(type, name),
                setter(type, name)
        );
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public void noGetter() {
        this.noGetter = true;
    }

    public void noSetter() {
        this.noSetter = true;
    }

    private static MethodSpec.Builder getter(TypeName type, String name) {
        String prefix = TypeName.BOOLEAN.equals(type) ? "is" : "get";
        return MethodSpec.methodBuilder(prefix + StringUtils.capitalize(name))
                .addModifiers(Arrays.asList(Modifier.PUBLIC))
                .returns(type)
                .addCode("return this.$N;", name);
    }

    private static MethodSpec.Builder setter(TypeName type, String name) {
        return MethodSpec.methodBuilder("set" + StringUtils.capitalize(name))
                .addModifiers(Arrays.asList(Modifier.PUBLIC))
                .addParameter(type, name)
                .addCode("this.$N = $N;", name, name);
    }
}
