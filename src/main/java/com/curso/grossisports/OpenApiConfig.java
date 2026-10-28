
package com.curso.grossisports;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI grossiSportsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Grossi Sports API")
                .version("1.0.0")
                .description(
                    "API para gerenciamento de produtos esportivos, " +
                        "grupos de produtos, fornecedores e movimentações de estoque."
                ));
    }
}
