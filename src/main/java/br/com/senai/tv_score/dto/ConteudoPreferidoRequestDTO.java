package br.com.senai.tv_score.dto;
import br.com.senai.tv_score.filme.TipoConteudo;
import jakarta.validation.constraints.*;
public record ConteudoPreferidoRequestDTO(@NotNull @Positive Long id, @NotNull TipoConteudo tipoConteudo) {}
