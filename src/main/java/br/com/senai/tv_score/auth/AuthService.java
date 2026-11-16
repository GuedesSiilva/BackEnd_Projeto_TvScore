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
    private final br.com.senai.tv_score.usuarios.FotoService fotos;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       br.com.senai.tv_score.usuarios.FotoService fotos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.fotos = fotos;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());

        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(request.senha(), usuarioOpt.get().getSenha())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtService.gerarToken(usuario.getId(), usuario.getVersaoCredenciais());

        return new LoginResponse(token, usuario.getNome(), usuario.getEmail(), fotos.url(usuario.getFotoChave()));
    }

    public LoginResponse cadastrar(CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        String senhaCriptografada = passwordEncoder.encode(request.senha());

        Usuario novoUsuario = new Usuario(
                Usuario.normalizarNome(request.nome()),
                request.email(),
                senhaCriptografada
        );

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);
        String token = jwtService.gerarToken(usuarioSalvo.getId());

        return new LoginResponse(token, usuarioSalvo.getNome(), usuarioSalvo.getEmail(), fotos.url(usuarioSalvo.getFotoChave()));
    }
}
