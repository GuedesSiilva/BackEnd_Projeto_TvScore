package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.usuarios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SenhaService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final ResetSenhaRepository resets;
    public SenhaService(UsuarioRepository usuarios, PasswordEncoder encoder, ResetSenhaRepository resets) {
        this.usuarios = usuarios; this.encoder = encoder; this.resets=resets;
    }
    @Transactional
    public void alterar(Usuario autenticado, AlterarSenhaRequest request) {
        PoliticaSenha.validar(request.novaSenha());
        Usuario usuario = usuarios.buscarPorIdComBloqueio(autenticado.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != autenticado.getVersaoCredenciais()
                || !encoder.matches(request.senhaAtual(), usuario.getSenha()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não foi possível alterar a senha.");
        if (encoder.matches(request.novaSenha(), usuario.getSenha()))
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual.");
        usuario.setSenha(encoder.encode(request.novaSenha()));
        usuario.setVersaoCredenciais(Math.incrementExact(usuario.getVersaoCredenciais()));
        resets.apagarDoUsuario(usuario.getId());
    }
}
