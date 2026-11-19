package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;

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
    @Operation(summary = "Consultar o painel de episódios de uma série")
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
    @Operation(summary = "Consultar uma temporada e seus episódios")
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
    @Operation(summary = "Consultar os detalhes de um episódio")
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
    @Operation(summary = "Consultar a grade de episódios de uma série")
    public br.com.senai.tv_score.dto.GradeEpisodiosDTO grade(@PathVariable Long serieId,
            @RequestParam(defaultValue = "false") boolean incluirEspeciais) {
        var temporadas = episodioService.carregarTemporadas(serieId, incluirEspeciais);
        return new br.com.senai.tv_score.dto.GradeEpisodiosDTO(serieId,
            temporadas.stream().flatMap(t -> t.episodes().stream())
            .map(e -> new br.com.senai.tv_score.dto.GradeEpisodiosDTO.Celula(
                e.temporada(), e.numero(), e.nota(), e.quantidadeVotos(), e.dataExibicao())).toList());
    }
    @GetMapping("/{serieId}/episodios/ranking")
    @Operation(summary = "Consultar o ranking de episódios de uma série")
    public java.util.List<EpisodioDTO> ranking(@PathVariable Long serieId,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(defaultValue = "1") int minimoVotos,
            @RequestParam(defaultValue = "false") boolean incluirEspeciais) {
        return episodioService.buscarRanking(serieId, limite, minimoVotos, incluirEspeciais);
    }
}
