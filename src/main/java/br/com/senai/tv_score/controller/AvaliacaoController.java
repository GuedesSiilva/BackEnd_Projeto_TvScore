package br.com.senai.tv_score.controller;


import br.com.senai.tv_score.avaliacao.Avaliacao;
import br.com.senai.tv_score.avaliacao.AvaliacaoService;
import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.exceptions.ErroResponse;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping("/filme/{filmeSerieId}")
    public ResponseEntity<List<AvaliacaoDTO>> listarPorFilme(
            @PathVariable Long filmeSerieId,
            @RequestParam("tipoConteudo") TipoConteudo tipoConteudo
    ) {
        return ResponseEntity.ok(
                avaliacaoService.listarPorFilme(
                        filmeSerieId,
                        tipoConteudo
                )
        );
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<AvaliacaoDTO>> listarMinhasAvaliacoes(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(avaliacaoService.listarPorUsuario(usuario));
    }

    @PostMapping
    public ResponseEntity<AvaliacaoDTO> avaliar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody AvaliacaoDTO dto
    ) {
        AvaliacaoDTO salva = avaliacaoService.avaliar(usuario, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerAvaliacao(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id
    ) {
        avaliacaoService.removerAvaliacao(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
