package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record FavoritoDTO(
        Long id,

        @NotNull(message = "O ID do filme/série é obrigatório")
        Long filmeSerieId,

        LocalDateTime dataAdicionado
) {}