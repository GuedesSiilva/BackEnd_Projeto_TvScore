package br.com.senai.tv_score.usuarios;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
@Entity @Table(name="foto_limpeza") @Getter @Setter @NoArgsConstructor
public class FotoLimpeza {
    @Id @Column(length=200) private String chave;
    @Column(nullable=false) private Instant depoisDe;
    public FotoLimpeza(String chave) { this.chave=chave; this.depoisDe=Instant.now().plusSeconds(3600); }
}
