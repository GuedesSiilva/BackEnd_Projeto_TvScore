package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultadoPesquisaTmdbDTO(
        Long id,
        String title,
        String name,
        String overview,

        @JsonProperty("vote_average")
        Double voteAverage,

        @JsonProperty("release_date")
        String releaseDate,

        @JsonProperty("first_air_date")
        String firstAirDate,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("backdrop_path")
        String backdropPath,

        @JsonProperty("genre_ids")
        List<Integer> genreIds,

        @JsonProperty("media_type")
        String mediaType
) {

    public boolean ehFilmeOuSerie() {
        return "movie".equals(mediaType) || "tv".equals(mediaType);
    }

    public FilmeDTO paraFilmeDTO(Map<Integer, String> nomesPorId) {
        if (!ehFilmeOuSerie()) {
            throw new IllegalArgumentException(
                    "O resultado não representa filme ou série."
            );
        }

        boolean filme = "movie".equals(mediaType);

        List<String> nomesGeneros = genreIds == null
                ? List.of()
                : genreIds.stream()
                .filter(Objects::nonNull)
                .map(nomesPorId::get)
                .filter(Objects::nonNull)
                .toList();

        return new FilmeDTO(
                id,
                filme ? title : name,
                overview,
                voteAverage,
                filme ? releaseDate : firstAirDate,
                posterPath,
                backdropPath,
                null, // Omite genre_ids na resposta da pesquisa
                filme ? TipoConteudo.MOVIE : TipoConteudo.TV,
                null, // Onde assistir continua nos detalhes
                nomesGeneros
        );
    }
}