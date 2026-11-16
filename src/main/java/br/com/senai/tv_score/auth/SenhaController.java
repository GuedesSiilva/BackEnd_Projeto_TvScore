package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
public class SenhaController {
    private final SenhaService service;
    public SenhaController(SenhaService service) { this.service = service; }
    @PutMapping("/api/usuarios/eu/senha")
    @io.swagger.v3.oas.annotations.Operation(summary="Trocar senha e revogar sessões",description="204 sem corpo. Flutter deve apagar a sessão e pedir login. 400 validação; 403 senha atual incorreta; 401 sem sessão válida. Nova senha: 8–72 bytes UTF-8, sem trim.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterar(@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody AlterarSenhaRequest request) {
        service.alterar(usuario, request);
    }
}
