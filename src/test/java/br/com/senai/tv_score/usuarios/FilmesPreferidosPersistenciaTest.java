package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.*;
import br.com.senai.tv_score.filme.TmdbClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@Import({FilmesPreferidosService.class, UsuarioService.class})
class FilmesPreferidosPersistenciaTest {
    @Autowired private FilmesPreferidosService service;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private FilmePreferidoRepository filmes;
    @Autowired private EntityManager em;
    @MockitoBean private TmdbClient tmdb;
    @MockitoBean private PasswordEncoder encoder;
    private Usuario usuario;

    @BeforeEach
    void configurar() {
        usuario = new Usuario();
        usuario.setNome("Teste");
        usuario.setEmail("teste@tvscore.com");
        usuario.setSenha("hash");
        usuario = usuarios.saveAndFlush(usuario);
        when(tmdb.consulta(anyString())).thenAnswer(inv ->
                UriComponentsBuilder.fromUriString("https://tmdb.test" + inv.getArgument(0)));
        when(tmdb.buscar(any(UriComponentsBuilder.class), eq(FilmePreferidoTmdbDTO.class)))
                .thenAnswer(inv -> {
                    var uri = ((UriComponentsBuilder) inv.getArgument(0)).build().toUri();
                    long id = Long.parseLong(uri.getPath().substring("/movie/".length()));
                    return new FilmePreferidoTmdbDTO(id, "Filme " + id, "Sinopse",
                            List.of(new GeneroDTO(18L, "Drama")), "en", "1999-10-15", 8.4, 20.0, "/poster.jpg");
                });
    }

    @Test
    void persisteMetadadosSemDuplicarEPermiteSubstituirOuLimpar() {
        service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L, 551L)));
        em.clear();
        var primeira = service.consultar(usuario).filmes();
        assertEquals(2, primeira.size());
        assertEquals(List.of(new GeneroDTO(18L, "Drama")), primeira.getFirst().generos());
        assertNotNull(primeira.getFirst().selecionadoEm());
        var selecionadoEm = primeira.getFirst().selecionadoEm();
        service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L, 550L)));
        em.clear();
        assertEquals(1, filmes.count());
        assertEquals(selecionadoEm, service.consultar(usuario).filmes().getFirst().selecionadoEm());
        assertEquals(1L, em.createNativeQuery("select count(*) from usuario_filme_preferido_genero", Long.class)
                .getSingleResult());
        service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of()));
        em.clear();
        assertEquals(0, filmes.count());
        assertEquals(0L, em.createNativeQuery("select count(*) from usuario_filme_preferido_genero", Long.class)
                .getSingleResult());
    }

    @Test
    void usuariosTemSelecoesIndependentes() {
        service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L)));
        var outro = new Usuario();
        outro.setNome("Outro"); outro.setEmail("outro@tvscore.com"); outro.setSenha("hash");
        outro = usuarios.saveAndFlush(outro);
        assertTrue(service.consultar(outro).filmes().isEmpty());
        service.salvar(outro, new FilmesPreferidosRequestDTO(List.of(551L)));
        assertEquals(550L, service.consultar(usuario).filmes().getFirst().filmeId());
        assertEquals(551L, service.consultar(outro).filmes().getFirst().filmeId());
    }

    @Test
    void excluirContaRemoveSelecaoEGenerosRelacionados() {
        service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L)));
        when(encoder.matches("senha", "hash")).thenReturn(true);
        usuarioService.excluir(usuario, new ExcluirContaDTO("senha"));
        em.clear();
        assertFalse(usuarios.existsById(usuario.getId()));
        assertEquals(0, filmes.count());
        assertEquals(0L, em.createNativeQuery("select count(*) from usuario_filme_preferido_genero", Long.class)
                .getSingleResult());
    }
}
