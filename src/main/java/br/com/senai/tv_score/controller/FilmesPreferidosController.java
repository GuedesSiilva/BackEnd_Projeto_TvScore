package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.FilmesPreferidosRequestDTO;
import br.com.senai.tv_score.dto.FilmesPreferidosResponseDTO;
import br.com.senai.tv_score.usuarios.FilmesPreferidosService;
import br.com.senai.tv_score.usuarios.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/eu/filmes-preferidos")
public class FilmesPreferidosController {
    private final FilmesPreferidosService service;

    public FilmesPreferidosController(FilmesPreferidosService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta os filmes escolhidos na segunda etapa de preferências")
    public ResponseEntity<FilmesPreferidosResponseDTO> consultar(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar(usuario));
    }

    @PutMapping
    @Operation(summary = "Substitui a seleção de até cinco filmes e salva dados do TMDB para recomendação")
    public ResponseEntity<FilmesPreferidosResponseDTO> salvar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody FilmesPreferidosRequestDTO dto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.salvar(usuario, dto));
    }
}
