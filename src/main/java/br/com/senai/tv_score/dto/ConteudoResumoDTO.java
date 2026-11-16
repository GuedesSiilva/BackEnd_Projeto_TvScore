package br.com.senai.tv_score.dto;
import br.com.senai.tv_score.filme.TipoConteudo;
public record ConteudoResumoDTO(Long id, TipoConteudo tipoConteudo, String titulo, String posterPath,
        String dataLancamento, Double notaTmdb) {}
