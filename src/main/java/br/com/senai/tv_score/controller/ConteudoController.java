package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;

import br.com.senai.tv_score.dto.GeneroConteudoDTO;
import br.com.senai.tv_score.dto.TmdbResponseDTO;
import br.com.senai.tv_score.filme.FilmeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conteudos")
public class ConteudoController {

    private final FilmeService filmeService;

    public ConteudoController(FilmeService filmeService) {
        this.filmeService = filmeService;
    }

    @GetMapping("/generos")
    @Operation(summary = "Listar gêneros de filmes e séries")
    public ResponseEntity<List<GeneroConteudoDTO>> listarGeneros() {
        return ResponseEntity.ok(
                filmeService.listarGenerosConteudo()
        );
    }

    @GetMapping("/por-genero")
    @Operation(summary = "Buscar filmes e séries por gênero")
    public ResponseEntity<TmdbResponseDTO> buscarPorGenero(
            @RequestParam("genero") String genero,
            @RequestParam(name = "pagina", defaultValue = "1")
            int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarConteudosPorGenero(genero, pagina)
        );
    }
}