package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AvaliacaoDTO(
        Long id,

        @NotNull(message = "O ID do filme/série é obrigatório")
        @Positive(message = "O ID do conteúdo deve ser positivo.")
        Long filmeSerieId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 0, message = "A nota mínima é 0")
        @Max(value = 10, message = "A nota máxima é 10")
        Double nota,

        @Size(max = 1000, message = "O comentário deve ter até 1000 caracteres.")
        String comentario,

        String nomeUsuario,

        LocalDateTime dataCriacao,

        @NotNull(message = "O tipo do conteúdo é obrigatório")
        TipoConteudo tipoConteudo
) {
}
