package br.com.senai.tv_score.favorito;

import br.com.senai.tv_score.dto.FavoritoDTO;
import br.com.senai.tv_score.exceptions.ErroResponse;
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

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
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
            @PathVariable Long filmeSerieId
    ) {
        favoritoService.removerFavorito(usuario, filmeSerieId);
        return ResponseEntity.noContent().build();
    }
}