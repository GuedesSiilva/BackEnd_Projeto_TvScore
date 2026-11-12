package br.com.senai.tv_score.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "usuario_genero",
            joinColumns = @JoinColumn(
                    name = "usuario_id",
                    nullable = false
            ),
            foreignKey = @ForeignKey(
                    name = "fk_usuario_genero_usuario"
            )
    )
    @Column(
            name = "codigo_genero",
            nullable = false,
            length = 40
    )
    private Set<String> generosPreferidos = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    public Usuario(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }
    public static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank() || nome.strip().length() > 60)
            throw new IllegalArgumentException("Nome obrigatorio, com ate 60 caracteres.");
        return nome.strip();
    }
}
