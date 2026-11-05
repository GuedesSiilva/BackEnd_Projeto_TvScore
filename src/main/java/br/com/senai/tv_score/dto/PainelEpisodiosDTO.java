package br.com.senai.tv_score.dto;

import java.util.List;

public record PainelEpisodiosDTO(
        Long serieId,
        String fonteNotas,
        List<TemporadaDTO> temporadas,
        List<EpisodioDTO> topRated
) {}