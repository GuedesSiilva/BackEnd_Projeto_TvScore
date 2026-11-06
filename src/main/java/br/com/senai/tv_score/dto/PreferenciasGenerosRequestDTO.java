package br.com.senai.tv_score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreferenciasGenerosRequestDTO(

        @NotNull(message = "A lista de gêneros é obrigatória.")
        @Size(
                max = 5,
                message = "Selecione no máximo cinco gêneros."
        )
        List<
                @NotBlank(message = "O código do gênero é obrigatório.")
                @Size(
                        max = 40,
                        message = "O código do gênero deve ter até 40 caracteres."
                )
                        String
                > generos

) {}