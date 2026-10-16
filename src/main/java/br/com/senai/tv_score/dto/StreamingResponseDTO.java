package br.com.senai.tv_score.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StreamingResponseDTO(
        @JsonProperty("results") List<StreamingDTO> resultados
) {}