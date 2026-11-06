package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.GeneroConteudoDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosRequestDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosResponseDTO;
import br.com.senai.tv_score.filme.GeneroConteudo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class PreferenciasUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public PreferenciasUsuarioService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PreferenciasGenerosResponseDTO consultar(
            Usuario usuarioAutenticado
    ) {
        Long usuarioId = exigirUsuario(usuarioAutenticado);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(this::usuarioNaoEncontrado);

        return paraDTO(usuario);
    }

    @Transactional
    public PreferenciasGenerosResponseDTO salvar(
            Usuario usuarioAutenticado,
            PreferenciasGenerosRequestDTO dto
    ) {
        Long usuarioId = exigirUsuario(usuarioAutenticado);

        // Valida todos os códigos antes de modificar a seleção.
        List<String> codigos = dto.generos().stream()
                .map(GeneroConteudo::porCodigo)
                .map(GeneroConteudo::getCodigo)
                .distinct()
                .toList();

        Usuario usuario = usuarioRepository
                .buscarPorIdComBloqueio(usuarioId)
                .orElseThrow(this::usuarioNaoEncontrado);

        usuario.getGenerosPreferidos().clear();
        usuario.getGenerosPreferidos().addAll(codigos);

        // A entidade está gerenciada. A transação persiste a alteração.
        return paraDTO(usuario);
    }

    private PreferenciasGenerosResponseDTO paraDTO(Usuario usuario) {
        List<GeneroConteudoDTO> selecionados =
                Arrays.stream(GeneroConteudo.values())
                        .filter(genero ->
                                usuario.getGenerosPreferidos()
                                        .contains(genero.getCodigo())
                        )
                        .map(genero -> new GeneroConteudoDTO(
                                genero.getCodigo(),
                                genero.getNome()
                        ))
                        .toList();

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