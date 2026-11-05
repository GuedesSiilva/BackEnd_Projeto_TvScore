package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.EpisodioDTO;
import br.com.senai.tv_score.dto.PainelEpisodiosDTO;
import br.com.senai.tv_score.dto.TemporadaDTO;
import br.com.senai.tv_score.filme.EpisodioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/series")
public class EpisodioController {

    private final EpisodioService episodioService;

    public EpisodioController(EpisodioService episodioService) {
        this.episodioService = episodioService;
    }

    @GetMapping("/{serieId}/episodios/painel")
    public PainelEpisodiosDTO buscarPainel(
            @PathVariable("serieId") Long serieId,
            @RequestParam(name = "limite", defaultValue = "10")
            int limite,
            @RequestParam(name = "minimoVotos", defaultValue = "1")
            int minimoVotos,
            @RequestParam(name = "incluirEspeciais", defaultValue = "false")
            boolean incluirEspeciais
    ) {
        return episodioService.buscarPainel(
                serieId,
                limite,
                minimoVotos,
                incluirEspeciais
        );
    }

    @GetMapping("/{serieId}/temporadas/{temporada}")
    public TemporadaDTO buscarTemporada(
            @PathVariable("serieId") Long serieId,
            @PathVariable("temporada") int temporada
    ) {
        return episodioService.buscarTemporada(
                serieId,
                temporada
        );
    }

    @GetMapping("/{serieId}/temporadas/{temporada}/episodios/{episodio}")
    public EpisodioDTO buscarDetalhes(
            @PathVariable("serieId") Long serieId,
            @PathVariable("temporada") int temporada,
            @PathVariable("episodio") int episodio
    ) {
        return episodioService.buscarDetalhes(
                serieId,
                temporada,
                episodio
        );
    }
}