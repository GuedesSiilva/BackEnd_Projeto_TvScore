package br.com.senai.tv_score.auth;

public record LoginResponse(
        String token,
        String nome,
        String email
) {}