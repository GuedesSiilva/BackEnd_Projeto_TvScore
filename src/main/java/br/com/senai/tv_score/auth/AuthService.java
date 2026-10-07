package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.usuarios.Usuario;
import br.com.senai.tv_score.usuarios.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());

        if(usuarioOpt.isEmpty() || !usuarioOpt.get().getSenha().equals(request.senha())){
            return null;
        }

        Usuario usuario = usuarioOpt.get();

        String token = "token-user-" + usuario.getId();

        return new LoginResponse(
                token,
                usuario.getNome(),
                usuario.getEmail()
        );
    }

    public LoginResponse cadastrar(CadastroRequest request){
        if(usuarioRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        Usuario novoUsuario = new Usuario(
                request.nome(),
                request.email(),
                request.senha()
        );
        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);
        String token = "token-user-" + usuarioSalvo.getId();

        return new LoginResponse(
                token,
                usuarioSalvo.getNome(),
                usuarioSalvo.getEmail()
        );
    }
    public boolean tokenValido(String token) {
        return token != null && token.startsWith("token-user-");
    }

    public Usuario buscarPorToken(String token) {
        if (!tokenValido(token)) {
            return null;
        }
        try {
            Long id = Long.parseLong(token.replace("token-user-", ""));
            return usuarioRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
