package br.com.senai.tv_score.auth;

import java.nio.charset.StandardCharsets;

public final class PoliticaSenha {
    private PoliticaSenha() {}
    public static void validar(String senha) {
        int bytes = senha == null ? 0 : senha.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 8 || bytes > 72) throw new IllegalArgumentException("A nova senha deve ter entre 8 e 72 bytes UTF-8.");
    }
}
