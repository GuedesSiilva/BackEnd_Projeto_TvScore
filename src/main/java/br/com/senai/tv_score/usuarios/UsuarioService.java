package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
import br.com.senai.tv_score.avaliacao.AvaliacaoRepository;
import br.com.senai.tv_score.dto.AtualizarUsuarioDTO;
import br.com.senai.tv_score.dto.ExcluirContaDTO;
import br.com.senai.tv_score.favorito.FavoritoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final FavoritoRepository favoritoRepository;
    private final FilmePreferidoRepository filmePreferidoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            AvaliacaoRepository avaliacaoRepository,
            FavoritoRepository favoritoRepository,
            FilmePreferidoRepository filmePreferidoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.favoritoRepository = favoritoRepository;
        this.filmePreferidoRepository = filmePreferidoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioLogadoResponse atualizar(
            Usuario autenticado,
            AtualizarUsuarioDTO dto
    ) {
        Usuario usuario = usuarioRepository.findById(autenticado.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));

        usuario.setNome(Usuario.normalizarNome(dto.nome()));

        return new UsuarioLogadoResponse(
                usuario.getNome(),
                usuario.getEmail()
        );
    }

    @Transactional
    public void excluir(
            Usuario autenticado,
            ExcluirContaDTO dto
    ) {
        Usuario usuario = usuarioRepository
                .buscarPorIdComBloqueio(autenticado.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Senha incorreta. A conta não foi excluída."
            );
        }

        avaliacaoRepository.deleteByUsuario(usuario);
        favoritoRepository.deleteByUsuario(usuario);
        filmePreferidoRepository.deleteByUsuario(usuario);

        // Executa a exclusão dos registros vinculados antes da conta.
        avaliacaoRepository.flush();
        favoritoRepository.flush();
        filmePreferidoRepository.flush();

        // A coleção é gerenciada pelo JPA na tabela usuario_genero.
        usuario.getGenerosPreferidos().clear();
        usuarioRepository.flush();

        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
    }
}