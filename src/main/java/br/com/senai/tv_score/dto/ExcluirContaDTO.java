package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.NotBlank;

public record ExcluirContaDTO(
        @NotBlank(message = "Informe sua senha para excluir a conta.")
        String senha
) {}