package scripts

import com.google.auto.common.MoreElements
import groovy.transform.BaseScript
import io.cruder.apt.CodegenScript

import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import java.lang.reflect.Modifier

@BaseScript
CodegenScript theScript

def services = roundEnv.getElementsAnnotatedWith(
        elementUtils.getTypeElement('io.cruder.example.codegen.AutoService')
).collect { it as TypeElement }

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
            'javax.validation.Valid',
            'io.cruder.example.core.ApiReply',
            'io.cruder.example.core.BusinessErrors',
            'io.swagger.v3.oas.annotations.media.Schema',
    )

    services.each { serviceElement ->
        def theEntity = typeOf(
                MoreElements.getAnnotationMirror(serviceElement, 'io.cruder.example.codegen.AutoService')
                        .get().elementValues.entrySet()
                        .find { it.key.simpleName.contentEquals('value') }
                        .value.value.asElement()
        )
        def theServiceImpl = typeOf(serviceElement.qualifiedName.toString() + 'Impl')
        def theConverter = typeOf(serviceElement.qualifiedName.toString() + 'Mapper')

        defInterface(theConverter, modifiers: 'public') {
            addAnnotation('Mapper', componentModel: 'spring')
        }

        defInterface(theRepository, modifiers: 'public', extends: [
                typeOf('JpaRepository', theEntity, 'Long'),
                typeOf('JpaSpecificationExecutor', theEntity),
        ]) {
            addAnnotation('Repository')
        }

        defClass(theServiceImpl, modifiers: 'public,abstract', implements: typeOf(serviceElement)) {
            addAnnotation('Service')
            addField('converter', type: theConverter, modifiers: 'private') {
                addAnnotation('Autowired')
            }
            addField('repository', type: theRepository, modifiers: 'private') {
                addAnnotation('Autowired')
            }
        }

        ElementFilter.methodsIn(serviceElement.enclosedElements)
                .findAll {
                    MoreElements.isAnnotationPresent(it, 'io.cruder.example.codegen.AutoService.Creating')
                }
                .each { method ->
                    def theDTO = ((DeclaredType) method.parameters[0].asType()).asElement()
                    def converterMethodName = "convert${theDTO.simpleName}ToEntity"
                    defInterface(theConverter) {
                        addMethod(converterMethodName, modifiers: 'public,abstract') {
                            addParameter('dto', type: dtoElement)
                            addParameter('entity', type: theEntity) {
                                addAnnotation('MappingTarget')
                            }
                        }
                    }
                    defClass(theServiceImpl) {
                        addMethod(overrides: method) {
                            addAnnotation('Transactional')
                            addCode(code(
                                    '''
                                    $entityType:T entity = new $entityType:T();
                                    converter.$convMethod:L(dto, entity);
                                    repository.save(entity);
                                    return entity.getId();
                                    '''.trim().stripIndent(),
                                    entityType: theEntity,
                                    convMethod: converterMethodName
                            ))
                        }
                    }
                }
    }
}
