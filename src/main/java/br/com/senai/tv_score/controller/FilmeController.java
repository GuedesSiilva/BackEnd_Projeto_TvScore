package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.FilmeDTO;
import br.com.senai.tv_score.dto.TmdbResponseDTO;
import br.com.senai.tv_score.filme.FilmeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.com.senai.tv_score.dto.GeneroResponseDTO;
import br.com.senai.tv_score.dto.StreamingResponseDTO;

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
    public ResponseEntity<TmdbResponseDTO> pesquisarFilmes(
            @RequestParam("q") String query,
            @RequestParam(name = "pagina", defaultValue = "1") int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.pesquisarFilmes(query, pagina)
        );
    }

    // Rota: GET /api/filmes/123
    @GetMapping("/{id}")
    public ResponseEntity<FilmeDTO> getDetalhesFilme(@PathVariable Long id) {
        FilmeDTO filme = filmeService.buscarDetalhesFilme(id);
        return ResponseEntity.ok(filme);
    }

    // GET /api/filmes/generos
    @GetMapping("/generos")
    public ResponseEntity<GeneroResponseDTO> listarGeneros() {
        return ResponseEntity.ok(filmeService.listarGeneros());
    }

    // GET /api/filmes/generos/{generoId}?pagina=1
    @GetMapping("/generos/{generoId}")
    public ResponseEntity<TmdbResponseDTO> buscarFilmesPorGenero(
            @PathVariable("generoId") Long generoId,
            @RequestParam(name = "pagina", defaultValue = "1") int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarFilmesPorGenero(generoId, pagina)
        );
    }

    // GET /api/filmes/streamings
    @GetMapping("/streamings")
    public ResponseEntity<StreamingResponseDTO> listarStreamings() {
        return ResponseEntity.ok(
                filmeService.listarStreamings()
        );
    }

    // GET /api/filmes/streaming/{streamingId}?pagina=1
    @GetMapping("/streaming/{streamingId}")
    public ResponseEntity<TmdbResponseDTO> buscarPorStreaming(
            @PathVariable("streamingId") Long streamingId,
            @RequestParam(name = "pagina", defaultValue = "1") int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarFilmesPorStreaming(streamingId, pagina)
        );
    }

    // GET /api/filmes/por-nota?minima=7&maxima=10&pagina=1
    @GetMapping("/por-nota")
    public ResponseEntity<TmdbResponseDTO> buscarPorNota(
            @RequestParam(name = "minima", defaultValue = "0") double minima,
            @RequestParam(name = "maxima", defaultValue = "10") double maxima,
            @RequestParam(name = "pagina", defaultValue = "1") int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarFilmesPorNota(minima, maxima, pagina)
        );
    }

    // GET /api/filmes/por-ano/2024?pagina=1
    @GetMapping("/por-ano/{ano}")
    public ResponseEntity<TmdbResponseDTO> buscarPorAno(
            @PathVariable("ano") int ano,
            @RequestParam(name = "pagina", defaultValue = "1") int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarFilmesPorAno(ano, pagina)
        );
    }

    // GET /api/filmes/catalogo?pagina=1
    @GetMapping("/catalogo")
    public ResponseEntity<TmdbResponseDTO> buscarCatalogo(
            @RequestParam(name = "pagina", defaultValue = "1") int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarCatalogo(pagina)
        );
    }


}