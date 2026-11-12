package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.GeneroConteudoDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosRequestDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosResponseDTO;
import br.com.senai.tv_score.filme.GeneroService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PreferenciasUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final GeneroService generoService;
    private final org.springframework.transaction.support.TransactionTemplate transacao;

    public PreferenciasUsuarioService(
            UsuarioRepository usuarioRepository,
            GeneroService generoService,
            org.springframework.transaction.PlatformTransactionManager manager
    ) {
        this.usuarioRepository = usuarioRepository;
        this.generoService = generoService;
        this.transacao = new org.springframework.transaction.support.TransactionTemplate(manager);
    }

    @Transactional(readOnly = true)
    public PreferenciasGenerosResponseDTO consultar(
            Usuario usuarioAutenticado
    ) {
        Long usuarioId = exigirUsuario(usuarioAutenticado);

        List<GeneroConteudoDTO> catalogo =
                generoService.listarGenerosDisponiveis().stream().map(g -> new GeneroConteudoDTO(g.codigo(), g.nome())).toList();

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(this::usuarioNaoEncontrado);

        return paraDTO(usuario, catalogo);
    }

    public PreferenciasGenerosResponseDTO salvar(
            Usuario usuarioAutenticado,
            PreferenciasGenerosRequestDTO dto
    ) {
        Long usuarioId = exigirUsuario(usuarioAutenticado);

        // Consulta e valida antes de bloquear/modificar o usuário.
        List<GeneroConteudoDTO> catalogo =
                generoService.listarGenerosDisponiveis().stream().map(g -> new GeneroConteudoDTO(g.codigo(), g.nome())).toList();

        List<String> codigos = dto.generos().stream()
                .map(codigo -> generoService.resolverGenero(codigo).codigo())
                .distinct()
                .toList();

        return transacao.execute(status -> {
        Usuario usuario = usuarioRepository
                .buscarPorIdComBloqueio(usuarioId)
                .orElseThrow(this::usuarioNaoEncontrado);

        usuario.getGenerosPreferidos().clear();
        usuario.getGenerosPreferidos().addAll(codigos);

        return paraDTO(usuario, catalogo);
        });
    }

    private PreferenciasGenerosResponseDTO paraDTO(
            Usuario usuario,
            List<GeneroConteudoDTO> catalogo
    ) {
        List<GeneroConteudoDTO> selecionados = usuario.getGenerosPreferidos().stream()
            .map(codigo -> catalogo.stream().filter(g -> g.codigo().equals(codigo) || GeneroService.codigoDoNome(g.nome()).equalsIgnoreCase(codigo)).findFirst()
                .map(g -> new GeneroConteudoDTO(codigo, g.nome())).orElse(new GeneroConteudoDTO(codigo, codigo))).toList();

        return new PreferenciasGenerosResponseDTO(selecionados);
    }

    private Long exigirUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        return usuario.getId();
    }

    private ResponseStatusException usuarioNaoEncontrado() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado."
        );
    }
}
