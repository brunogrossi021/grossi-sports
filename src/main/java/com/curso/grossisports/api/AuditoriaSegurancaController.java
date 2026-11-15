package com.curso.grossisports.api;

import com.curso.grossisports.api.dto.ApiError;
import com.curso.grossisports.api.dto.EventoSegurancaResponse;
import com.curso.grossisports.api.dto.PageResponse;
import com.curso.grossisports.api.mapper.EventoSegurancaMapper;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import com.curso.grossisports.service.AuditoriaSegurancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta administrativa da trilha de segurança (Aula 17).
 *
 * <p>Somente leitura: não existe endpoint para criar, alterar ou excluir
 * eventos. A autorização é exigida em duas camadas — a política HTTP
 * ({@code hasRole('ADMINISTRADOR')}) e o {@code @PreAuthorize} do
 * serviço.</p>
 */
@RestController
@RequestMapping("/api/auditoria-seguranca")
@Tag(
    name = "Auditoria de segurança",
    description = "Consulta de eventos de segurança (ADMINISTRADOR)"
)
public class AuditoriaSegurancaController {

    private final AuditoriaSegurancaService service;
    private final EventoSegurancaMapper mapper;

    public AuditoriaSegurancaController(
        AuditoriaSegurancaService service,
        EventoSegurancaMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Consultar eventos de segurança",
        description = "Retorna a trilha de auditoria em ordem cronológica "
            + "decrescente, com paginação e filtro opcional por tipo de "
            + "evento. Restrito a ADMINISTRADOR. O DTO não expõe o hash de "
            + "correlação."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Página de eventos retornada"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros de consulta inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Sem token válido",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sem perfil ADMINISTRADOR",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public PageResponse<EventoSegurancaResponse> consultar(
        @Parameter(description = "Filtro opcional por tipo de evento")
        @RequestParam(required = false)
        TipoEventoSeguranca tipo,

        @Parameter(description = "Número da página (base zero)")
        @RequestParam(defaultValue = "0")
        int page,

        @Parameter(description = "Tamanho da página (1 a 100)")
        @RequestParam(defaultValue = "20")
        int size) {

        return mapper.toPage(service.consultar(tipo, page, size));
    }
}
