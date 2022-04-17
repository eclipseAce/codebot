package scripts


import groovy.transform.BaseScript
import io.cruder.apt.PreCompileScript

@BaseScript PreCompileScript script

script.javaPoet {
    typeRef(
            'org.springframework.data.jpa.repository.JpaRepository',
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

            theEntity: typeOf(targetElement),
            theRepository: 'io.cruder.example.generated.repository.UserRepository',
            theConverter: 'io.cruder.example.generated.converter.UserConverter',
            theService: 'io.cruder.example.generated.service.UserService',
            theController: 'io.cruder.example.generated.controller.UserController',
    )

    defInterface('theRepository', modifiers: 'public', extends: [
            typeOf('JpaRepository', 'theEntity', 'Long')
    ]) {
        addAnnotation('Repository')
    }

    defInterface('theConverter', modifiers: 'public') {
        addAnnotation('Mapper', componentModel: 'spring')
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
}

