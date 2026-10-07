package br.com.senai.tv_score.favorito;

import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorito")
@Getter
@Setter
@NoArgsConstructor
public class Favorito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "filme_serie_id", nullable = false)
    private Long filmeSerieId;

    @CreationTimestamp
    @Column(name = "data_adicionado", updatable = false)
    private LocalDateTime dataAdicionado;

    public Favorito(Usuario usuario, Long filmeSerieId) {
        this.usuario = usuario;
        this.filmeSerieId = filmeSerieId;
    }
}
