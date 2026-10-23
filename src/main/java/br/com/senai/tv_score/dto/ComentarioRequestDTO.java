package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ComentarioRequestDTO(

        @NotNull(message = "O ID do conteúdo é obrigatório.")
        @Positive(message = "O ID do conteúdo deve ser positivo.")
        Long filmeSerieId,

        @NotNull(message = "O tipo do conteúdo é obrigatório.")
        TipoConteudo tipoConteudo,

        @NotBlank(message = "O comentário não pode estar vazio.")
        @Size(
                max = 4000,
                message = "O comentário deve ter até 4000 caracteres."
        )
        String texto

) {}