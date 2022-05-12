package io.codebot.apt.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Set;

public class Variable implements Annotated, Modified {
    private final VariableElement variableElement;
    private final Lazy<Type> lazyType;
    private final Lazy<List<Annotation>> lazyAnnotations;

    Variable(Type enclosingType, VariableElement variableElement) {
        this.variableElement = variableElement;
        this.lazyType = Lazy.of(() ->
                enclosingType.factory().getType(enclosingType.asMember(variableElement))
        );
        this.lazyAnnotations = Lazy.of(() -> ImmutableList.copyOf(
                variableElement.getAnnotationMirrors().stream().map(Annotation::new).iterator()
        ));
    }

    @Override
    public List<Annotation> annotations() {
        return lazyAnnotations.get();
    }

    @Override
    public Set<Modifier> modifiers() {
        return variableElement.getModifiers();
    }

    public VariableElement element() {
        return variableElement;
    }

    public String simpleName() {
        return variableElement.getSimpleName().toString();
    }

    public Type type() {
        return lazyType.get();
    }

    public static List<Variable> fieldsOf(Type type) {
        List<VariableElement> fields = Lists.newArrayList();
        collectFieldsInHierarchy(type.asDeclaredType(), fields);
        return ImmutableList.copyOf(fields.stream().map(it -> new Variable(type, it)).iterator());
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
