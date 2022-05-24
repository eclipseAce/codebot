package io.codebot.apt.code;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Methods {

    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";
    private static final String SETTER_PREFIX = "set";

    public static List<? extends Method> allOf(Type containingType) {
        List<ExecutableElement> elements = Lists.newArrayList();
        collectMethodsInHierarchy(
                containingType.asDeclaredType(),
                elements, Sets.newHashSet(),
                containingType.getFactory().getElementUtils()
        );
        return elements.stream()
                .map(element -> of(containingType, element))
                .collect(Collectors.toList());
    }

    public static Method of(Type containingType, ExecutableElement element) {
        TypeFactory typeFactory = containingType.getFactory();
        ExecutableType methodType = containingType.asMember(element);
        List<Parameter> parameters = Lists.newArrayList();
        for (int i = 0; i < element.getParameters().size(); i++) {
            parameters.add(new ParameterImpl(
                    element.getParameters().get(i),
                    typeFactory.getType(methodType.getParameterTypes().get(i))
            ));
        }
        MethodImpl method = new MethodImpl(
                element,
                containingType,
                typeFactory.getType(methodType.getReturnType()),
                Collections.unmodifiableList(parameters)
        );
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            return method;
        }
        String methodName = method.getSimpleName().toString();
        if (methodName.length() > GETTER_PREFIX.length()
                && methodName.startsWith(GETTER_PREFIX)
                && method.getParameters().isEmpty()
                && methodType.getReturnType().getKind() != TypeKind.VOID) {
            String readName = StringUtils.uncapitalize(methodName.substring(GETTER_PREFIX.length()));
            return new ReadMethodImpl(method, readName, method.getReturnType());
        }
        if (methodName.length() > BOOLEAN_GETTER_PREFIX.length()
                && methodName.startsWith(BOOLEAN_GETTER_PREFIX)
                && method.getParameters().isEmpty()
                && methodType.getReturnType().getKind() == TypeKind.BOOLEAN) {
            String readName = StringUtils.uncapitalize(methodName.substring(BOOLEAN_GETTER_PREFIX.length()));
            return new ReadMethodImpl(method, readName, method.getReturnType());
        }
        if (methodName.length() > SETTER_PREFIX.length()
                && methodName.startsWith(SETTER_PREFIX)
                && method.getParameters().size() == 1) {
            String writeName = StringUtils.uncapitalize(methodName.substring(BOOLEAN_GETTER_PREFIX.length()));
            return new WriteMethodImpl(method, writeName, method.getParameters().get(0).getType());
        }
        return method;
    }

    private static void collectMethodsInHierarchy(DeclaredType declaredType,
                                                  List<ExecutableElement> collected,
                                                  Set<TypeElement> visited,
                                                  Elements elementUtils) {
        TypeElement element = (TypeElement) declaredType.asElement();
        if (Object.class.getName().contentEquals(element.getQualifiedName()) || !visited.add(element)) {
            return;
        }
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
            collectMethodsInHierarchy((DeclaredType) it, collected, visited, elementUtils);
        });

        if (element.getSuperclass().getKind() != TypeKind.NONE) {
            collectMethodsInHierarchy((DeclaredType) element.getSuperclass(), collected, visited, elementUtils);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static class MethodImpl implements Method {
        private final @Getter ExecutableElement element;
        private final @Getter Type containingType;
        private final @Getter Type returnType;
        private final @Getter List<? extends Parameter> parameters;

        @Override
        public String getSimpleName() {
            return element.getSimpleName().toString();
        }

        @Override
        public Set<Modifier> getModifiers() {
            return element.getModifiers();
        }
    }

    private static class ReadMethodImpl extends MethodImpl implements ReadMethod {
        private final @Getter String readName;
        private final @Getter Type readType;

        ReadMethodImpl(Method method, String readName, Type readType) {
            super(method.getElement(), method.getContainingType(),
                    method.getReturnType(), method.getParameters());
            this.readName = readName;
            this.readType = readType;
        }
    }

    private static class WriteMethodImpl extends MethodImpl implements WriteMethod {
        private final @Getter String writeName;
        private final @Getter Type writeType;

        WriteMethodImpl(Method method, String writeName, Type writeType) {
            super(method.getElement(), method.getContainingType(),
                    method.getReturnType(), method.getParameters());
            this.writeName = writeName;
            this.writeType = writeType;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static class ParameterImpl implements Parameter {
        private final @Getter VariableElement element;
        private final @Getter Type type;

        @Override
        public String getName() {
            return element.getSimpleName().toString();
        }
    }
}
