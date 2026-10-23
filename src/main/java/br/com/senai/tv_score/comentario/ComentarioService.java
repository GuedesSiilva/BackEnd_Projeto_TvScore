package br.com.senai.tv_score.comentario;

import br.com.senai.tv_score.dto.ComentarioRequestDTO;
import br.com.senai.tv_score.dto.ComentarioResponseDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;

    public ComentarioService(
            ComentarioRepository comentarioRepository
    ) {
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<ComentarioResponseDTO> listarPorConteudo(
            Long filmeSerieId,
            TipoConteudo tipoConteudo,
            int pagina,
            int tamanho
    ) {
        validarId(filmeSerieId);

        if (tipoConteudo == null) {
            throw new IllegalArgumentException(
                    "Informe o tipo do conteúdo."
            );
        }

        return comentarioRepository
                .findByFilmeSerieIdAndTipoConteudo(
                        filmeSerieId,
                        tipoConteudo,
                        criarPaginacao(pagina, tamanho)
                )
                .map(this::paraDTO);
    }

    @Transactional(readOnly = true)
    public Page<ComentarioResponseDTO> listarMeus(
            Usuario usuario,
            int pagina,
            int tamanho
    ) {
        exigirUsuario(usuario);

        return comentarioRepository
                .findByUsuario_Id(
                        usuario.getId(),
                        criarPaginacao(pagina, tamanho)
                )
                .map(this::paraDTO);
    }

    @Transactional
    public ComentarioResponseDTO publicar(
            Usuario usuario,
            ComentarioRequestDTO dto
    ) {
        exigirUsuario(usuario);

        Comentario comentario = new Comentario(
                usuario,
                dto.filmeSerieId(),
                dto.tipoConteudo(),
                dto.texto().strip()
        );

        Comentario salvo = comentarioRepository.save(comentario);

        return paraDTO(salvo);
    }

    @Transactional
    public ComentarioResponseDTO editar(
            Usuario usuario,
            Long id,
            ComentarioRequestDTO dto
    ) {
        Comentario comentario = buscarDoAutor(usuario, id);

        if (!comentario.getFilmeSerieId().equals(dto.filmeSerieId())
                || comentario.getTipoConteudo() != dto.tipoConteudo()) {
            throw new IllegalArgumentException(
                    "Não é permitido mudar o conteúdo de um comentário."
            );
        }

        comentario.setTexto(dto.texto().strip());

        // A entidade está gerenciada pela transação:
        // a alteração será persistida ao concluir o método.
        return paraDTO(comentario);
    }

    @Transactional
    public void excluir(Usuario usuario, Long id) {
        Comentario comentario = buscarDoAutor(usuario, id);

        comentarioRepository.delete(comentario);
    }

    private Comentario buscarDoAutor(Usuario usuario, Long id) {
        exigirUsuario(usuario);
        validarId(id);

        return comentarioRepository
                .findByIdAndUsuario_Id(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comentário não encontrado para este usuário."
                ));
    }

    private PageRequest criarPaginacao(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException(
                    "A página deve ser zero ou maior."
            );
        }

        if (tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve estar entre 1 e 100."
            );
        }

        Sort ordenacao = Sort.by(
                Sort.Order.desc("dataCriacao"),
                Sort.Order.desc("id")
        );

        return PageRequest.of(pagina, tamanho, ordenacao);
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "O ID deve ser positivo."
            );
        }
    }

    private void exigirUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }
    }

    private ComentarioResponseDTO paraDTO(Comentario comentario) {
        return new ComentarioResponseDTO(
                comentario.getId(),
                comentario.getFilmeSerieId(),
                comentario.getTipoConteudo(),
                comentario.getTexto(),
                comentario.getUsuario().getId(),
                comentario.getUsuario().getNome(),
                comentario.getDataCriacao()
        );
    }
}