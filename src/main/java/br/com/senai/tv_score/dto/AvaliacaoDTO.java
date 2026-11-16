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
        TipoConteudo tipoConteudo,
        @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
        String fotoUsuarioUrl,
        @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
        ConteudoResumoDTO conteudo,
        @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
        String estadoConteudo
) {
    public AvaliacaoDTO(Long id, Long filmeSerieId, Double nota, String comentario, String nomeUsuario,
            LocalDateTime dataCriacao, TipoConteudo tipoConteudo) {
        this(id,filmeSerieId,nota,comentario,nomeUsuario,dataCriacao,tipoConteudo,null,null,"NAO_SOLICITADO");
    }
    public AvaliacaoDTO(Long id, Long filmeSerieId, Double nota, String comentario, String nomeUsuario,
            LocalDateTime dataCriacao, TipoConteudo tipoConteudo,String fotoUsuarioUrl,ConteudoResumoDTO conteudo) {
        this(id,filmeSerieId,nota,comentario,nomeUsuario,dataCriacao,tipoConteudo,fotoUsuarioUrl,conteudo,"NAO_SOLICITADO");
    }
    public AvaliacaoDTO comConteudo(ConteudoResumoDTO resumo) {
        return new AvaliacaoDTO(id,filmeSerieId,nota,comentario,nomeUsuario,dataCriacao,tipoConteudo,fotoUsuarioUrl,resumo,resumo==null?"INDISPONIVEL":"DISPONIVEL");
    }
}
