package io.cruder.apt;

import com.google.auto.service.AutoService;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.classfile.Type;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes({TemplateProcessor.TEMPLATE_ANNOTATION_NAME})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TemplateProcessor extends AbstractProcessor {
    public static final String TEMPLATE_ANNOTATION_NAME = "io.cruder.apt.Template";

    private JavacProcessingEnvironment processingEnv;

    private JavacTypes types;
    private JavacElements elements;
    private JavacTrees trees;
    private TreeMaker treeMaker;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = (JavacProcessingEnvironment) processingEnv;
        this.trees = JavacTrees.instance(this.processingEnv.getContext());
        this.treeMaker = TreeMaker.instance(this.processingEnv.getContext());
        this.types = this.processingEnv.getTypeUtils();
        this.elements = this.processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                    ReplicaTreeCopier copier = new ReplicaTreeCopier();
                    TreePath treePath = trees.getPath(element);
                    JCTree.JCCompilationUnit replica = copier.copy(
                            (JCTree.JCCompilationUnit) treePath.getCompilationUnit());
                    System.out.println(replica);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private class ReplicaTreeCopier extends TreeCopier<Void> {
        public ReplicaTreeCopier() {
            super(treeMaker);
        }

        @Override
        public <T extends JCTree> List<T> copy(List<T> list, Void unused) {
            if (list == null) {
                return null;
            }
            ListBuffer buffer = new ListBuffer();
            for (T item : list) {
                T copy = copy(item, unused);
                if (copy != null) {
                    buffer.append(copy);
                }
            }
            return buffer.toList();
        }

        @Override
        public JCTree visitAnnotation(AnnotationTree annotationTree, Void unused) {
            JCTree.JCAnnotation t = (JCTree.JCAnnotation) annotationTree;
            String name = t.annotationType.type.asElement()
                    .getQualifiedName().toString();
            if (name.equals(TEMPLATE_ANNOTATION_NAME)) {
                return null;
            }
            return super.visitAnnotation(annotationTree, unused);
        }

        @Override
        public JCTree visitIdentifier(IdentifierTree identifierTree, Void unused) {
            JCTree.JCIdent t = (JCTree.JCIdent) identifierTree;
            if (t.type != null) {
                Symbol.TypeSymbol sym = t.type.asElement();
                if (sym != null) {
                    String typeName = sym.getQualifiedName().toString();
                    if (typeName.startsWith("template.crud.dto.T")) {
                        String replaceName = "io.cruder.example.dto.user.User"
                                + typeName.substring("template.crud.dto.T".length());
                        return treeMaker.Ident(elements.getName(replaceName));
                    }
                    if (typeName.equals("template.crud.TEntity")) {
                        String replaceName = "io.cruder.example.domain.User";
                        return treeMaker.Ident(elements.getName(replaceName));
                    }
                }
            }
            return super.visitIdentifier(identifierTree, unused);
        }

        @Override
        public JCTree visitVariable(VariableTree variableTree, Void unused) {
            JCTree.JCVariableDecl t = (JCTree.JCVariableDecl) variableTree;
            System.out.println("Variable " + t.name + " is " + t.vartype.type);
            return super.visitVariable(variableTree, unused);
        }

        @Override
        public JCTree visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
            return super.visitMemberSelect(memberSelectTree, unused);
        }
    }
}
