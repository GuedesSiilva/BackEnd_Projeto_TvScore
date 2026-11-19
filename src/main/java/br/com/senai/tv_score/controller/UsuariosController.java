package br.com.senai.tv_score.controller;

import io.swagger.v3.oas.annotations.Operation;

import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
import br.com.senai.tv_score.dto.AtualizarUsuarioDTO;
import br.com.senai.tv_score.dto.ExcluirContaDTO;
import br.com.senai.tv_score.usuarios.Usuario;
import br.com.senai.tv_score.usuarios.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final UsuarioService usuarioService;
    private final br.com.senai.tv_score.usuarios.FotoService fotos;

    public UsuariosController(UsuarioService usuarioService, br.com.senai.tv_score.usuarios.FotoService fotos) {
        this.usuarioService = usuarioService;
        this.fotos = fotos;
    }

    @GetMapping("/eu")
    @Operation(summary = "Consultar meu perfil")
    public UsuarioLogadoResponse usuarioLogado(
            @AuthenticationPrincipal Usuario usuario
    ) {
        return new UsuarioLogadoResponse(
                usuario.getNome(),
                usuario.getEmail(), fotos.url(usuario.getFotoChave())
        );
    }

    @PutMapping("/eu")
    @Operation(summary = "Atualizar meu perfil")
    public UsuarioLogadoResponse atualizarUsuario(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody AtualizarUsuarioDTO dto
    ) {
        return usuarioService.atualizar(usuario, dto);
    }

    @DeleteMapping("/eu")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir minha conta")
    public void excluirConta(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody ExcluirContaDTO dto
    ) {
        usuarioService.excluir(usuario, dto);
    }
}
