package br.com.senai.tv_score.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OndeAssistirDTO(
        String link,
        @JsonAlias("flatrate") List<StreamingDTO> assinatura,
        @JsonAlias("rent") List<StreamingDTO> aluguel,
        @JsonAlias("buy") List<StreamingDTO> compra,
        @JsonAlias("free") List<StreamingDTO> gratis,
        @JsonAlias("ads") List<StreamingDTO> comAnuncios
) {
    public OndeAssistirDTO {
        assinatura = ouVazia(assinatura);
        aluguel = ouVazia(aluguel);
        compra = ouVazia(compra);
        gratis = ouVazia(gratis);
        comAnuncios = ouVazia(comAnuncios);
    }

    private static List<StreamingDTO> ouVazia(List<StreamingDTO> lista) {
        return lista == null ? List.of() : lista;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespostaTmdb(Map<String, OndeAssistirDTO> results) {}
}