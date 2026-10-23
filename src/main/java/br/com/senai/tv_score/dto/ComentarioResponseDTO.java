package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;

import java.time.LocalDateTime;

public record ComentarioResponseDTO(
        Long id,
        Long filmeSerieId,
        TipoConteudo tipoConteudo,
        String texto,
        Long usuarioId,
        String nomeUsuario,
        LocalDateTime dataCriacao
) {}