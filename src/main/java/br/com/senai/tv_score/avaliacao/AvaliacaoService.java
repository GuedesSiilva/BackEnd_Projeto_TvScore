package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                        dto.comentario(),
                        dto.tipoConteudo()
                ));

        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(dto.comentario());

        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        return paraDTO(salva);
    }

    @Transactional
    public void removerAvaliacao(Usuario usuario, Long id) {
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Avaliação não encontrada."
                        )
                );

        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException(
                    "Você não tem permissão para remover esta avaliação."
            );
        }

        avaliacaoRepository.delete(avaliacao);
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
}