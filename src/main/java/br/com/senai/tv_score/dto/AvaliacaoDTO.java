package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AvaliacaoDTO(
        Long id,

        @NotNull(message = "O ID do filme/série é obrigatório")
        Long filmeSerieId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 0, message = "A nota mínima é 0")
        @Max(value = 10, message = "A nota máxima é 10")
        Double nota,

        String comentario,

        String nomeUsuario,

        LocalDateTime dataCriacao
) {
}
