
package com.curso.grossisports;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BASIC = "basicAuth";

    @Bean
    public OpenAPI grossiSportsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Grossi Sports API")
                .version("1.0.0")
                .description(
                    "API para gerenciamento de produtos esportivos, " +
                        "grupos de produtos, fornecedores e movimentações de estoque."))
            .components(new Components()
                .addSecuritySchemes(
                    ESQUEMA_BASIC,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")))
            .addSecurityItem(
                new SecurityRequirement().addList(ESQUEMA_BASIC));
    }
}
