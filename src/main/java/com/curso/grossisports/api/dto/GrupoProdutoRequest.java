package com.curso.grossisports.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoProdutoRequest(

    @NotBlank(message = "Nome do grupo é obrigatório")
    @Size(
        max = 120,
        message = "Nome do grupo deve possuir no máximo 120 caracteres")
    String nome

) {
}
