package br.com.senai.tv_score.filme;



public enum GeneroConteudo {

    ANIMACAO("animacao", 16L, 16L),
    COMEDIA("comedia", 35L, 35L),
    CRIME("crime", 80L, 80L),
    DOCUMENTARIO("documentario", 99L, 99L),
    DRAMA("drama", 18L, 18L),
    FAMILIA("familia", 10751L, 10751L),
    MISTERIO("misterio", 9648L, 9648L),
    FAROESTE("faroeste", 37L, 37L);

    private final String codigo;
    private final Long generoFilmeId;
    private final Long generoSerieId;

    GeneroConteudo(
            String codigo,
            Long generoFilmeId,
            Long generoSerieId
    ) {
        this.codigo = codigo;
        this.generoFilmeId = generoFilmeId;
        this.generoSerieId = generoSerieId;
    }

    public String getCodigo() {
        return codigo;
    }

    public Long getGeneroFilmeId() {
        return generoFilmeId;
    }

    public Long getGeneroSerieId() {
        return generoSerieId;
    }

}
