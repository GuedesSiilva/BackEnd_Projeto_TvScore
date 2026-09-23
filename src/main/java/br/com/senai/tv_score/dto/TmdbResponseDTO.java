package br.com.senai.tv_score.dto;

import java.util.List;

public record TmdbResponseDTO(
        int page,
        List<FilmeDTO> results,
        int total_pages,
        int total_results
) {}
