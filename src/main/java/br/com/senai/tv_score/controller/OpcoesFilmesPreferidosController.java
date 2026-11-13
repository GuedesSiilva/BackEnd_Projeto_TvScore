package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.TmdbResponseDTO;
import br.com.senai.tv_score.usuarios.OpcoesFilmesPreferidosService;
import br.com.senai.tv_score.usuarios.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/usuarios/eu/filmes-preferidos/opcoes", "/api/usuarios/eu/conteudos-preferidos/opcoes"})
public class OpcoesFilmesPreferidosController {
    private final OpcoesFilmesPreferidosService service;

    public OpcoesFilmesPreferidosController(OpcoesFilmesPreferidosService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Filmes e séries para a segunda etapa, relacionados aos gêneros escolhidos",
            description = "Intercala a página de cada tipo, até 40 itens; totais somados e máximo de páginas. Não é ordem global de popularidade.")
    public ResponseEntity<TmdbResponseDTO> consultar(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "1") int pagina) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.consultar(usuario, pagina));
    }
}
