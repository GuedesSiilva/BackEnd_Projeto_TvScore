package br.com.senai.tv_score.comentario;

import br.com.senai.tv_score.filme.TipoConteudo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComentarioRepository
        extends JpaRepository<Comentario, Long> {

    @EntityGraph(attributePaths = "usuario")
    Page<Comentario> findByFilmeSerieIdAndTipoConteudo(
            Long filmeSerieId,
            TipoConteudo tipoConteudo,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "usuario")
    Page<Comentario> findByUsuario_Id(
            Long usuarioId,
            Pageable pageable
    );

    Optional<Comentario> findByIdAndUsuario_Id(
            Long id,
            Long usuarioId
    );
}