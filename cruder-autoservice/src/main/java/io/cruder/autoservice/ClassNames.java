package io.cruder.autoservice;

import com.squareup.javapoet.ClassName;

public interface ClassNames {

    ClassName List = ClassName.get("java.util", "List");

    interface Spring {
        ClassName Autowired = ClassName.get("org.springframework.beans.factory.annotation", "Autowired");
        ClassName Transactional = ClassName.get("org.springframework.transaction.annotation", "Transactional");
        ClassName Repository = ClassName.get("org.springframework.stereotype", "Repository");
        ClassName Service = ClassName.get("org.springframework.stereotype", "Service");
    }

    interface SpringWeb {
        ClassName ResponseEntity = ClassName.get("org.springframework.http", "ResponseEntity");
        ClassName RestController = ClassName.get("org.springframework.web.bind.annotation", "RestController");
        ClassName RequestBody = ClassName.get("org.springframework.web.bind.annotation", "RequestBody");
        ClassName RequestParam = ClassName.get("org.springframework.web.bind.annotation", "RequestParam");
        ClassName RequestMapping = ClassName.get("org.springframework.web.bind.annotation", "RequestMapping");
        ClassName RequestMethod = ClassName.get("org.springframework.web.bind.annotation", "RequestMethod");
    }

    interface SpringData {
        ClassName Page = ClassName.get("org.springframework.data.domain", "Page");
        ClassName Pageable = ClassName.get("org.springframework.data.domain", "Pageable");
        ClassName Specification = ClassName.get("org.springframework.data.jpa.domain", "Specification");
        ClassName JpaRepository = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
        ClassName JpaSpecificationExecutor = ClassName.get("org.springframework.data.jpa.repository", "JpaSpecificationExecutor");
    }

    interface MapStruct {
        ClassName Mappers = ClassName.get("org.mapstruct.factory", "Mappers");
        ClassName Mapper = ClassName.get("org.mapstruct", "Mapper");
        ClassName MappingTarget = ClassName.get("org.mapstruct", "MappingTarget");
    }

    interface JavaxValidation {
        ClassName Valid = ClassName.get("javax.validation", "Valid");
    }

    interface JavaxPersistence {
        ClassName Id = ClassName.get("javax.persistence", "Id");
        ClassName EntityNotFoundException = ClassName.get("javax.persistence", "EntityNotFoundException");
    }

}
