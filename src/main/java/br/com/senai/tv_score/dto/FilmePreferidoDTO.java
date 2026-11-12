package br.com.senai.tv_score.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FilmePreferidoDTO(
        Long filmeId,
        String titulo,
        String sinopse,
        List<GeneroDTO> generos,
        String idiomaOriginal,
        String dataLancamento,
        Double notaTmdb,
        Double popularidade,
        String posterPath,
        LocalDateTime selecionadoEm
) {}
