package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecomendacoesDTO(
        Map<String, Double> preferencias,
        Integer total,
        List<Item> recomendacoes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long id,
            String titulo,
            String sinopse,
            Double nota,
            List<Integer> generos,
            String poster,

            @JsonProperty("score_recomendacao")
            Double scoreRecomendacao,

            @JsonProperty("motivo_recomendacao")
            String motivoRecomendacao,

            @JsonProperty("similaridade_sinopse")
            Double similaridadeSinopse
    ) {}
}