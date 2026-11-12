package br.com.senai.tv_score.filme;

import java.text.Normalizer;
import java.util.Locale;

public record GeneroConteudo(
        String codigo,
        String nome,
        Long generoFilmeId,
        Long generoSerieId
) {
    static String codigoDoNome(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
