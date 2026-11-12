package br.com.senai.tv_score.dto;
import java.util.List;
public record GradeEpisodiosDTO(Long serieId, List<Celula> episodios) {
    public record Celula(Integer temporada, Integer numero, Double nota, Integer votos, java.time.LocalDate data) {}
}
