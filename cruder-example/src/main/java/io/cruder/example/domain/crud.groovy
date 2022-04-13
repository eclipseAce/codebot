package io.cruder.example.domain

import com.squareup.javapoet.*
import io.cruder.apt.bean.BeanInfo

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier

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

def beanClass(ClassName name, List<FieldSpec> fields) {
    TypeSpec.Builder typeBld = TypeSpec.classBuilder(name.simpleName())
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

final namePrefix = beanInfo.typeElement.simpleName
final basePackage = "io.cruder.example.generated"

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
final teRestController = processingEnv.elementUtils
        .getTypeElement("org.springframework.web.bind.annotation.RestController")
final teRequestBody = processingEnv.elementUtils
        .getTypeElement("org.springframework.web.bind.annotation.RequestBody")
final teValid = processingEnv.elementUtils
        .getTypeElement("javax.validation.Valid")
final teRepository = processingEnv.elementUtils
        .getTypeElement("org.springframework.stereotype.Repository")
final teRequestMapping = processingEnv.elementUtils
        .getTypeElement("org.springframework.web.bind.annotation.RequestMapping")
final teRequestMethod = processingEnv.elementUtils
        .getTypeElement("org.springframework.web.bind.annotation.RequestMethod")
final teAutowired = processingEnv.elementUtils
        .getTypeElement("org.springframework.beans.factory.annotation.Autowired")
final teTransactional = processingEnv.elementUtils
        .getTypeElement("org.springframework.transaction.annotation.Transactional")
final teJpaRepository = processingEnv.elementUtils
        .getTypeElement("org.springframework.data.jpa.repository.JpaRepository")

final teApiResult = processingEnv.elementUtils
        .getTypeElement("io.cruder.example.core.ApiReply")

var entityTypeName = ClassName.get(beanInfo.typeElement)

var addDtoTypeName = ClassName.get("${basePackage}.dto", "${namePrefix}AddDTO")
var addDtoType = beanClass(addDtoTypeName, writableFields('username', 'password', 'mobile', 'email'))

var converterTypeName = ClassName.get("${basePackage}.converter", "${namePrefix}Converter")
var converterType = TypeSpec
        .interfaceBuilder(converterTypeName)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AnnotationSpec
                .builder(ClassName.get(teMapper))
                .addMember("componentModel", '"spring"')
                .build())
        .addMethod(MethodSpec
                .methodBuilder("convertAddToEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(addDtoTypeName, "dto")
                .returns(entityTypeName)
                .build())
        .build()

var repositoryTypeName = ClassName.get("${basePackage}.repository", "${namePrefix}Repository")
var repositoryType = TypeSpec
        .interfaceBuilder(repositoryTypeName)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(ClassName.get(teRepository))
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(teJpaRepository), entityTypeName, TypeName.LONG.box()))
        .build()

var serviceTypeName = ClassName.get("${basePackage}.service", "${namePrefix}Service")
var serviceType = TypeSpec
        .classBuilder(serviceTypeName)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(ClassName.get(teService))
        .addField(FieldSpec
                .builder(converterTypeName, "converter")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get(teAutowired))
                .build())
        .addField(FieldSpec
                .builder(repositoryTypeName, "repository")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get(teAutowired))
                .build())
        .addMethod(MethodSpec
                .methodBuilder("add")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(addDtoTypeName, "dto")
                .returns(TypeName.LONG.box())
                .addAnnotation(ClassName.get(teTransactional))
                .addCode("\$T entity = converter.convertAddToEntity(dto);\n", entityTypeName)
                .addCode("repository.save(entity);\n")
                .addCode("return entity.getId();\n")
                .build())
        .build()

var controllerTypeName = ClassName.get("${basePackage}.controller", "${namePrefix}Controller")
var controllerType = TypeSpec
        .classBuilder(controllerTypeName)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(ClassName.get(teRestController))
        .addAnnotation(AnnotationSpec
                .builder(ClassName.get(teRequestMapping))
                .addMember("path", '"/api/\$L"', entityTypeName.simpleName().toLowerCase())
                .build())
        .addField(FieldSpec
                .builder(serviceTypeName, "service")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get(teAutowired))
                .build())
        .addMethod(MethodSpec
                .methodBuilder("add")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.get(teRequestMapping))
                        .addMember("method", "\$T.POST", teRequestMethod)
                        .addMember("path", '"/add"')
                        .build())
                .addParameter(ParameterSpec
                        .builder(addDtoTypeName, "body")
                        .addAnnotation(ClassName.get(teRequestBody))
                        .addAnnotation(ClassName.get(teValid))
                        .build())
                .returns(ParameterizedTypeName.get(ClassName.get(teApiResult), TypeName.LONG.box()))
                .addCode("return \$T.ok(service.add(body));\n", ClassName.get(teApiResult))
                .build())
        .build()

JavaFile.builder(addDtoTypeName.packageName(), addDtoType).build().writeTo(processingEnv.filer)
JavaFile.builder(converterTypeName.packageName(), converterType).build().writeTo(processingEnv.filer)
JavaFile.builder(repositoryTypeName.packageName(), repositoryType).build().writeTo(processingEnv.filer)
JavaFile.builder(serviceTypeName.packageName(), serviceType).build().writeTo(processingEnv.filer)
JavaFile.builder(controllerTypeName.packageName(), controllerType).build().writeTo(processingEnv.filer)