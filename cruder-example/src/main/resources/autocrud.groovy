import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import groovy.transform.BaseScript
import io.cruder.apt.script.CrudBuilder
import io.cruder.apt.script.JavaBuilder
import io.cruder.apt.script.ProcessingScript
import org.codehaus.groovy.control.CompilerConfiguration

import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import java.lang.reflect.Modifier

@BaseScript
ProcessingScript script

class CrudDSL extends GroovyObjectSupport {
    GroovyShell shell

    CrudDSL() {
        def config = new CompilerConfiguration()
        config.scriptBaseClass = DelegatingScript.name
        shell = new GroovyShell(config)
    }

    @Override
    Object invokeMethod(String name, Object args) {
        println(name + ' invoked with ' + args)
        this
    }

    def run(Element element, String text) {
        def script = shell.parse(text) as DelegatingScript
        script.setDelegate(this)
        script.run()
    }
}

def dsl = new CrudDSL()

for (TypeElement e = targetElement;
     e.qualifiedName.toString() != 'java.lang.Object';
     e = processingEnv.typeUtils.asElement(e.superclass) as TypeElement) {
    def elements = [targetElement as Element]
    ElementFilter.fieldsIn(e.enclosedElements)
            .findAll { !it.modifiers.contains(Modifier.STATIC) }
            .each { elements.add(it) }
    elements.each { element ->
        element.annotationMirrors
                .collect { AnnotationSpec.get(it) }
                .findAll { it.type.toString() == 'io.cruder.example.core.CRUD' }
                .collectMany { it.members['value'] }
                .collect { it.toString().replaceAll('^"|"$', '') }
                .each { dsl.run(it) }
    }
}

JavaBuilder.build(processingEnv.filer) {
    def entityName = classOf(targetElement).simpleName()
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

            theEntity: classOf(targetElement),
            theRepository: "io.cruder.example.generated.repository.${entityName}Repository",
            theConverter: "io.cruder.example.generated.converter.${entityName}Converter",
            theService: "io.cruder.example.generated.service.${entityName}Service",
            theController: "io.cruder.example.generated.controller.${entityName}Controller",
    )

    defInterface('theRepository', modifiers: 'public', extends: [
            typeOf('JpaRepository', 'theEntity', 'Long'),
            typeOf('JpaSpecificationExecutor', 'theEntity'),
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
