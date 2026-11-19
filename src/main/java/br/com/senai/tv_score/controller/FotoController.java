package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.senai.tv_score.usuarios.*;
import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/usuarios/eu/foto")
public class FotoController {
    private final FotoService fotos;
    public FotoController(FotoService fotos) { this.fotos=fotos; }
    @PutMapping(consumes="multipart/form-data")
    @io.swagger.v3.oas.annotations.Operation(summary="Enviar foto JPEG/PNG",description="Campo arquivo, até 5 MiB. 200 perfil com fotoUrl; 400 imagem inválida; 413 tamanho; 415 formato; 503 storage. URL pública, escrita restrita ao usuário autenticado.")
    public UsuarioLogadoResponse atualizar(@AuthenticationPrincipal Usuario usuario, @RequestPart("arquivo") MultipartFile arquivo) {
        return new UsuarioLogadoResponse(usuario.getNome(),usuario.getEmail(),fotos.atualizar(usuario,arquivo));
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover minha foto de perfil")
    public void remover(@AuthenticationPrincipal Usuario usuario) { fotos.remover(usuario); }
}
