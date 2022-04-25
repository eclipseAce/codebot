package io.cruder.apt;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.*;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.util.BuilderSupport;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class CodegenBuilder extends BuilderSupport {
    private final Map<ClassName, TypeSpec.Builder> typeBuilders = Maps.newHashMap();

    private final Map<String, TypeName> typeRefs = Maps.newHashMap();

    private CodegenBuilder() {
        defaultTypeRefs();
    }

    public static CodegenBuilder codegen(Filer filer,
                                         @DelegatesTo(CodegenBuilder.class) Closure<?> cl)
            throws IOException {
        CodegenBuilder builder = new CodegenBuilder();
        cl.rehydrate(builder, cl.getOwner(), builder).call();
        builder.writeTo(filer);
        return builder;
    }

    public CodegenBuilder typeRef(Map<String, ?> mappings, String... qualifiedNames) {
        for (Map.Entry<String, ?> entry : mappings.entrySet()) {
            typeRef(entry.getKey(), typeOf(entry.getValue()));
        }
        typeRef(qualifiedNames);
        return this;
    }

    public CodegenBuilder typeRef(String... qualifiedNames) {
        for (String qualifiedName : qualifiedNames) {
            ClassName className = ClassName.bestGuess(qualifiedName);
            typeRef(className.simpleName(), className);
        }
        return this;
    }

    public CodegenBuilder typeRef(String name, TypeName typeName) {
        if (typeRefs.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Type alias '" + name + "' already mapped to '" + typeRefs.get(name) + "'");
        }
        typeRefs.put(name, typeName);
        return this;
    }

    public CodegenBuilder writeTo(Filer filer) throws IOException {
        for (Map.Entry<ClassName, TypeSpec.Builder> entry : typeBuilders.entrySet()) {
            JavaFile.builder(entry.getKey().packageName(), entry.getValue().build())
                    .build()
                    .writeTo(filer);
        }
        return this;
    }

    public TypeName typeOf(Object name) {
        if (name instanceof CharSequence) {
            String nameStr = name.toString();
            int arrayDepth = 0;
            while (nameStr.endsWith("[]")) {
                nameStr = nameStr.substring(0, nameStr.length() - 2);
                arrayDepth++;
            }
            TypeName typeName = typeRefs.get(nameStr);
            if (typeName == null) {
                typeName = ClassName.bestGuess(nameStr);
            }
            for (int i = 0; i < arrayDepth; i++) {
                typeName = ArrayTypeName.of(typeName);
            }
            return typeName;
        }
        if (name instanceof TypeName) {
            return (TypeName) name;
        }
        if (name instanceof TypeMirror) {
            return TypeName.get((TypeMirror) name);
        }
        if (name instanceof TypeElement) {
            return TypeName.get(((TypeElement) name).asType());
        }
        throw new IllegalArgumentException("Can't get type of '" + name + "'");
    }

    public ClassName classOf(Object name) {
        TypeName typeName = typeOf(name);
        if (typeName instanceof ClassName) {
            return (ClassName) typeName;
        }
        if (typeName instanceof ParameterizedTypeName) {
            return ((ParameterizedTypeName) typeName).rawType;
        }
        while (typeName instanceof ArrayTypeName) {
            typeName = ((ArrayTypeName) typeName).componentType;
            if (typeName instanceof ClassName) {
                return (ClassName) typeName;
            }
        }
        throw new IllegalArgumentException("Non-class type: '" + name + "'");
    }

    public TypeName typeOf(Object name, Object... params) {
        return ParameterizedTypeName.get(classOf(name),
                Stream.of(params).map(this::typeOf).toArray(TypeName[]::new));
    }

    public CodeBlock code(String format, Object... args) {
        return CodeBlock.of(format, args);
    }

    public CodeBlock code(Map<String, ?> args, String format) {
        return CodeBlock.builder().addNamed(format, args).build();
    }

    @Override
    protected void setParent(Object parent, Object child) {
    }

    @Override
    protected Object createNode(Object name) {
        return createNode(name, Collections.emptyMap(), null);
    }

    @Override
    protected Object createNode(Object name, Object value) {
        return createNode(name, Collections.emptyMap(), value);
    }

    @Override
    protected Object createNode(Object name, Map attributes) {
        return createNode(name, attributes, null);
    }

    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        switch ((String) name) {
            case "defClass":
                return classBuilder(attributes, value);
            case "defInterface":
                return interfaceBuilder(attributes, value);
            case "addField":
                return fieldBuilder(attributes, value);
            case "addMethod":
                return methodBuilder(attributes, value);
            case "addParameter":
                return parameterBuilder(attributes, value);
            case "addAnnotation":
                return annotationBuilder(attributes, value);
            case "addCode":
                return codeBlockBuilder(attributes, value, false);
            case "addStatement":
                return codeBlockBuilder(attributes, value, true);
            default:
                throw new IllegalArgumentException("Invalid keyword '" + name);
        }
    }

    @Override
    protected void nodeCompleted(Object parent, Object node) {
        if (node instanceof FieldSpec.Builder && parent instanceof TypeSpec.Builder) {
            ((TypeSpec.Builder) parent).addField(((FieldSpec.Builder) node).build());
        } //
        else if (node instanceof MethodSpec.Builder && parent instanceof TypeSpec.Builder) {
            ((TypeSpec.Builder) parent).addMethod(((MethodSpec.Builder) node).build());
        } //
        else if (node instanceof ParameterSpec.Builder && parent instanceof MethodSpec.Builder) {
            ((MethodSpec.Builder) parent).addParameter(((ParameterSpec.Builder) node).build());
        } //
        else if (node instanceof CodeBlock.Builder && parent instanceof MethodSpec.Builder) {
            ((MethodSpec.Builder) parent).addCode(((CodeBlock.Builder) node).build());
        } //
        else if (node instanceof AnnotationSpec.Builder) {
            if (parent instanceof TypeSpec.Builder) {
                ((TypeSpec.Builder) parent).addAnnotation(((AnnotationSpec.Builder) node).build());
            } //
            else if (parent instanceof FieldSpec.Builder) {
                ((FieldSpec.Builder) parent).addAnnotation(((AnnotationSpec.Builder) node).build());
            } //
            else if (parent instanceof MethodSpec.Builder) {
                ((MethodSpec.Builder) parent).addAnnotation(((AnnotationSpec.Builder) node).build());
            }
        }
    }

    private void defaultTypeRefs() {
        typeRef("boolean", TypeName.BOOLEAN);
        typeRef("byte", TypeName.BYTE);
        typeRef("short", TypeName.SHORT);
        typeRef("int", TypeName.INT);
        typeRef("long", TypeName.LONG);
        typeRef("char", TypeName.CHAR);
        typeRef("float", TypeName.FLOAT);
        typeRef("double", TypeName.DOUBLE);
        typeRef("Boolean", TypeName.BOOLEAN.box());
        typeRef("Byte", TypeName.BYTE.box());
        typeRef("Short", TypeName.SHORT.box());
        typeRef("Integer", TypeName.INT.box());
        typeRef("Long", TypeName.LONG.box());
        typeRef("Character", TypeName.CHAR.box());
        typeRef("Float", TypeName.FLOAT.box());
        typeRef("Double", TypeName.DOUBLE.box());
        typeRef("Object", TypeName.OBJECT);
        typeRef("String", ClassName.get("java.lang", "String"));
    }

    private List<TypeName> resolveTypes(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<TypeName> typeNames = Lists.newArrayList();
        if (value instanceof TypeName) {
            typeNames.add((TypeName) value);
        } else if (value instanceof CharSequence) {
            Stream.of(value.toString().split(","))
                    .filter(StringUtils::isNotBlank)
                    .forEach(it -> typeNames.add(typeOf(StringUtils.trim(it))));
        } else if (value instanceof Collection) {
            ((Collection<?>) value).forEach(it -> typeNames.add(typeOf(it)));
        }
        return typeNames;
    }

    private Modifier[] resolveModifiers(Object value) {
        if (value == null) {
            return new Modifier[0];
        }
        if (value instanceof CharSequence) {
            return Stream.of(value.toString().split(","))
                    .filter(StringUtils::isNotBlank)
                    .map(it -> Modifier.valueOf(it.toUpperCase()))
                    .toArray(Modifier[]::new);
        }
        throw new IllegalArgumentException("Can't resolve modifiers '" + value + "'");
    }

    private TypeSpec.Builder classBuilder(Map attributes, Object value) {
        TypeSpec.Builder builder = typeBuilders
                .computeIfAbsent(classOf(value), TypeSpec::classBuilder)
                .addModifiers(resolveModifiers(attributes.get("modifiers")));
        Object superclass = attributes.get("extends");
        if (superclass != null) {
            builder.superclass(typeOf(superclass));
        }
        resolveTypes(attributes.get("implements"))
                .forEach(builder::addSuperinterface);
        return builder;
    }

    private TypeSpec.Builder interfaceBuilder(Map attributes, Object value) {
        TypeSpec.Builder builder = typeBuilders
                .computeIfAbsent(classOf(value), TypeSpec::interfaceBuilder)
                .addModifiers(resolveModifiers(attributes.get("modifiers")));
        resolveTypes(attributes.get("extends"))
                .forEach(builder::addSuperinterface);
        return builder;
    }

    private FieldSpec.Builder fieldBuilder(Map attributes, Object value) {
        return FieldSpec.builder(typeOf(attributes.get("type")), (String) value)
                .addModifiers(resolveModifiers(attributes.get("modifiers")));
    }

    private Object methodBuilder(Map attributes, Object value) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder((String) value)
                .addModifiers(resolveModifiers(attributes.get("modifiers")));
        Object returns = attributes.get("returns");
        if (returns != null) {
            builder.returns(typeOf(returns));
        }
        resolveTypes(attributes.get("throws"))
                .forEach(builder::addException);
        return builder;
    }

    private Object parameterBuilder(Map attributes, Object value) {
        Object type = attributes.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Field type is missing");
        }
        return ParameterSpec.builder(typeOf(attributes.get("type")), (String) value)
                .addModifiers(resolveModifiers(attributes.get("modifiers")));
    }

    private AnnotationSpec.Builder annotationBuilder(Map attributes, Object value) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(classOf(value));
        attributes.forEach((key, val) -> {
            if (val instanceof CharSequence) {
                builder.addMember((String) key, "$S", val.toString());
            } else if (val instanceof CodeBlock) {
                builder.addMember((String) key, (CodeBlock) val);
            } else if (val instanceof TypeName) {
                builder.addMember((String) key, "$T.class", val);
            } else {
                builder.addMember((String) key, "$L", val);
            }
        });
        return builder;
    }

    private CodeBlock.Builder codeBlockBuilder(Map attributes, Object value, boolean statement) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (value instanceof CodeBlock) {
            if (statement) {
                builder.addStatement((CodeBlock) value);
            } else {
                builder.add((CodeBlock) value);
            }
        } else if (value instanceof CharSequence) {
            if (statement) {
                builder.addStatement(CodeBlock.builder().addNamed(value.toString(), attributes).build());
            } else {
                builder.addNamed((String) value, attributes);
            }
        }
        return builder;
    }
}
