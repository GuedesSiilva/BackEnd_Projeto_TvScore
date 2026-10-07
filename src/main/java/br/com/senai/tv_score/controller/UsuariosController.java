package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.auth.AuthService;
import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
import br.com.senai.tv_score.exceptions.ErroResponse;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final AuthService authService;

    public UsuariosController(AuthService authService){
        this.authService = authService;
    }

    @GetMapping("/eu")
    public ResponseEntity<?> usuarioLogado(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErroResponse("Usuário não autenticado."));
        }

        return ResponseEntity.ok(new UsuarioLogadoResponse(
                usuario.getNome(),
                usuario.getEmail()
        ));
    }
}
