package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarAvaliacaoDTO(
        @NotNull(message = "A nota é obrigatória.")
        @DecimalMin(value = "0.0", message = "A nota mínima é 0.")
        @DecimalMax(value = "10.0", message = "A nota máxima é 10.")
        Double nota,

        @Size(
                max = 1000,
                message = "O comentário deve ter até 1000 caracteres."
        )
        String comentario
) {}