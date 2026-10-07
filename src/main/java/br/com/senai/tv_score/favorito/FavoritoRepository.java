package br.com.senai.tv_score.favorito;

import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByUsuario(Usuario usuario);

    boolean existsByUsuarioAndFilmeSerieId(Usuario usuario, Long filmeSerieId);

    void deleteByUsuarioAndFilmeSerieId(Usuario usuario, Long filmeSerieId);
}
