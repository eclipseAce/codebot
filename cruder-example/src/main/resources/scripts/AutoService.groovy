package scripts

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ParameterizedTypeName
import groovy.transform.BaseScript
import io.cruder.apt.CodegenScript

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import java.lang.reflect.Modifier

@BaseScript
CodegenScript theScript

def services = roundEnv.getElementsAnnotatedWith(
        elementUtils.getTypeElement('io.cruder.example.codegen.AutoService')
).collect { it as TypeElement }


def matchFindPageMethod(ExecutableElement method) {
    if (!typeUtils.isSameType(((DeclaredType) method.returnType).asElement().asType(),
            elementUtils.getTypeElement('org.springframework.data.domain.Page').asType())) {
        return [matches: false]
    }
    def predicateParam = method.parameters.find {
        println(it.asType())
        typeUtils.isAssignable(it.asType(),
                typeUtils.getDeclaredType(
                        elementUtils.getTypeElement('org.springframework.data.jpa.domain.Specification'),
                        typeUtils.getWildcardType(null, null)
                ))
    }
    if (predicateParam == null) {
        print("no predicate param")
        return [matches: false]
    }
    def pageableParam = method.parameters.find {
        typeUtils.isSameType(it.asType(),
                elementUtils.getTypeElement('org.springframework.data.domain.Pageable').asType())
    }
    if (pageableParam == null) {
        print("no pageable param")
        return [matches: false]
    }
    return [matches       : true,
            dtoType       : method.returnType.typeArguments[0],
            predicateParam: predicateParam,
            pageableParam : pageableParam]
}

codegen {
    typeAlias(
            'org.springframework.data.jpa.repository.JpaRepository',
            'org.springframework.data.jpa.repository.JpaSpecificationExecutor',
            'org.mapstruct.Mapper',
            'org.mapstruct.MappingTarget',
            'org.mapstruct.Mapping',
            'org.springframework.stereotype.Repository',
            'org.springframework.stereotype.Service',
            'org.springframework.transaction.annotation.Transactional',
            'org.springframework.beans.factory.annotation.Autowired',
            'org.springframework.web.bind.annotation.RestController',
            'org.springframework.web.bind.annotation.RequestMapping',
            'org.springframework.web.bind.annotation.RequestBody',
            'org.springframework.web.bind.annotation.RequestMethod',
            'org.springframework.web.bind.annotation.RequestParam',
            'org.springframework.web.bind.annotation.PathVariable',
            'javax.validation.Valid',
            'io.cruder.example.core.ApiReply',
            'io.cruder.example.core.BusinessErrors',
            'io.swagger.v3.oas.annotations.media.Schema',
    )

    services.each { serviceElement ->
        def theEntity = classOf(
                MoreElements.getAnnotationMirror(serviceElement, 'io.cruder.example.codegen.AutoService')
                        .get().elementValues.entrySet()
                        .find { it.key.simpleName.contentEquals('value') }
                        .value.value.asElement()
        )
        def theMapper = classOf("io.cruder.example.mapper.${theEntity.simpleName()}Mapper")
        def theRepository = classOf("io.cruder.example.repository.${theEntity.simpleName()}Repository")
        def theController = classOf("io.cruder.example.controller.${theEntity.simpleName()}Controller")
        def theService = classOf(serviceElement)
        def theServiceImpl = typeOf(serviceElement.qualifiedName.toString() + 'Impl')

        defInterface(theMapper, modifiers: 'public') {
            addAnnotation('Mapper', componentModel: 'spring')
        }

        defInterface(theRepository, modifiers: 'public', extends: [
                typeOf('JpaRepository', theEntity, 'Long'),
                typeOf('JpaSpecificationExecutor', theEntity),
        ]) {
            addAnnotation('Repository')
        }

        defClass(theController, modifiers: 'public') {
            addAnnotation('RestController')
            addField('service', type: theService, modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        defClass(theServiceImpl, modifiers: 'public', implements: typeOf(serviceElement)) {
            addAnnotation('Service')
            addField('converter', type: theMapper, modifiers: 'private') {
                addAnnotation('Autowired')
            }
            addField('repository', type: theRepository, modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        def mapperMethods = []
        ElementFilter.methodsIn(serviceElement.enclosedElements).each { method ->
            if (method.simpleName.startsWithAny('create')) {
                def theDTO = classOf(method.parameters[0].asType())
                def mapMethodName = "map${theDTO.simpleName()}ToEntity"
                def returnType = typeOf(method.returnType).box()

                if (!mapperMethods.contains(mapMethodName)) {
                    mapperMethods << mapMethodName
                    defInterface(theMapper) {
                        addMethod(mapMethodName, modifiers: 'public,abstract') {
                            addParameter('dto', type: theDTO)
                            addParameter('entity', type: theEntity) {
                                addAnnotation('MappingTarget')
                            }
                        }
                    }
                }
                defClass(theServiceImpl) {
                    addMethod(overrides: method) {
                        addAnnotation('Transactional')
                        addCode(code('''\
                                    $entityType:T entity = new $entityType:T();
                                    converter.$mapMethod:L(dto, entity);
                                    repository.save(entity);
                                    return entity.getId();
                                '''.stripIndent(),
                                entityType: theEntity,
                                mapMethod: mapMethodName
                        ))
                    }
                }
                defClass(theController) {
                    addMethod(method.simpleName, modifiers: 'public',
                            returns: typeOf('ApiReply', typeOf(method.returnType).box())) {
                        addAnnotation('RequestMapping',
                                method: code('$T.POST', typeOf('RequestMethod')),
                                path: "/api/${theEntity.simpleName().uncapitalize()}/${method.simpleName}")
                        addParameter('body', type: theDTO) {
                            addAnnotation('RequestBody')
                            addAnnotation('Valid')
                        }
                        if (returnType != typeOf('Void')) {
                            addCode(code('''\
                                        return $replyType:T.ok(service.$svcMethod:L(body));
                                    '''.stripIndent(),
                                    replyType: typeOf('ApiReply'), svcMethod: method.simpleName))
                        } else {
                            addCode(code('''\
                                        service.$svcMethod:L(body);
                                        return $replyType:T.ok();
                                    '''.stripIndent(),
                                    replyType: typeOf('ApiReply'), svcMethod: method.simpleName))
                        }
                    }
                }
            } else if (method.simpleName.startsWithAny('update')) {
                def theDTO = classOf(method.parameters[0].asType())
                def mapMethodName = "map${theDTO.simpleName()}ToEntity"
                def returnType = typeOf(method.returnType).box()

                if (!mapperMethods.contains(mapMethodName)) {
                    mapperMethods << mapMethodName
                    defInterface(theMapper) {
                        addMethod(mapMethodName, modifiers: 'public,abstract') {
                            addParameter('dto', type: theDTO)
                            addParameter('entity', type: theEntity) {
                                addAnnotation('MappingTarget')
                            }
                        }
                    }
                }
                defClass(theServiceImpl) {
                    addMethod(overrides: method) {
                        addAnnotation('Transactional')
                        addCode(code('''\
                                    $entityType:T entity = repository.findById(dto.getId())
                                        .orElseThrow(() -> $bizErrs:T.ENTITY_NOT_FOUND
                                            .withMessage("$entityName:L not found")
                                            .toException());
                                    converter.$mapMethod:L(dto, entity);
                                    repository.save(entity);
                                '''.stripIndent(),
                                entityType: theEntity,
                                entityName: theEntity.simpleName(),
                                mapMethod: mapMethodName,
                                bizErrs: classOf('BusinessErrors')
                        ))
                    }
                }
                defClass(theController) {
                    addMethod(method.simpleName, modifiers: 'public',
                            returns: typeOf('ApiReply',)) {
                        addAnnotation('RequestMapping',
                                method: code('$T.POST', typeOf('RequestMethod')),
                                path: "/api/${theEntity.simpleName().uncapitalize()}/${method.simpleName}")
                        addParameter('body', type: theDTO) {
                            addAnnotation('RequestBody')
                            addAnnotation('Valid')
                        }
                        if (returnType != typeOf('Void')) {
                            addCode(code('''\
                                        return $replyType:T.ok(service.$svcMethod:L(body));
                                    '''.stripIndent(),
                                    replyType: typeOf('ApiReply'), svcMethod: method.simpleName))
                        } else {
                            addCode(code('''\
                                        service.$svcMethod:L(body);
                                        return $replyType:T.ok();
                                    '''.stripIndent(),
                                    replyType: typeOf('ApiReply'), svcMethod: method.simpleName))
                        }
                    }
                }
            } else if (method.simpleName.startsWithAny('find')) {
                def paramType = typeOf(method.parameters[0].asType()).box()
                def returnType = classOf(method.returnType).box()
                if (paramType == typeOf('Long')) {
                    def mapMethodName = "mapEntityTo${returnType.simpleName()}"
                    if (!mapperMethods.contains(mapMethodName)) {
                        mapperMethods << mapMethodName
                        defInterface(theMapper) {
                            addMethod(mapMethodName, modifiers: 'public,abstract') {
                                addParameter('entity', type: theEntity)
                                addParameter('dto', type: returnType) {
                                    addAnnotation('MappingTarget')
                                }
                            }
                        }
                    }
                    defClass(theServiceImpl) {
                        addMethod(overrides: method) {
                            addCode(code('''\
                                        $entityType:T entity = repository.findById(id)
                                            .orElseThrow(() -> $bizErrs:T.ENTITY_NOT_FOUND
                                                .withMessage("$entityName:L not found")
                                                .toException());
                                        $dtoType:T dto = new $dtoType:T();
                                        converter.$mapMethod:L(entity, dto);
                                        return dto;
                                    '''.stripIndent(),
                                    entityType: theEntity,
                                    entityName: theEntity.simpleName(),
                                    dtoType: returnType,
                                    mapMethod: mapMethodName,
                                    bizErrs: classOf('BusinessErrors')
                            ))
                        }
                    }
                    defClass(theController) {
                        addMethod(method.simpleName, modifiers: 'public', returns: typeOf('ApiReply', returnType)) {
                            addAnnotation('RequestMapping',
                                    method: code('$T.POST', typeOf('RequestMethod')),
                                    path: "/api/${theEntity.simpleName().uncapitalize()}/${method.simpleName}/{id}")
                            addParameter('id', type: 'Long') {
                                addAnnotation('PathVariable', name: 'id')
                            }
                            addCode(code('''\
                                        return $replyType:T.ok(service.$svcMethod:L(id));
                                    '''.stripIndent(),
                                    replyType: typeOf('ApiReply'), svcMethod: method.simpleName))
                        }
                    }
                } else {
                    def result = matchFindPageMethod(method)
                    if (result.matches) {
                        def mapMethodName = "mapEntityTo${classOf(result.dtoType).simpleName()}"
                        if (!mapperMethods.contains(mapMethodName)) {
                            mapperMethods << mapMethodName
                            defInterface(theMapper) {
                                addMethod(mapMethodName, modifiers: 'public,abstract', returns: classOf(result.dtoType)) {
                                    addParameter('entity', type: theEntity)
                                }
                            }
                        }
                        defClass(theServiceImpl) {
                            addMethod(overrides: method) {
                                addCode(code('''\
                                        return repository.findAll($predicateParam:L, $pageableParam:L)
                                            .map(converter::$mapMethod:L);
                                    '''.stripIndent(),
                                        predicateParam: result.predicateParam.simpleName,
                                        pageableParam: result.pageableParam.simpleName,
                                        mapMethod: mapMethodName
                                ))
                            }
                        }
                    }
                }
            }
        }
    }
}
