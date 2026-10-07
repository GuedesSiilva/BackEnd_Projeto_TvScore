package br.com.senai.tv_score.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "Informe um e-mail válido")
        @Email(message = "E-mail em formato inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 4, message = "A senha deve ter pelo menos 4 caracteres")
        String senha
) {
}
