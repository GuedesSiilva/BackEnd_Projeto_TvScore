package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FilmeDTO(
        Long id,
        String title,
        String overview, // sinopse
        @JsonProperty("vote_average") Double voteAverage, // nota média (RF06)
        @JsonProperty("release_date") String releaseDate, // data de lançamento (RF06)
        @JsonProperty("poster_path") String posterPath // imagem de capa (RF06)
) {}