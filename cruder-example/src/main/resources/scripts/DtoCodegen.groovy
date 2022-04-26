package scripts

import groovy.transform.BaseScript
import io.cruder.apt.CodegenScript

@BaseScript
CodegenScript theScript

def entities = roundEnv.getElementsAnnotatedWith(typeElementOf('javax.persistence.Entity'))
        .collectEntries { element -> [element, [:]] }

['creating', 'updating'].each { action ->
    def anno = typeElementOf("io.cruder.example.codegen.${action.capitalize()}DTO")
    roundEnv.getElementsAnnotatedWith(anno).each { element ->
        def entityElement = element.annotationMirrors
                .find { it.annotationType.asElement() == anno }
                .elementValues.entrySet()
                .find { it.key.simpleName.contentEquals('value') }
                .value.value.asElement()
        entities[entityElement].computeIfAbsent(action, { [] }).add(element)
    }
}

codegen {
    typeAlias(
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
    )
    entities.each { entityElement, actions ->
        def entityName = entityElement.simpleName.toString()
        def theEntity = classOf(entityElement)
        def theConverter = classOf("io.cruder.example.converter.${entityName}Converter")
        def theRepository = classOf("io.cruder.example.repository.${entityName}Repository")
        def theService = classOf("io.cruder.example.service.${entityName}Service")
        def theController = classOf("io.cruder.example.controller.${entityName}Controller")

        defInterface(theConverter, modifiers: 'public') {
            addAnnotation('Mapper', componentModel: 'spring')
        }

        defInterface(theRepository, modifiers: 'public', extends: [
                typeOf('JpaRepository', theEntity, 'Long'),
                typeOf('JpaSpecificationExecutor', theEntity),
        ]) {
            addAnnotation('Repository')
        }

        defClass(theService, modifiers: 'public') {
            addAnnotation('Service')
            addField('repository', type: theRepository, modifiers: 'private') {
                addAnnotation('Autowired')
            }
            addField('converter', type: theConverter, modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        defClass(theController, modifiers: 'public') {
            addAnnotation('RestController')
            addField('service', type: theService, modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        actions.creating.each { dtoElement ->
            def dtoName = dtoElement.simpleName.toString()
            def converterMethodName = "convert${dtoName}ToEntity"
            def serviceMethodName = dtoName.uncapitalize()
            def controllerMethodName = dtoName.uncapitalize()
            def requestMappingPath = "/api/${entityName.uncapitalize()}/${dtoName.uncapitalize()}"
            defInterface(theConverter) {
                addMethod(converterMethodName, modifiers: 'public,abstract') {
                    addParameter('dto', type: dtoElement)
                    addParameter('entity', type: theEntity) {
                        addAnnotation('MappingTarget')
                    }
                }
            }
            defClass(theService) {
                addMethod(serviceMethodName, modifiers: 'public', returns: 'long') {
                    addAnnotation('Transactional')
                    addParameter('dto', type: dtoElement)
                    addCode(code('''\
                            $entityType:T entity = new $entityType:T();
                            converter.$convMethod:L(dto, entity);
                            repository.save(entity);
                            return entity.getId();
                            '''.stripIndent(),
                            entityType: theEntity, convMethod: converterMethodName))
                }
            }
            defClass(theController) {
                addMethod(controllerMethodName, modifiers: 'public', returns: typeOf('ApiReply', 'Long')) {
                    addAnnotation('RequestMapping', method: code('$T.POST', typeOf('RequestMethod')), path: requestMappingPath)
                    addParameter('body', type: dtoElement) {
                        addAnnotation('RequestBody')
                        addAnnotation('Valid')
                    }
                    addCode(code('''\
                            return $replyType:T.ok(service.$svcMethod:L(body));
                            '''.stripIndent(),
                            replyType: typeOf('ApiReply'), svcMethod: serviceMethodName))
                }
            }
        }
    }
}
