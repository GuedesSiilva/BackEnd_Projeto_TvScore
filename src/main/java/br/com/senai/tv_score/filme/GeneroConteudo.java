package br.com.senai.tv_score.filme;

import java.util.Arrays;

public enum GeneroConteudo {

    ANIMACAO("animacao", "Animação", 16L, 16L),
    COMEDIA("comedia", "Comédia", 35L, 35L),
    CRIME("crime", "Crime", 80L, 80L),
    DOCUMENTARIO("documentario", "Documentário", 99L, 99L),
    DRAMA("drama", "Drama", 18L, 18L),
    FAMILIA("familia", "Família", 10751L, 10751L),
    MISTERIO("misterio", "Mistério", 9648L, 9648L),
    FAROESTE("faroeste", "Faroeste", 37L, 37L);

    private final String codigo;
    private final String nome;
    private final Long generoFilmeId;
    private final Long generoSerieId;

    GeneroConteudo(
            String codigo,
            String nome,
            Long generoFilmeId,
            Long generoSerieId
    ) {
        this.codigo = codigo;
        this.nome = nome;
        this.generoFilmeId = generoFilmeId;
        this.generoSerieId = generoSerieId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public Long getGeneroFilmeId() {
        return generoFilmeId;
    }

    public Long getGeneroSerieId() {
        return generoSerieId;
    }

    public static GeneroConteudo porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe um gênero."
            );
        }

        return Arrays.stream(values())
                .filter(genero ->
                        genero.codigo.equalsIgnoreCase(codigo.trim())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Gênero inválido. Consulte /api/conteudos/generos."
                        )
                );
    }
}