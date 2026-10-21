package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacao", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_avaliacao_usuario_conteudo",
                columnNames = {
                        "usuario_id",
                        "filme_serie_id",
                        "tipo_conteudo"
                }
        )
})
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "filme_serie_id", nullable = false)
    private Long filmeSerieId;

    @Column(nullable = false)
    private Double nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conteudo", nullable = false)
    private TipoConteudo tipoConteudo;


    public Avaliacao(
            Usuario usuario,
            Long filmeSerieId,
            Double nota,
            String comentario,
            TipoConteudo tipoConteudo
    ) {
        this.usuario = usuario;
        this.filmeSerieId = filmeSerieId;
        this.nota = nota;
        this.comentario = comentario;
        this.tipoConteudo = tipoConteudo;
    }
}
