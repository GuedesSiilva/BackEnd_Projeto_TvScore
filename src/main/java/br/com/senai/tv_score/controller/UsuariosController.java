package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.auth.AuthService;
import br.com.senai.tv_score.exceptions.ErroResponse;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<?> usuarioLogado(
            @RequestHeader(value = "Autorizado", required = false) String autorizacao
    ) {
        if (autorizacao == null || !autorizacao.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ErroResponse("Token não informado"));
        }
        String token = autorizacao.substring(7);

        if (!authService.tokenValido(token)) {
            return ResponseEntity.status(401)
                    .body(new ErroResponse("Token invalido."));
        }
        return ResponseEntity.ok(new UsuarioResponse(
                "Usuario teste",
                "teste@email.com"
        ));
    }
    public record UsuarioResponse(String nome, String email){}
}
