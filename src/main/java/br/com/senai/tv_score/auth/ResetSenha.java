package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "reset_senha") @Getter @Setter
public class ResetSenha {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Column(nullable = false, unique = true, length = 64) private String hash;
    @Column(nullable = false) private Instant criadoEm;
    @Column(nullable = false) private Instant expiraEm;
    private Instant consumidoEm;
}
