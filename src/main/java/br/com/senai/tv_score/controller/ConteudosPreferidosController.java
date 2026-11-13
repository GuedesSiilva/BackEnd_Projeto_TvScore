package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.ConteudosPreferidosRequestDTO;
import br.com.senai.tv_score.dto.ConteudosPreferidosResponseDTO;
import br.com.senai.tv_score.usuarios.FilmesPreferidosService;
import br.com.senai.tv_score.usuarios.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/eu/conteudos-preferidos")
public class ConteudosPreferidosController {
    private final FilmesPreferidosService service;

    public ConteudosPreferidosController(FilmesPreferidosService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta filmes e séries selecionados")
    public ResponseEntity<ConteudosPreferidosResponseDTO> consultar(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultarConteudos(usuario));
    }

    @PutMapping
    @Operation(summary = "Substitui a seleção completa de até cinco filmes e séries")
    public ResponseEntity<ConteudosPreferidosResponseDTO> salvar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody ConteudosPreferidosRequestDTO dto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.salvarConteudos(usuario, dto));
    }
}
