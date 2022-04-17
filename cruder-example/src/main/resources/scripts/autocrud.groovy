package scripts

import groovy.transform.BaseScript
import io.cruder.apt.PreCompileScript

@BaseScript PreCompileScript script

script.javaPoet {
    typeRef(
            'org.springframework.web.bind.annotation.RequestMapping',
            'java.io.Serializable',
            'java.lang.Cloneable',
            'theAddDTO': 'io.cruder.example.generated.dto.UserAddDTO'
    )

    defClass('theAddDTO', modifiers: 'public', implements: 'Serializable') {
        addField('id', type: typeOf('Long'), modifiers: 'public')
    }

    defClass('theAddDTO', implements: 'Cloneable') {
        addMethod('getId', modifiers: 'public', returns: 'long') {
            addAnnotation('RequestMapping', path: '/api/user/add')
            addStatement('return this.$id:L', id: 'id')
        }
    }
}

