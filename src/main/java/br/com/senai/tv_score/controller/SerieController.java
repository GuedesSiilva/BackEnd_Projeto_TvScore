package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.FilmeDTO;
import br.com.senai.tv_score.dto.TmdbResponseDTO;
import br.com.senai.tv_score.filme.FilmeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/series")
public class SerieController {

    private final FilmeService filmeService;

    public SerieController(FilmeService filmeService) {
        this.filmeService = filmeService;
    }

    @GetMapping("/em-alta")
    public ResponseEntity<TmdbResponseDTO> buscarEmAlta(
            @RequestParam(name = "pagina", defaultValue = "1")
            int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarSeriesEmAlta(pagina)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<FilmeDTO> buscarDetalhes(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(
                filmeService.buscarDetalhesSerie(id)
        );
    }
}