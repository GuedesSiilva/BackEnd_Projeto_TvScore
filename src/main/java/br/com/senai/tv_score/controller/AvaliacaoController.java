package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;


import br.com.senai.tv_score.avaliacao.Avaliacao;
import br.com.senai.tv_score.avaliacao.AvaliacaoService;
import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.dto.ResumoAvaliacaoDTO;
import br.com.senai.tv_score.exceptions.ErroResponse;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.senai.tv_score.dto.AtualizarAvaliacaoDTO;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;
    private final br.com.senai.tv_score.filme.ResumoConteudoService resumos;

    public AvaliacaoController(AvaliacaoService avaliacaoService, br.com.senai.tv_score.filme.ResumoConteudoService resumos) {
        this.avaliacaoService = avaliacaoService;
        this.resumos = resumos;
    }

    @GetMapping("/filme/{filmeSerieId}")
    @Operation(summary = "Listar avaliações de um filme ou série")
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
    @Operation(summary = "Listar minhas avaliações")
    public ResponseEntity<List<AvaliacaoDTO>> listarMinhasAvaliacoes(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(avaliacaoService.listarPorUsuario(usuario));
    }

    @PostMapping
    @Operation(summary = "Registrar uma avaliação de filme ou série")
    public ResponseEntity<AvaliacaoDTO> avaliar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody AvaliacaoDTO dto
    ) {
        AvaliacaoDTO salva = avaliacaoService.avaliar(usuario, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar minha avaliação")
    public ResponseEntity<AvaliacaoDTO> atualizarAvaliacao(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable("id") Long id,
            @Valid @RequestBody AtualizarAvaliacaoDTO dto
    ) {
        return ResponseEntity.ok(
                avaliacaoService.atualizar(usuario, id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir minha avaliação")
    public ResponseEntity<Void> removerAvaliacao(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id
    ) {
        avaliacaoService.removerAvaliacao(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conteudo/{filmeSerieId}/resumo")
    @Operation(summary = "Consultar o resumo das avaliações de um título")
    public ResponseEntity<ResumoAvaliacaoDTO> buscarResumo(
            @PathVariable("filmeSerieId") Long filmeSerieId,
            @RequestParam("tipoConteudo") TipoConteudo tipoConteudo
    ) {
        return ResponseEntity.ok(
                avaliacaoService.buscarResumo(
                        filmeSerieId,
                        tipoConteudo
                )
        );
    }
    @GetMapping("/minhas/paginadas")
    @Operation(summary = "Listar minhas avaliações com paginação")
    public org.springframework.data.domain.Page<AvaliacaoDTO> paginados(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return resumos.avaliacoes(avaliacaoService.listarPorUsuario(usuario, pagina, tamanho));
    }
    @GetMapping("/filme/{filmeSerieId}/paginadas")
    @Operation(summary = "Listar avaliações de um título com paginação")
    public org.springframework.data.domain.Page<AvaliacaoDTO> porConteudoPaginado(@PathVariable Long filmeSerieId,
        @RequestParam TipoConteudo tipoConteudo, @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "20") int tamanho) {
        return resumos.avaliacoes(avaliacaoService.listarPorFilme(filmeSerieId, tipoConteudo, pagina, tamanho));
    }
}
