package br.com.senai.tv_score.auth;

import jakarta.validation.constraints.NotNull;
public record AlterarSenhaRequest(@NotNull String senhaAtual, @NotNull String novaSenha) {}
