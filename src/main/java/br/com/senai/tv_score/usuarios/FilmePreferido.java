package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.FilmePreferidoTmdbDTO;
import jakarta.persistence.*;
import br.com.senai.tv_score.filme.TipoConteudo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "usuario_filme_preferido", uniqueConstraints = @UniqueConstraint(
        name = "uk_usuario_conteudo_preferido", columnNames = {"usuario_id", "filme_id", "tipo_conteudo"}
))
@Getter
@NoArgsConstructor
public class FilmePreferido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "filme_id", nullable = false)
    private Long filmeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conteudo", nullable = false, length = 5)
    private TipoConteudo tipoConteudo = TipoConteudo.MOVIE;

    @Column(nullable = false, length = 1000)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String sinopse;

    @ElementCollection
    @CollectionTable(name = "usuario_filme_preferido_genero",
            joinColumns = @JoinColumn(name = "filme_preferido_id"))
    @MapKeyColumn(name = "genero_id")
    @Column(name = "nome", nullable = false)
    private Map<Long, String> generos = new HashMap<>();

    @Column(name = "idioma_original", length = 20)
    private String idiomaOriginal;

    @Column(name = "data_lancamento", length = 10)
    private String dataLancamento;

    @Column(name = "nota_tmdb")
    private Double notaTmdb;

    private Double popularidade;

    @Column(name = "poster_path", length = 1000)
    private String posterPath;

    @CreationTimestamp
    @Column(name = "selecionado_em", nullable = false, updatable = false)
    private LocalDateTime selecionadoEm;

    public FilmePreferido(Usuario usuario, Long filmeId) {
        this(usuario, filmeId, TipoConteudo.MOVIE);
    }

    public FilmePreferido(Usuario usuario, Long filmeId, TipoConteudo tipo) {
        this.tipoConteudo = tipo;
        this.usuario = usuario;
        this.filmeId = filmeId;
    }

    public void atualizarDados(FilmePreferidoTmdbDTO dados) {
        titulo = dados.title();
        sinopse = dados.overview();
        idiomaOriginal = dados.originalLanguage();
        dataLancamento = dados.releaseDate();
        notaTmdb = dados.voteAverage();
        popularidade = dados.popularity();
        posterPath = dados.posterPath();
        generos.clear();
        dados.genres().forEach(genero -> generos.put(genero.id(), genero.name()));
    }
}
