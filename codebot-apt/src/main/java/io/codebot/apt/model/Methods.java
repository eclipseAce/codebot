package io.codebot.apt.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
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
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Methods {
    private final Elements elementUtils;
    private final Types typeUtils;

    public static Methods instanceOf(ProcessingEnvironment processingEnv) {
        return new Methods(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
    }

    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";
    private static final String SETTER_PREFIX = "set";

    public Method of(DeclaredType containingType, ExecutableElement element) {
        ExecutableType methodType = (ExecutableType) typeUtils.asMemberOf(containingType, element);
        List<Parameter> parameters = Lists.newArrayList();
        for (int i = 0; i < element.getParameters().size(); i++) {
            parameters.add(new ParameterImpl(
                    element.getParameters().get(i),
                    methodType.getParameterTypes().get(i)
            ));
        }
        MethodImpl method = new MethodImpl(
                element,
                containingType,
                methodType.getReturnType(),
                Collections.unmodifiableList(parameters)
        );
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            return method;
        }
        String methodName = method.getSimpleName();
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
            String writeName = StringUtils.uncapitalize(methodName.substring(SETTER_PREFIX.length()));
            return new WriteMethodImpl(method, writeName, method.getParameters().get(0).getType());
        }
        return method;
    }

    public MethodCollection allOf(DeclaredType containingType) {
        List<ExecutableElement> elements = Lists.newLinkedList();
        collectMethodsInHierarchy(containingType, elements, Sets.newHashSet());
        return elements.stream()
                .map(element -> of(containingType, element))
                .collect(Collectors.toCollection(() -> new MethodCollectionImpl(typeUtils)));
    }

    public MethodCollection newCollection() {
        return new MethodCollectionImpl(typeUtils);
    }

    public MethodCollection newCollection(Iterable<? extends Method> methods) {
        MethodCollectionImpl coll = new MethodCollectionImpl(typeUtils);
        methods.forEach(coll::add);
        return coll;
    }

    private void collectMethodsInHierarchy(DeclaredType containingType,
                                           List<ExecutableElement> collected,
                                           Set<TypeElement> visited) {
        TypeElement element = (TypeElement) containingType.asElement();
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

        element.getInterfaces().forEach(it ->
                collectMethodsInHierarchy((DeclaredType) it, collected, visited)
        );

        if (element.getSuperclass().getKind() != TypeKind.NONE) {
            collectMethodsInHierarchy((DeclaredType) element.getSuperclass(), collected, visited);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static class MethodImpl implements Method {
        private final @Getter ExecutableElement element;
        private final @Getter DeclaredType containingType;
        private final @Getter TypeMirror returnType;
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
        private final @Getter TypeMirror readType;

        ReadMethodImpl(Method method, String readName, TypeMirror readType) {
            super(method.getElement(), method.getContainingType(),
                    method.getReturnType(), method.getParameters());
            this.readName = readName;
            this.readType = readType;
        }
    }

    private static class WriteMethodImpl extends MethodImpl implements WriteMethod {
        private final @Getter String writeName;
        private final @Getter TypeMirror writeType;

        WriteMethodImpl(Method method, String writeName, TypeMirror writeType) {
            super(method.getElement(), method.getContainingType(),
                    method.getReturnType(), method.getParameters());
            this.writeName = writeName;
            this.writeType = writeType;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private static class ParameterImpl implements Parameter {
        private final @Getter VariableElement element;
        private final @Getter TypeMirror type;

        @Override
        public String getName() {
            return element.getSimpleName().toString();
        }
    }

    private static class MethodCollectionImpl extends AbstractCollection<Method> implements MethodCollection {
        private final Types typeUtils;
        private final List<Method> methods = Lists.newArrayList();

        MethodCollectionImpl(Types typeUtils) {
            this.typeUtils = typeUtils;
        }

        @Override
        public boolean add(Method method) {
            return methods.add(method);
        }

        @Override
        public Iterator<Method> iterator() {
            return methods.iterator();
        }

        @Override
        public int size() {
            return methods.size();
        }

        @Override
        public Collection<ReadMethod> readers() {
            return methods.stream()
                    .filter(ReadMethod.class::isInstance)
                    .map(ReadMethod.class::cast)
                    .collect(Collectors.toList());
        }

        @Override
        public Collection<WriteMethod> writers() {
            return methods.stream()
                    .filter(WriteMethod.class::isInstance)
                    .map(WriteMethod.class::cast)
                    .collect(Collectors.toList());
        }

        @Override
        public WriteMethod findWriter(String name, TypeMirror assigningType) {
            return writers().stream()
                    .filter(it -> it.getWriteName().equals(name)
                            && typeUtils.isAssignable(assigningType, it.getWriteType())
                    )
                    .findFirst().orElse(null);
        }

        @Override
        public ReadMethod findReader(String name, TypeMirror acceptingType) {
            return readers().stream()
                    .filter(it -> it.getReadName().equals(name)
                            && typeUtils.isAssignable(it.getReadType(), acceptingType)
                    )
                    .findFirst().orElse(null);
        }
    }

}
