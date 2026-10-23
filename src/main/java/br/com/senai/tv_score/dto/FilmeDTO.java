package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FilmeDTO(
        Long id,

        @JsonAlias("name")
        String title,

        String overview,

        @JsonProperty("vote_average")
        Double voteAverage,

        @JsonProperty("release_date")
        @JsonAlias("first_air_date")
        String releaseDate,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("backdrop_path")
        String backdropPath,

        @JsonProperty("genre_ids")
        List<Integer> genreIds,

        TipoConteudo tipoConteudo
) {
        public FilmeDTO comTipo(TipoConteudo tipo) {
        return new FilmeDTO(
                id,
                title,
                overview,
                voteAverage,
                releaseDate,
                posterPath,
                backdropPath,
                genreIds,
                tipo
        );
}}