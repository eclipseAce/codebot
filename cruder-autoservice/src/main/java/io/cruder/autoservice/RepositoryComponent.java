package io.cruder.autoservice;

import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;

@RequiredArgsConstructor
public class RepositoryComponent {
    private final ProcessingContext ctx;
    private final @Getter ClassName name;
    private final @Getter EntityDescriptor entity;

    public static RepositoryComponent forEntity(ProcessingContext ctx, EntityDescriptor entity) {
        ClassName entityName = ClassName.get(entity.getEntityElement());
        String pkg = entityName.packageName();
        int sepIndex = pkg.lastIndexOf('.');
        if (sepIndex > -1) {
            pkg = pkg.substring(0, sepIndex) + ".repository";
        }
        ClassName repositoryName = ClassName.get(pkg, entityName.simpleName() + "Repository");
        return new RepositoryComponent(ctx, repositoryName, entity);
    }

    public boolean isNecessary() {
        return true;
    }

    public JavaFile createComponent() {
        TypeSpec type = TypeSpec.interfaceBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
                        ClassName.get(entity.getEntityElement()),
                        TypeName.get(entity.getIdField().getType()).box()
                ))
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaSpecificationExecutor"),
                        ClassName.get(entity.getEntityElement())
                ))
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Repository"))
                .build();
        return JavaFile.builder(name.packageName(), type).build();
    }
}
