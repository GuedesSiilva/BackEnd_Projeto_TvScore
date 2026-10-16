package br.com.senai.tv_score.dto;

import java.util.List;

public record GeneroResponseDTO(
        List<GeneroDTO> genres
) {}