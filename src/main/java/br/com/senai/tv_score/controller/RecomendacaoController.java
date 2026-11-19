package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;

import br.com.senai.tv_score.dto.RecomendacoesDTO;
import br.com.senai.tv_score.recomendacao.RecomendacaoService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recomendacoes")
public class RecomendacaoController {

    private final RecomendacaoService recomendacaoService;

    public RecomendacaoController(
            RecomendacaoService recomendacaoService
    ) {
        this.recomendacaoService = recomendacaoService;
    }

    @GetMapping("/filmes")
    @Operation(summary = "Obter recomendações personalizadas de filmes")
    public ResponseEntity<RecomendacoesDTO> buscarFilmes(
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(recomendacaoService.buscarFilmes(authorization));
    }
    @GetMapping("/series")
    @Operation(summary = "Obter recomendações personalizadas de séries")
    public ResponseEntity<RecomendacoesDTO> buscarSeries(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recomendacaoService.buscarSeries(authorization));
    }
}
