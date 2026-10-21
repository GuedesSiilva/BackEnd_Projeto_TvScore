package br.com.senai.tv_score.favorito;

import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long>{

    List<Favorito> findByUsuario(Usuario usuario);

    boolean existsByUsuarioAndFilmeSerieIdAndTipoConteudo(
            Usuario usuario,
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    );

    void deleteByUsuarioAndFilmeSerieIdAndTipoConteudo(
            Usuario usuario,
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    );
}