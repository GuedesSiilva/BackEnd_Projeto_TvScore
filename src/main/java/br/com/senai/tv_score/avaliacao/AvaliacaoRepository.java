package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByFilmeSerieIdAndTipoConteudo(
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    );

    List<Avaliacao> findByUsuario(Usuario usuario);

    Optional<Avaliacao> findByUsuarioAndFilmeSerieIdAndTipoConteudo(
            Usuario usuario,
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    );

    interface ResumoNotas {
        Double getMedia();
        Long getTotalAvaliacoes();
    }

    @Query("""
        SELECT AVG(a.nota) AS media,
               COUNT(a) AS totalAvaliacoes
        FROM Avaliacao a
        WHERE a.filmeSerieId = :filmeSerieId
          AND a.tipoConteudo = :tipoConteudo
        """)
    ResumoNotas buscarResumo(
            @Param("filmeSerieId") Long filmeSerieId,
            @Param("tipoConteudo") TipoConteudo tipoConteudo
    );
}
