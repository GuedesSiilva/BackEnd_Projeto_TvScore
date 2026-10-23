package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.comentario.ComentarioService;
import br.com.senai.tv_score.dto.ComentarioRequestDTO;
import br.com.senai.tv_score.dto.ComentarioResponseDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(
            ComentarioService comentarioService
    ) {
        this.comentarioService = comentarioService;
    }

    @GetMapping("/conteudo/{filmeSerieId}")
    public ResponseEntity<Page<ComentarioResponseDTO>> listarPorConteudo(
            @PathVariable("filmeSerieId") Long filmeSerieId,
            @RequestParam("tipoConteudo") TipoConteudo tipoConteudo,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "20") int tamanho
    ) {
        return ResponseEntity.ok(
                comentarioService.listarPorConteudo(
                        filmeSerieId,
                        tipoConteudo,
                        pagina,
                        tamanho
                )
        );
    }

    @GetMapping("/meus")
    public ResponseEntity<Page<ComentarioResponseDTO>> listarMeus(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "20") int tamanho
    ) {
        return ResponseEntity.ok(
                comentarioService.listarMeus(
                        usuario,
                        pagina,
                        tamanho
                )
        );
    }

    @PostMapping
    public ResponseEntity<ComentarioResponseDTO> publicar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody ComentarioRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.publicar(usuario, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComentarioResponseDTO> editar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable("id") Long id,
            @Valid @RequestBody ComentarioRequestDTO dto
    ) {
        return ResponseEntity.ok(
                comentarioService.editar(usuario, id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable("id") Long id
    ) {
        comentarioService.excluir(usuario, id);

        return ResponseEntity.noContent().build();
    }
}