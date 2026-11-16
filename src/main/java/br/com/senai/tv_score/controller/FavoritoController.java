package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.FavoritoDTO;
import br.com.senai.tv_score.exceptions.ErroResponse;
import br.com.senai.tv_score.favorito.FavoritoService;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final br.com.senai.tv_score.filme.ResumoConteudoService resumos;

    public FavoritoController(FavoritoService favoritoService, br.com.senai.tv_score.filme.ResumoConteudoService resumos) {
        this.favoritoService = favoritoService;
        this.resumos = resumos;
    }

    @GetMapping
    public ResponseEntity<List<FavoritoDTO>> listarFavoritos(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(favoritoService.listarFavoritos(usuario));
    }

    @PostMapping
    public ResponseEntity<FavoritoDTO> adicionarFavorito(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody FavoritoDTO dto
    ) {
        FavoritoDTO salvo = favoritoService.adicionarFavorito(usuario, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @DeleteMapping("/{filmeSerieId}")
    public ResponseEntity<Void> removerFavorito(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long filmeSerieId,
            @RequestParam("tipoConteudo") TipoConteudo tipoConteudo
    ) {
        favoritoService.removerFavorito(
                usuario,
                filmeSerieId,
                tipoConteudo
        );

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/paginados")
    public org.springframework.data.domain.Page<FavoritoDTO> paginados(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return resumos.favoritos(favoritoService.listarFavoritos(usuario, pagina, tamanho));
    }
}
