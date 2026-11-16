package br.com.senai.tv_score.auth;

public record LoginResponse(
        String token,
        String nome,
        String email,
        String fotoUrl
) {
    public LoginResponse(String token,String nome,String email) { this(token,nome,email,null); }
}
