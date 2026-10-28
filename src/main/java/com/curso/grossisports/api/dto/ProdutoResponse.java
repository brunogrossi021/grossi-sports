
package com.curso.grossisports.api.dto;

import com.curso.grossisports.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados de um produto esportivo retornados pela API")
public record ProdutoResponse(

    @Schema(description = "Identificador único do produto", example = "1")
    Long id,

    @Schema(description = "Código de barras do produto", example = "7891234567890")
    String codigoBarras,

    @Schema(description = "Descrição ou nome do produto", example = "Bola de futebol")
    String descricao,

    @Schema(description = "Quantidade atual disponível em estoque", example = "20")
    BigDecimal saldoEstoque,

    @Schema(description = "Preço unitário do produto", example = "149.90")
    BigDecimal valorUnitario,

    @Schema(description = "Quantidade mínima desejada em estoque", example = "5")
    BigDecimal estoqueMinimo,

    @Schema(description = "Valor total do estoque, calculado a partir do saldo e do preço unitário", example = "2998.00")
    BigDecimal valorEstoque,

    @Schema(description = "Data de cadastro do produto", example = "2026-10-22")
    LocalDate dataCadastro,

    @Schema(description = "Situação atual do produto", example = "ATIVO")
    Status status,

    @Schema(description = "Identificador do grupo do produto", example = "1")
    Long grupoId,

    @Schema(description = "Nome do grupo ao qual o produto pertence", example = "Bolas")
    String grupoNome,

    @Schema(description = "Identificador do fornecedor do produto", example = "1")
    Long fornecedorId,

    @Schema(description = "Razão social do fornecedor do produto", example = "Fornecedor Esportivo LTDA")
    String fornecedorRazaoSocial

) {
}
