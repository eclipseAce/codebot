package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class Field implements Annotated, Modified {
    private final VariableElement variableElement;
    private final Type type;
    private final Lazy<List<Annotation>> lazyAnnotations;

    Field(VariableElement variableElement, Type type) {
        this.variableElement = variableElement;
        this.type = type;
        this.lazyAnnotations = Lazy.of(() -> ImmutableList.copyOf(
                variableElement.getAnnotationMirrors().stream().map(Annotation::new).iterator()
        ));
    }

    @Override
    public List<Annotation> getAnnotations() {
        return lazyAnnotations.get();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return variableElement.getModifiers();
    }

    public VariableElement getElement() {
        return variableElement;
    }

    public String getSimpleName() {
        return variableElement.getSimpleName().toString();
    }

    public Type getType() {
        return type;
    }

    public static List<Field> fieldsOf(Type enclosingType) {
        List<VariableElement> fields = Lists.newArrayList();
        collectFieldsInHierarchy(enclosingType.asDeclaredType(), fields);
        return ImmutableList.copyOf(
                fields.stream().map(it ->
                        new Field(it, enclosingType.getFactory().getType(enclosingType.asMember(it)))
                ).iterator()
        );
    }

    private static void collectFieldsInHierarchy(DeclaredType declaredType,
                                                 List<VariableElement> collected) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (!"java.lang.Object".contentEquals(element.getQualifiedName())) {
            ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
                    .filter(field -> {
                        return !field.getModifiers().contains(Modifier.STATIC);
                    })
                    .forEach(collected::add);
            if (element.getSuperclass().getKind() != TypeKind.NONE) {
                collectFieldsInHierarchy((DeclaredType) element.getSuperclass(), collected);
            }
        }
    }
}
