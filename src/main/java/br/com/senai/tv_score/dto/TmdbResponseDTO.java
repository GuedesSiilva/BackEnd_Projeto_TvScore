package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TmdbResponseDTO(
        @JsonProperty("results") List<FilmeDTO> resultados,

        Integer page,

        @JsonProperty("total_pages") Integer totalPages,

        @JsonProperty("total_results") Integer totalResults
) {}