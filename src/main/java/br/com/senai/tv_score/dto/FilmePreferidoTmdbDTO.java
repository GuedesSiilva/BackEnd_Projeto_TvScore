package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FilmePreferidoTmdbDTO(
        Long id,
        @com.fasterxml.jackson.annotation.JsonAlias("name") String title,
        String overview,
        List<GeneroDTO> genres,
        @JsonProperty("original_language") String originalLanguage,
        @JsonProperty("release_date") @com.fasterxml.jackson.annotation.JsonAlias("first_air_date") String releaseDate,
        @JsonProperty("vote_average") Double voteAverage,
        Double popularity,
        @JsonProperty("poster_path") String posterPath
) {}
