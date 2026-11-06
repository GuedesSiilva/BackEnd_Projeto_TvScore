package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
import br.com.senai.tv_score.dto.AtualizarUsuarioDTO;
import br.com.senai.tv_score.usuarios.Usuario;
import br.com.senai.tv_score.usuarios.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final UsuarioService usuarioService;

    public UsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/eu")
    public UsuarioLogadoResponse usuarioLogado(
            @AuthenticationPrincipal Usuario usuario
    ) {
        return new UsuarioLogadoResponse(
                usuario.getNome(),
                usuario.getEmail()
        );
    }

    @PutMapping("/eu")
    public UsuarioLogadoResponse atualizarUsuario(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody AtualizarUsuarioDTO dto
    ) {
        return usuarioService.atualizar(usuario, dto);
    }
}