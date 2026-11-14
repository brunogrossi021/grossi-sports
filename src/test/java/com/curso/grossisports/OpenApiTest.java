package com.curso.grossisports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveDisponibilizarDocumentacaoOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.")))
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type")
                .value("http"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme")
                .value("bearer"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat")
                .value("JWT"))
            .andExpect(jsonPath("$.paths['/api/produtos']").exists())
            .andExpect(jsonPath("$.paths['/api/produtos/{id}']").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas'].get").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas'].post").exists())
            .andExpect(jsonPath("$.paths['/api/pessoas/{id}/status'].put")
                .exists())
            .andExpect(jsonPath("$.paths['/api/clientes'].get").exists())
            .andExpect(jsonPath("$.paths['/api/clientes'].post").exists())
            .andExpect(jsonPath("$.paths['/api/clientes/{id}'].put")
                .exists())
            .andExpect(jsonPath("$.paths['/api/colaboradores'].get")
                .exists())
            .andExpect(jsonPath("$.paths['/api/colaboradores'].post")
                .exists())
            .andExpect(jsonPath("$.paths['/api/vendas'].get").exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{id}/itens'].post")
                .exists())
            .andExpect(jsonPath("$.paths['/api/vendas/{id}/finalizacao'].post")
                .exists())
            .andExpect(jsonPath("$.components.schemas.VendaResponse")
                .exists())
            .andExpect(jsonPath("$.components.schemas.ClienteResponse")
                .exists())
            .andExpect(jsonPath("$.components.schemas.ColaboradorResponse")
                .exists())
            .andExpect(jsonPath("$.components.schemas.ContaAcessoResponse")
                .exists())
            .andExpect(jsonPath("$.paths['/api/contas-acesso'].post").exists())
            .andExpect(jsonPath("$.paths['/api/contas-acesso/{id}/perfis'].put")
                .exists())
            .andExpect(jsonPath("$.paths['/api/contas-acesso/{id}/status'].put")
                .exists())
            .andExpect(jsonPath("$.paths['/api/contas-acesso/me'].get")
                .exists())
            .andExpect(jsonPath("$.paths['/api/contas-acesso/me/senha'].put")
                .exists())
            .andExpect(jsonPath(
                "$.components.schemas.PerfisContaAcessoRequest").exists())
            .andExpect(jsonPath(
                "$.components.schemas.StatusContaAcessoRequest").exists())
            .andExpect(jsonPath("$.paths['/api/autenticacao/login'].post")
                .exists())
            .andExpect(jsonPath("$.components.schemas.LoginRequest").exists())
            .andExpect(jsonPath("$.components.schemas.TokenResponse").exists())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }
}
