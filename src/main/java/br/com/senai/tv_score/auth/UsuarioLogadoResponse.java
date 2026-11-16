package br.com.senai.tv_score.auth;

public record UsuarioLogadoResponse(
        String nome,
        String email,
        String fotoUrl
) {
    public UsuarioLogadoResponse(String nome, String email) { this(nome,email,null); }
}
