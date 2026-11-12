package br.com.senai.tv_score.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FilmePreferidoRepository extends JpaRepository<FilmePreferido, Long> {
    List<FilmePreferido> findByUsuarioIdOrderByFilmeIdAsc(Long usuarioId);
    void deleteByUsuario(Usuario usuario);
}
