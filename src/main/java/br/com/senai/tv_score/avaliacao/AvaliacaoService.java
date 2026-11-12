package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.dto.AtualizarAvaliacaoDTO;
import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.dto.ResumoAvaliacaoDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarPorFilme(
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    ) {
        return avaliacaoRepository
                .findByFilmeSerieIdAndTipoConteudo(
                        filmeSerieId,
                        tipoConteudo
                )
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarPorUsuario(Usuario usuario) {
        return avaliacaoRepository.findByUsuario(usuario)
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional
    public AvaliacaoDTO avaliar(Usuario usuario, AvaliacaoDTO dto) {
        validarConteudo(dto.filmeSerieId(), dto.tipoConteudo());
        String comentario = validarEdicao(dto.nota(), dto.comentario());
        Avaliacao avaliacao = avaliacaoRepository
                .findByUsuarioAndFilmeSerieIdAndTipoConteudo(
                        usuario,
                        dto.filmeSerieId(),
                        dto.tipoConteudo()
                )
                .orElseGet(() -> new Avaliacao(
                        usuario,
                        dto.filmeSerieId(),
                        dto.nota(),
                        comentario,
                        dto.tipoConteudo()
                ));

        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(comentario);

        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        return paraDTO(salva);
    }

    @Transactional
    public void removerAvaliacao(Usuario usuario, Long id) {
        avaliacaoRepository.delete(buscarDoUsuario(usuario, id));
    }

    private AvaliacaoDTO paraDTO(Avaliacao avaliacao) {
        return new AvaliacaoDTO(
                avaliacao.getId(),
                avaliacao.getFilmeSerieId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getUsuario().getNome(),
                avaliacao.getDataCriacao(),
                avaliacao.getTipoConteudo()
        );
    }

    @Transactional(readOnly = true)
    public ResumoAvaliacaoDTO buscarResumo(
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    ) {
        validarConteudo(filmeSerieId, tipoConteudo);

        AvaliacaoRepository.ResumoNotas resumo =
                avaliacaoRepository.buscarResumo(
                        filmeSerieId,
                        tipoConteudo
                );

        return new ResumoAvaliacaoDTO(
                filmeSerieId,
                tipoConteudo,
                resumo.getMedia(),
                resumo.getTotalAvaliacoes()
        );
    }
    @Transactional
    public AvaliacaoDTO atualizar(
            Usuario usuario,
            Long id,
            AtualizarAvaliacaoDTO dto
    ) {
        String comentario = validarEdicao(dto.nota(), dto.comentario());
        Avaliacao avaliacao = buscarDoUsuario(usuario, id);
        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(comentario);
        return paraDTO(avaliacao);
    }

    private Avaliacao buscarDoUsuario(Usuario usuario, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("O ID da avaliação deve ser positivo.");
        }
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Avaliação não encontrada."
                ));
        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não tem permissão para alterar esta avaliação."
            );
        }
        return avaliacao;
    }

    private static void validarConteudo(Long id, TipoConteudo tipo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("O ID do conteúdo deve ser positivo.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo do conteúdo é obrigatório.");
        }
    }

    private static String validarEdicao(Double nota, String comentario) {
        if (nota == null || !Double.isFinite(nota) || nota < 0 || nota > 10) {
            throw new IllegalArgumentException("Informe uma nota entre 0 e 10.");
        }
        if (comentario != null && comentario.length() > 1000) {
            throw new IllegalArgumentException("O comentário deve ter até 1000 caracteres.");
        }
        return comentario == null ? null : comentario.strip();
    }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AvaliacaoDTO> listarPorUsuario(Usuario usuario, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) throw new IllegalArgumentException("P?gina inv?lida; tamanho entre 1 e 100.");
        return avaliacaoRepository.findByUsuario(usuario, org.springframework.data.domain.PageRequest.of(
            pagina, tamanho, org.springframework.data.domain.Sort.by("id").ascending())).map(this::paraDTO);
    }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AvaliacaoDTO> listarPorFilme(Long id, TipoConteudo tipo, int pagina, int tamanho) {
        validarConteudo(id, tipo);
        if (pagina < 0 || tamanho < 1 || tamanho > 100) throw new IllegalArgumentException("Pagina invalida.");
        return avaliacaoRepository.findByFilmeSerieIdAndTipoConteudo(id, tipo,
            org.springframework.data.domain.PageRequest.of(pagina, tamanho,
                org.springframework.data.domain.Sort.by("id").ascending())).map(this::paraDTO);
    }
}
