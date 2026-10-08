package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByFilmeSerieId(Long filmeSerieId);

    List<Avaliacao> findByUsuario(Usuario usuario);

    Optional<Avaliacao> findByUsuarioAndFilmeSerieId(Usuario usuario, Long filmeSerieId);
}
