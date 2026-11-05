package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TemporadaDTO(
        Long id,

        String name,

        String overview,

        @JsonProperty("season_number")
        Integer numero,

        @JsonProperty("poster_path")
        String poster,

        List<EpisodioDTO> episodes
) {}