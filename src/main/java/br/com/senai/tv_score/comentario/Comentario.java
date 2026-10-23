package br.com.senai.tv_score.comentario;

import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentario")
@Getter
@Setter
@NoArgsConstructor
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "filme_serie_id", nullable = false)
    private Long filmeSerieId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conteudo", nullable = false)
    private TipoConteudo tipoConteudo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @CreationTimestamp
    @Column(
            name = "data_criacao",
            nullable = false,
            updatable = false
    )
    private LocalDateTime dataCriacao;

    public Comentario(
            Usuario usuario,
            Long filmeSerieId,
            TipoConteudo tipoConteudo,
            String texto
    ) {
        this.usuario = usuario;
        this.filmeSerieId = filmeSerieId;
        this.tipoConteudo = tipoConteudo;
        this.texto = texto;
    }
}