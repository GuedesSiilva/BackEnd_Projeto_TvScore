package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecomendacoesDTO(
        Map<String, Double> preferencias,
        Integer total,
        List<Item> recomendacoes,
        Boolean parcial,
        List<String> avisos
) {
    public RecomendacoesDTO(Map<String, Double> preferencias, Integer total, List<Item> recomendacoes) {
        this(preferencias, total, recomendacoes, false, List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long id,
            String titulo,
            String sinopse,
            Double nota,
            List<Integer> generos,
            String poster,
            @JsonProperty("score_recomendacao") Double scoreRecomendacao,
            @JsonProperty("motivo_recomendacao") String motivoRecomendacao,
            @JsonProperty("similaridade_sinopse") Double similaridadeSinopse,
            TipoConteudo tipoConteudo,
            Integer ano
    ) {
        public Item(Long id, String titulo, String sinopse, Double nota, List<Integer> generos,
                    String poster, Double scoreRecomendacao, String motivoRecomendacao,
                    Double similaridadeSinopse) {
            this(id, titulo, sinopse, nota, generos, poster, scoreRecomendacao,
                    motivoRecomendacao, similaridadeSinopse, null, null);
        }
    }
}
