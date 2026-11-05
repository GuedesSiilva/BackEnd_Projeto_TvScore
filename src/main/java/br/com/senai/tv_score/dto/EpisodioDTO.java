package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EpisodioDTO(
        Long id,

        String name,

        String overview,

        @JsonProperty("season_number")
        Integer temporada,

        @JsonProperty("episode_number")
        Integer numero,

        @JsonProperty("air_date")
        LocalDate dataExibicao,

        Integer runtime,

        @JsonProperty("still_path")
        String imagem,

        @JsonProperty("vote_average")
        Double nota,

        @JsonProperty("vote_count")
        Integer quantidadeVotos
) {
    public boolean possuiNota() {
        return quantidadeVotos != null
                && quantidadeVotos > 0
                && nota != null
                && Double.isFinite(nota);
    }
}