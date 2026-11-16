package br.com.senai.tv_score.usuarios;

public interface FotoStorage {
    void gravar(String chave, byte[] png);
    void apagar(String chave);
}
