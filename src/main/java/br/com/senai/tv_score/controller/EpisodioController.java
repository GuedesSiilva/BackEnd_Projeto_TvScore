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

    @GetMapping("/{serieId}/episodios/grade")
    public br.com.senai.tv_score.dto.GradeEpisodiosDTO grade(@PathVariable Long serieId,
            @RequestParam(defaultValue = "false") boolean incluirEspeciais) {
        var painel = episodioService.buscarPainel(serieId, 10, 1, incluirEspeciais);
        return new br.com.senai.tv_score.dto.GradeEpisodiosDTO(serieId,
            painel.temporadas().stream().flatMap(t -> t.episodes().stream())
            .map(e -> new br.com.senai.tv_score.dto.GradeEpisodiosDTO.Celula(
                e.temporada(), e.numero(), e.nota(), e.quantidadeVotos(), e.dataExibicao())).toList());
    }
    @GetMapping("/{serieId}/episodios/ranking")
    public java.util.List<EpisodioDTO> ranking(@PathVariable Long serieId,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(defaultValue = "1") int minimoVotos,
            @RequestParam(defaultValue = "false") boolean incluirEspeciais) {
        return episodioService.buscarPainel(serieId, limite, minimoVotos, incluirEspeciais).topRated();
    }
}
