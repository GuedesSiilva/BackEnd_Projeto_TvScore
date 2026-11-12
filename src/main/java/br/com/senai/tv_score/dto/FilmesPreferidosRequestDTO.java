package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FilmesPreferidosRequestDTO(
        @NotNull(message = "A lista de filmes é obrigatória.")
        @Size(max = 5, message = "Selecione no máximo cinco filmes.")
        List<@NotNull @Positive(message = "O ID do filme deve ser positivo.") Long> filmes
) {}
