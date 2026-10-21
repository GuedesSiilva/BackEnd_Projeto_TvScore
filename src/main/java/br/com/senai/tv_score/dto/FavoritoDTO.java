package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record FavoritoDTO(
        Long id,

        @NotNull(message = "O ID do filme/série é obrigatório")
        Long filmeSerieId,

        LocalDateTime dataAdicionado,

        @NotNull(message = "O tipo do conteúdo é obrigatório")
        TipoConteudo tipoConteudo
) {}