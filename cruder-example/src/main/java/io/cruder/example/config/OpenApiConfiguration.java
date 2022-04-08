package io.cruder.example.config;

import static org.springdoc.core.SpringDocUtils.getConfig;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(title = "CRUDER Example"))
//@SecurityScheme( //
//        name = "user", //
//        type = SecuritySchemeType.HTTP, //
//        scheme = "Bearer", //
//        paramName = HttpHeaders.AUTHORIZATION, //
//        in = SecuritySchemeIn.HEADER, //
//        bearerFormat = "accessToken")
public class OpenApiConfiguration {
    static {
        getConfig().addRequestWrapperToIgnore(Pageable.class);
    }

    @Bean
    public GroupedOpenApi authorizedApi() {
        return GroupedOpenApi.builder()
                .group("All APIs")
                .pathsToMatch(new String[] {
                        "/api/**"
                })
//                .addOperationCustomizer((operation, handlerMethod) -> {
//                    operation.setSecurity(Arrays.asList(new SecurityRequirement().addList("user")));
//                    return operation;
//                })
                .build();
    }
}