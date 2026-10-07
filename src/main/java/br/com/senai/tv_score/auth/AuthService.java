package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.security.JwtService;
import br.com.senai.tv_score.usuarios.Usuario;
import br.com.senai.tv_score.usuarios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());

        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(request.senha(), usuarioOpt.get().getSenha())) {
            return null;
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtService.gerarToken(usuario.getEmail());

        return new LoginResponse(token, usuario.getNome(), usuario.getEmail());
    }

    public LoginResponse cadastrar(CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        String senhaCriptografada = passwordEncoder.encode(request.senha());

        Usuario novoUsuario = new Usuario(
                request.nome(),
                request.email(),
                senhaCriptografada
        );

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);
        String token = jwtService.gerarToken(usuarioSalvo.getEmail());

        return new LoginResponse(token, usuarioSalvo.getNome(), usuarioSalvo.getEmail());
    }
}
