package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.PreferenciasGenerosRequestDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosResponseDTO;
import br.com.senai.tv_score.usuarios.PreferenciasUsuarioService;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/eu/generos")
public class PreferenciasUsuarioController {

    private final PreferenciasUsuarioService preferenciasService;

    public PreferenciasUsuarioController(
            PreferenciasUsuarioService preferenciasService
    ) {
        this.preferenciasService = preferenciasService;
    }

    @GetMapping
    public ResponseEntity<PreferenciasGenerosResponseDTO> consultar(
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
                preferenciasService.consultar(usuario)
        );
    }

    @PutMapping
    public ResponseEntity<PreferenciasGenerosResponseDTO> salvar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody PreferenciasGenerosRequestDTO dto
    ) {
        return ResponseEntity.ok(
                preferenciasService.salvar(usuario, dto)
        );
    }
}