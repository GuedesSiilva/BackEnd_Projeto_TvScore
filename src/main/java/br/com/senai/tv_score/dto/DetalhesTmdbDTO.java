package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DetalhesTmdbDTO(
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

        List<GeneroDTO> genres
) {

    public FilmeDTO paraFilmeDTO(TipoConteudo tipoConteudo) {
        List<Integer> idsGeneros = genres == null
                ? List.of()
                : genres.stream()
                .filter(Objects::nonNull)
                .map(GeneroDTO::id)
                .filter(Objects::nonNull)
                .map(idGenero -> Math.toIntExact(idGenero))
                .toList();

        return new FilmeDTO(
                id,
                title,
                overview,
                voteAverage,
                releaseDate,
                posterPath,
                backdropPath,
                idsGeneros,
                tipoConteudo
        );
    }
}