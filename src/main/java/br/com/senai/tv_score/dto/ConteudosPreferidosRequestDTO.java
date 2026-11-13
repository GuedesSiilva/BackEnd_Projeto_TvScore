package br.com.senai.tv_score.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
@io.swagger.v3.oas.annotations.media.Schema(example = "{\"conteudos\":[{\"id\":550,\"tipoConteudo\":\"MOVIE\"},{\"id\":1399,\"tipoConteudo\":\"TV\"}]}")
public record ConteudosPreferidosRequestDTO(@NotNull @Size(max = 5) List<@NotNull @Valid ConteudoPreferidoRequestDTO> conteudos) {}
