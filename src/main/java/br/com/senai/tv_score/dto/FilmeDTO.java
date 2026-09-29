package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.core.util.Json;

import java.util.List;

public record FilmeDTO(
        Long id,
        String title,
        String overview, // sinopse
        @JsonProperty("vote_average") Double voteAverage, // nota média
        @JsonProperty("release_date") String releaseDate, // data de lançamento
        @JsonProperty("poster_path") String posterPath, // imagem de capa
        @JsonProperty("genre_ids") List<Integer> genreIds // captura o id de genero de capa filme
) {}