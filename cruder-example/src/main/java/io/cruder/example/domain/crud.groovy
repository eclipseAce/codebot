package io.cruder.example.domain

import com.squareup.javapoet.*
import io.cruder.apt.bean.BeanInfo

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind

def findFields(Collection<BeanInfo.Property> props, String... names) {
    props.findAll { prop -> names.contains(prop.name) }.collect { prop ->
        FieldSpec.builder(TypeName.get(prop.type).box(), prop.name, Modifier.PRIVATE).build()
    }
}

def writableFields(String... names) {
    findFields(__beanInfo.writableProperties.values(), names)
}

def readableFields(String... names) {
    findFields(__beanInfo.readableProperties.values(), names)
}

def beanClass(String name, List<FieldSpec> fields) {
    TypeSpec.Builder typeBld = TypeSpec.classBuilder(name)
            .addModifiers(Modifier.PUBLIC)
    fields.each { field ->
        typeBld.addField(field)
                .addMethod(MethodSpec.methodBuilder("get" + field.name.capitalize())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(field.type)
                        .addCode("return this.\$N;", field.name)
                        .build())
                .addMethod(MethodSpec.methodBuilder("set" + field.name.capitalize())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(field.type, field.name)
                        .addCode("this.\$N = \$N;", field.name, field.name)
                        .build())
    }
    typeBld.build()
}


final ProcessingEnvironment processingEnv = __processingEnv
final BeanInfo beanInfo = __beanInfo

var namePrefix = beanInfo.typeElement.simpleName
var basePackage = "io.cruder.example.generated"

//beanClass("${namePrefix}SetProfileDTO",
//        readableFields('id') + writableFields('username', 'password', 'mobile', 'email')),
//beanClass("${namePrefix}SetLockedDTO",
//        readableFields('id') + writableFields('locked')),
//beanClass("${namePrefix}DetailsDTO",
//        readableFields('id', 'username', 'locked', 'mobile', 'email', 'createdAt', 'updatedAt')),
//beanClass("${namePrefix}SummaryDTO",
//        readableFields('id', 'username', 'createdAt', 'updatedAt')),

final teMapper = processingEnv.elementUtils
        .getTypeElement("org.mapstruct.Mapper")
final teService = processingEnv.elementUtils
        .getTypeElement("org.springframework.stereotype.Service")
final teRepository = processingEnv.elementUtils
        .getTypeElement("org.springframework.stereotype.Repository")
final teAutowired = processingEnv.elementUtils
        .getTypeElement("org.springframework.beans.factory.annotation.Autowired")
final teTransactional = processingEnv.elementUtils
        .getTypeElement("org.springframework.transaction.annotation.Transactional")
final teJpaRepository = processingEnv.elementUtils
        .getTypeElement("org.springframework.data.jpa.repository.JpaRepository")
final teLong = processingEnv.elementUtils
        .getTypeElement("java.lang.Long")

var addDtoType = beanClass("${namePrefix}AddDTO",
        writableFields('username', 'password', 'mobile', 'email'))

var converterType = TypeSpec
        .interfaceBuilder("${namePrefix}Converter")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AnnotationSpec
                .builder(ClassName.get(teMapper))
                .addMember("componentModel", '"spring"')
                .build())
        .addMethod(MethodSpec
                .methodBuilder("convertAddToEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ClassName.get("${basePackage}.dto", addDtoType.name), "dto")
                .returns(ClassName.get(beanInfo.typeElement))
                .build())
        .build()

var repositoryType = TypeSpec
        .interfaceBuilder("${namePrefix}Repository")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(ClassName.get(teRepository))
        .addSuperinterface(processingEnv.typeUtils
                .getDeclaredType(teJpaRepository, beanInfo.typeElement.asType(), teLong.asType()))
        .build()

var serviceType = TypeSpec
        .classBuilder("${namePrefix}Service")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(ClassName.get(teService))
        .addField(FieldSpec
                .builder(ClassName.get("${basePackage}.converter", converterType.name), "converter")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get(teAutowired))
                .build())
        .addField(FieldSpec
                .builder(ClassName.get("${basePackage}.repository", repositoryType.name), "repository")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get(teAutowired))
                .build())
        .addMethod(MethodSpec
                .methodBuilder("add")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("${basePackage}.dto", addDtoType.name), "dto")
                .returns(TypeName.LONG.box())
                .addAnnotation(ClassName.get(teTransactional))
                .addCode("\$T entity = converter.convertAddToEntity(dto);\n", ClassName.get(beanInfo.typeElement))
                .addCode("repository.save(entity);\n")
                .addCode("return entity.getId();\n", ClassName.get(beanInfo.typeElement))
                .build())
        .build()

JavaFile.builder("${basePackage}.dto", addDtoType).build().writeTo(processingEnv.filer)
JavaFile.builder("${basePackage}.converter", converterType).build().writeTo(processingEnv.filer)
JavaFile.builder("${basePackage}.repository", repositoryType).build().writeTo(processingEnv.filer)
JavaFile.builder("${basePackage}.service", serviceType).build().writeTo(processingEnv.filer)