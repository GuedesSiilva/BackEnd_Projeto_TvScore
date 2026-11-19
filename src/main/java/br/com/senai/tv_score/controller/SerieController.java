package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;

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
    @Operation(summary = "Listar séries em alta")
    public ResponseEntity<TmdbResponseDTO> buscarEmAlta(
            @RequestParam(name = "pagina", defaultValue = "1")
            int pagina
    ) {
        return ResponseEntity.ok(
                filmeService.buscarSeriesEmAlta(pagina)
        );
    }


@GetMapping("/por-nota")
@Operation(summary = "Listar séries por nota")
public ResponseEntity<TmdbResponseDTO> buscarSeriesPorNota(
        @RequestParam(name = "minima", defaultValue = "0") double minima,
        @RequestParam(name = "maxima", defaultValue = "10") double maxima,
        @RequestParam(name = "pagina", defaultValue = "1") int pagina
) {
    return ResponseEntity.ok(
            filmeService.buscarSeriesPorNota(minima, maxima, pagina)
    );
}


    @GetMapping("/{id}")
    @Operation(summary = "Consultar os detalhes de uma série")
    public ResponseEntity<FilmeDTO> buscarDetalhes(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(
                filmeService.buscarDetalhesSerie(id)
        );
    }
}