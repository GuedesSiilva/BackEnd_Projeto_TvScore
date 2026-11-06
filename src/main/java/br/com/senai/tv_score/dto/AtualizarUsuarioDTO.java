package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioDTO(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 60, message = "O nome deve ter até 60 caracteres.")
        String nome
) {}