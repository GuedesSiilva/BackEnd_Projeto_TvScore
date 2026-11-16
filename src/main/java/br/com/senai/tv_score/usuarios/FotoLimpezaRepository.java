package br.com.senai.tv_score.usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface FotoLimpezaRepository extends JpaRepository<FotoLimpeza,String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select f from FotoLimpeza f where f.chave = :chave")
    java.util.Optional<FotoLimpeza> buscarComBloqueio(@org.springframework.data.repository.query.Param("chave") String chave);
    List<FotoLimpeza> findTop50ByDepoisDeBeforeOrderByDepoisDeAsc(Instant agora);
}
