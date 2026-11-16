package br.com.senai.tv_score.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController @RequestMapping("/api/auth/senha")
public class RecuperacaoController {
    public record Pedido(@NotBlank @Email @Size(max=254) String email) {}
    public record Redefinicao(@NotNull @Size(max=100) String token, @NotNull String novaSenha) {}
    private final RecuperacaoService service;
    public RecuperacaoController(RecuperacaoService service) { this.service = service; }
    @PostMapping("/recuperacao") @ResponseStatus(HttpStatus.ACCEPTED)
    @io.swagger.v3.oas.annotations.Operation(summary="Solicitar recuperação",description="202 idêntico para contas existentes/inexistentes; 429 com Retry-After. Envio assíncrono, sem token na resposta.")
    public Map<String,String> solicitar(@Valid @RequestBody Pedido pedido, HttpServletRequest request) {
        service.solicitar(pedido.email(), request.getRemoteAddr());
        return Map.of("mensagem", "Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação.");
    }
    @PostMapping("/redefinicao") @ResponseStatus(HttpStatus.NO_CONTENT)
    @io.swagger.v3.oas.annotations.Operation(summary="Redefinir senha uma única vez",description="204 sem autenticar. 400 para token inválido/expirado/consumido. Revoga todos os JWT anteriores. GET nunca consome token.")
    public void redefinir(@Valid @RequestBody Redefinicao pedido, HttpServletRequest request) {
        service.redefinir(pedido.token(), pedido.novaSenha(), request.getRemoteAddr());
    }
}
