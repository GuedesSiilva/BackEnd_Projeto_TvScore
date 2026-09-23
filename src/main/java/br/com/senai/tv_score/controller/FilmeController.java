package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.FilmeDTO;
import br.com.senai.tv_score.dto.TmdbResponseDTO;
import br.com.senai.tv_score.filme.FilmeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/filmes")
public class FilmeController {

    private final FilmeService filmeService;

    // Injeção de dependência do serviço
    public FilmeController(FilmeService filmeService) {
        this.filmeService = filmeService;
    }

    // Rota: GET /api/filmes/em-alta
    @GetMapping("/em-alta")
    public ResponseEntity<TmdbResponseDTO> getFilmesEmAlta() {
        TmdbResponseDTO response = filmeService.buscarFilmesEmAlta();
        return ResponseEntity.ok(response);
    }

    // Rota: GET /api/filmes/pesquisar?q=Batman
    @GetMapping("/pesquisar")
    public ResponseEntity<TmdbResponseDTO> pesquisarFilmes(@RequestParam("q") String query) {
        TmdbResponseDTO response = filmeService.pesquisarFilmes(query);
        return ResponseEntity.ok(response);
    }

    // Rota: GET /api/filmes/123
    @GetMapping("/{id}")
    public ResponseEntity<FilmeDTO> getDetalhesFilme(@PathVariable Long id) {
        FilmeDTO filme = filmeService.buscarDetalhesFilme(id);
        return ResponseEntity.ok(filme);
    }
}