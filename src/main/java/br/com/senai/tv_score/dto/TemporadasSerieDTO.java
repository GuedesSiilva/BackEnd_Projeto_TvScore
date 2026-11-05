package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TemporadasSerieDTO(
        List<ResumoTemporada> seasons
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResumoTemporada(
            @JsonProperty("season_number")
            Integer numero
    ) {}
}