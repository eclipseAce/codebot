package scripts

import groovy.transform.BaseScript
import io.cruder.apt.CodegenScript

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@BaseScript
CodegenScript theScript

def entityAnnotationElement = processingEnv.elementUtils.getTypeElement('javax.persistence.Entity')
def dtoActionAnnotationElement = processingEnv.elementUtils.getTypeElement('io.cruder.example.codegen.DtoAction')

def entityAndDto = [:]
roundEnv.getElementsAnnotatedWith(entityAnnotationElement).each { element ->
    entityAndDto[element] = []
}
roundEnv.getElementsAnnotatedWith(dtoActionAnnotationElement).each { element ->
    def entityElement = element.annotationMirrors
            .find { it.annotationType.asElement() == dtoActionAnnotationElement }
            .elementValues.entrySet()
            .find { it.key.simpleName.contentEquals('value') }
            .value.value.asElement()
    entityAndDto[entityElement] << element
}

entityAndDto.each { entityElement, dtoElements ->
    codegen {
        def entityName = entityElement.simpleName.toString()
        typeRef(
                'org.springframework.data.jpa.repository.JpaRepository',
                'org.springframework.data.jpa.repository.JpaSpecificationExecutor',
                'org.mapstruct.Mapper',
                'org.mapstruct.MappingTarget',
                'org.springframework.stereotype.Repository',
                'org.springframework.stereotype.Service',
                'org.springframework.transaction.annotation.Transactional',
                'org.springframework.beans.factory.annotation.Autowired',
                'org.springframework.web.bind.annotation.RestController',
                'org.springframework.web.bind.annotation.RequestMapping',
                'org.springframework.web.bind.annotation.RequestBody',
                'org.springframework.web.bind.annotation.RequestMethod',
                'org.springframework.web.bind.annotation.RequestParam',
                'javax.validation.Valid',
                'io.cruder.example.core.ApiReply',
                'io.cruder.example.core.BusinessErrors',
                'io.swagger.v3.oas.annotations.media.Schema',

                theEntity: classOf(entityElement),
                theConverter: "io.cruder.example.converter.${entityName}Converter",
                theRepository: "io.cruder.example.repository.${entityName}Repository",
                theService: "io.cruder.example.service.${entityName}Service",
                theController: "io.cruder.example.controller.${entityName}Controller",
        )

        defInterface('theConverter', modifiers: 'public') {
            addAnnotation('Mapper', componentModel: 'spring')
        }

        defInterface('theRepository', modifiers: 'public', extends: [
                typeOf('JpaRepository', 'theEntity', 'Long'),
                typeOf('JpaSpecificationExecutor', 'theEntity'),
        ]) {
            addAnnotation('Repository')
        }

        defClass('theService', modifiers: 'public') {
            addAnnotation('Service')
            addField('repository', type: 'theRepository', modifiers: 'private') {
                addAnnotation('Autowired')
            }
            addField('converter', type: 'theConverter', modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        defClass('theController', modifiers: 'public') {
            addAnnotation('RestController')
            addField('service', type: 'theService', modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        dtoElements.each { dtoElement->
            def dtoName = dtoElement.simpleName.toString()
            def converterMethodName = "convert${dtoName}ToEntity"
            def controllerMethodName = dtoName.uncapitalize()
            def requestMappingPath = "/api/${entityName.uncapitalize()}/${dtoName.uncapitalize()}"
            defInterface('theConverter') {
                addMethod(converterMethodName, modifiers: 'public,abstract') {
                    addParameter('dto', type: dtoElement)
                    addParameter('entity', type: 'theEntity') {
                        addAnnotation('MappingTarget')
                    }
                }
            }
            defClass('theController') {
                addMethod(controllerMethodName, modifiers: 'public', returns: typeOf('ApiReply', 'Void')) {
                    addAnnotation('RequestMapping', method: code('$T.POST', typeOf('RequestMethod')), path: requestMappingPath)
                    addParameter('body', type: dtoElement) {
                        addAnnotation('RequestBody')
                        addAnnotation('Valid')
                    }
                    addStatement(code('return $T.ok()', typeOf('ApiReply')))
                }
            }
        }
    }
}