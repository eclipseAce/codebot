package io.cruder.apt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import io.cruder.apt.mapstruct.Mapper;
import io.cruder.apt.util.AnnotationUtils;
import io.cruder.apt.util.TypeNameUtils;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

public class TemplateCloner {
    private final String basePackage;
    private final String[] replaceTypeName;
    private final List<String[]> replaceStringLiterals;
    private final List<String[]> replaceTypes;

    public TemplateCloner(
            ProcessingEnvironment processingEnv,
            Template template,
            ReplaceTypeName replaceTypeName,
            ReplaceStringLiteral[] replaceStringLiterals,
            ReplaceType[] replaceTypes) {

        this.basePackage = template.basePackage();

        this.replaceTypeName = new String[] {
                replaceTypeName.regex(),
                replaceTypeName.replacement()
        };

        this.replaceStringLiterals = Stream.of(replaceStringLiterals)
                .map(it -> new String[] { it.regex(), it.replacement() })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ArrayList::new));

        this.replaceTypes = Stream.of(replaceTypes)
                .map(it -> new String[] {
                        TypeNameUtils.getQualifiedName((TypeElement) processingEnv.getTypeUtils()
                                .asElement(AnnotationUtils.getClassValue(it, ReplaceType::target))),
                        TypeNameUtils.getQualifiedName((TypeElement) processingEnv.getTypeUtils()
                                .asElement(AnnotationUtils.getClassValue(it, ReplaceType::with)))
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ArrayList::new));

        AnnotationUtils.getClassValues(template, Template::uses).stream()
                .map(it -> (TypeElement) processingEnv.getTypeUtils().asElement(it))
                .forEach(use -> {
                    this.replaceTypes.add(new String[] {
                            TypeNameUtils.getQualifiedName(use),
                            this.basePackage + "." + replaceTypeName(TypeNameUtils.getSimpleName(use))
                    });
                });
    }

    private String replace(String input, String[] replace) {
        return input.replaceAll(replace[0], replace[1]);
    }

    private String replaceTypeName(String input) {
        return replace(input, replaceTypeName);
    }

    private String replaceStringLiteral(String input) {
        for (String[] replace : replaceStringLiterals) {
            input = replace(input, replace);
        }
        return input;
    }

    private String replaceType(String input) {
        for (String[] replace : replaceTypes) {
            if (replace[0].contentEquals(input)) {
                return replace[1];
            }
        }
        return null;
    }

    public <T extends CtElement> T clone(CtElement element) {
        CloneVisitor cv = new ReplacingCloneVisitor(new ReplacingCloneHelper());
        cv.scan(element);
        return cv.getClone();
    }

    private class ReplacingCloneVisitor extends CloneVisitor {
        public ReplacingCloneVisitor(ReplacingCloneHelper cloneHelper) {
            super(cloneHelper);
        }

        @Override
        public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
            if (annotation.getType().getQualifiedName().equals(Mapper.class.getName())) {
                CtAnnotation<?> actual = annotation.getFactory().createAnnotation();
                actual.setAnnotationType(annotation.getFactory().Type()
                        .createReference("org.mapstruct.Mapper"));
                actual.setValues(annotation.getValues());
                super.visitCtAnnotation(actual);
            } else {
                super.visitCtAnnotation(annotation);
            }
        }

        @Override
        public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
            String replaced = replaceType(reference.getQualifiedName());
            if (replaced != null) {
                CtTypeReference<T> referenceReplace = reference.getFactory().Type()
                        .createReference(replaced);
                super.visitCtTypeReference(referenceReplace);
            } else {
                super.visitCtTypeReference(reference);
            }
        }

        @Override
        public <T> void visitCtLiteral(CtLiteral<T> literal) {
            if (String.class.getName().equals(literal.getType().getQualifiedName())) {
                String replaced = replaceStringLiteral((String) literal.getValue());
                super.visitCtLiteral(literal.getFactory().createLiteral(replaced));
            } else {
                super.visitCtLiteral(literal);
            }
        }
    }

    private class ReplacingCloneHelper extends CloneHelper {

        @Override
        public <T extends CtElement> T clone(T element) {
            ReplacingCloneVisitor cloneVisitor = new ReplacingCloneVisitor(this);
            cloneVisitor.scan(element);
            return cloneVisitor.getClone();
        }

        @Override
        public void tailor(CtElement topLevelElement, CtElement topLevelClone) {
            if (topLevelClone instanceof CtType) {
                CtType<?> type = (CtType<?>) topLevelClone;
                if (type.isTopLevel()) {
                    type.setSimpleName(replaceTypeName(type.getSimpleName()));
                    type.getFactory().Package().create(null, basePackage)
                            .addType(type);
                }
            }
        }

    }
}
