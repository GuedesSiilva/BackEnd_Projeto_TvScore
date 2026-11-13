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
                    long id = Long.parseLong(uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));
                    return new FilmePreferidoTmdbDTO(id, uri.getPath(), "Sinopse",
                            List.of(new GeneroDTO(18L, "Drama")), "en", "1999-10-15", 8.4, 20.0, "/poster.jpg");
                });
    }

    private ConteudoPreferidoRequestDTO item(long id, br.com.senai.tv_score.filme.TipoConteudo tipo) {
        return new ConteudoPreferidoRequestDTO(id, tipo);
    }

    @Test
    void mesmoIdNosDoisTiposDeduplicaParPreservaTimestampELegadoPreservaSeries() {
        var movie = item(550L, br.com.senai.tv_score.filme.TipoConteudo.MOVIE);
        var tv = item(550L, br.com.senai.tv_score.filme.TipoConteudo.TV);
        service.salvarConteudos(usuario, new ConteudosPreferidosRequestDTO(List.of(movie, tv, tv)));
        em.clear();
        var salvos = service.consultarConteudos(usuario).conteudos();
        assertEquals(2, salvos.size());
        assertEquals("/movie/550", salvos.get(0).titulo());
        assertEquals("/tv/550", salvos.get(1).titulo());
        var timestamp = salvos.get(1).selecionadoEm();
        service.salvarConteudos(usuario, new ConteudosPreferidosRequestDTO(List.of(movie, tv)));
        em.clear();
        assertEquals(timestamp, service.consultarConteudos(usuario).conteudos().get(1).selecionadoEm());
        assertEquals(1, service.consultar(usuario).filmes().size());
        service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of()));
        em.clear();
        assertTrue(service.consultar(usuario).filmes().isEmpty());
        assertEquals(timestamp, service.consultarConteudos(usuario).conteudos().getFirst().selecionadoEm());
        service.salvarConteudos(usuario, new ConteudosPreferidosRequestDTO(List.of()));
        em.clear();
        assertEquals(0, filmes.count());
        assertEquals(0L, em.createNativeQuery("select count(*) from usuario_filme_preferido_genero", Long.class).getSingleResult());
    }

    @Test
    void limiteLegadoContaSeriesSemAlterarSelecao() {
        service.salvarConteudos(usuario, new ConteudosPreferidosRequestDTO(List.of(item(1, br.com.senai.tv_score.filme.TipoConteudo.TV))));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(2L, 3L, 4L, 5L, 6L))));
        assertEquals(1, filmes.count());
    }

    @Test
    void bancoRejeitaMesmoParDuplicado() {
        var dados = new FilmePreferidoTmdbDTO(550L, "Título", "Sinopse", List.of(), "en", "2020-01-01", 8.0, 1.0, null);
        var primeiro = new FilmePreferido(usuario, 550L);
        primeiro.atualizarDados(dados);
        filmes.saveAndFlush(primeiro);
        var duplicado = new FilmePreferido(usuario, 550L);
        duplicado.atualizarDados(dados);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> filmes.saveAndFlush(duplicado));
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
        assertTrue(service.consultarConteudos(outro).conteudos().isEmpty());
        service.salvar(outro, new FilmesPreferidosRequestDTO(List.of(551L)));
        assertEquals(550L, service.consultar(usuario).filmes().getFirst().filmeId());
        assertEquals(551L, service.consultar(outro).filmes().getFirst().filmeId());
    }

    @Test
    void excluirContaRemoveSelecaoEGenerosRelacionados() {
        service.salvarConteudos(usuario, new ConteudosPreferidosRequestDTO(List.of(
                item(550L, br.com.senai.tv_score.filme.TipoConteudo.MOVIE),
                item(550L, br.com.senai.tv_score.filme.TipoConteudo.TV))));
        when(encoder.matches("senha", "hash")).thenReturn(true);
        usuarioService.excluir(usuario, new ExcluirContaDTO("senha"));
        em.clear();
        assertFalse(usuarios.existsById(usuario.getId()));
        assertEquals(0, filmes.count());
        assertEquals(0L, em.createNativeQuery("select count(*) from usuario_filme_preferido_genero", Long.class)
                .getSingleResult());
    }
}
