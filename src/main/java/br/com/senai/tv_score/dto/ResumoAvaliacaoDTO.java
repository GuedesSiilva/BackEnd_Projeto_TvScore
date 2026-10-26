package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;

public record ResumoAvaliacaoDTO(
        Long filmeSerieId,
        TipoConteudo tipoConteudo,
        Double media,
        Long totalAvaliacoes
) {}