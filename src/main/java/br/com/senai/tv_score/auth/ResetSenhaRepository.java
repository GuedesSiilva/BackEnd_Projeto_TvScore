package br.com.senai.tv_score.auth;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface ResetSenhaRepository extends JpaRepository<ResetSenha, Long> {
    Optional<ResetSenha> findByHash(String hash);
    @Modifying @Query("update ResetSenha r set r.consumidoEm = :agora where r.hash = :hash and r.consumidoEm is null and r.expiraEm > :agora")
    int consumir(@Param("hash") String hash, @Param("agora") Instant agora);
    @Modifying @Query("delete from ResetSenha r where r.usuario.id = :id")
    void apagarDoUsuario(@Param("id") Long id);
    @Modifying @Query("delete from ResetSenha r where r.expiraEm < :agora")
    void apagarExpirados(@Param("agora") Instant agora);
}
