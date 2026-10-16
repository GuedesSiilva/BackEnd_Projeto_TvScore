package br.com.senai.tv_score.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StreamingDTO(
        @JsonProperty("provider_id") Long id,
        @JsonProperty("provider_name") String nome,
        @JsonProperty("logo_path") String logo
) {}