package br.com.senai.tv_score.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe um e-mail válido")
        String email,
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {}