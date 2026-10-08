package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public List<AvaliacaoDTO> listarPorFilme (Long filmeSerieId) {
        return avaliacaoRepository.findByFilmeSerieId(filmeSerieId)
                .stream()
                .map(a -> new AvaliacaoDTO(
                        a.getId(),
                        a.getFilmeSerieId(),
                        a.getNota(),
                        a.getComentario(),
                        a.getUsuario().getNome(),
                        a.getDataCriacao()
                ))
                .toList();
    }

    public List<AvaliacaoDTO> listarPorUsuario(Usuario usuario) {
        return avaliacaoRepository.findByUsuario(usuario)
                .stream()
                .map(a -> new AvaliacaoDTO(
                        a.getId(),
                        a.getFilmeSerieId(),
                        a.getNota(),
                        a.getComentario(),
                        a.getUsuario().getNome(),
                        a.getDataCriacao()
                ))
                .toList();
    }


    public AvaliacaoDTO avaliar(Usuario usuario, AvaliacaoDTO dto) {
        Optional<Avaliacao> avaliacaoExistente = avaliacaoRepository
                .findByUsuarioAndFilmeSerieId(usuario, dto.filmeSerieId());

        Avaliacao avaliacao;
        if (avaliacaoExistente.isPresent()) {
            avaliacao = avaliacaoExistente.get();
            avaliacao.setNota(dto.nota());
            avaliacao.setComentario(dto.comentario());
        } else {
            avaliacao = new Avaliacao(usuario, dto.filmeSerieId(), dto.nota(), dto.comentario());
        }

        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        return new AvaliacaoDTO(
                salva.getId(),
                salva.getFilmeSerieId(),
                salva.getNota(),
                salva.getComentario(),
                salva.getUsuario().getNome(),
                salva.getDataCriacao()
        );
    }

    public void removerAvaliacao(Usuario usuario, Long id) {
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada."));

        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para remover esta avaliação.");
        }

        avaliacaoRepository.delete(avaliacao);
    }
}
