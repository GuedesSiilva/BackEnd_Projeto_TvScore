package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.*;
import br.com.senai.tv_score.filme.TmdbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class FilmesPreferidosServiceTest {
    private final FilmePreferidoRepository filmes = mock(FilmePreferidoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Usuario usuario = new Usuario();
    private MockRestServiceServer servidor;
    private FilmesPreferidosService service;

    @BeforeEach
    void configurar() {
        usuario.setId(7L);
        RestTemplate rest = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(rest).build();
        service = new FilmesPreferidosService(filmes, usuarios,
                new TmdbClient(rest, "https://tmdb.test/3", "teste", "pt-BR"));
        when(usuarios.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(usuario));
        when(filmes.findByUsuarioIdOrderByFilmeIdAsc(7L)).thenReturn(List.of());
        when(filmes.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void respostaTmdb(long id, String json) {
        servidor.expect(requestTo("https://tmdb.test/3/movie/" + id + "?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void serieReconheceAliasesEDeduplicaPar() {
        servidor.expect(requestTo("https://tmdb.test/3/tv/550?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess("{\"id\":550,\"name\":\"Série\",\"first_air_date\":\"2011-04-17\",\"genres\":[]}", MediaType.APPLICATION_JSON));
        var item = new ConteudoPreferidoRequestDTO(550L, br.com.senai.tv_score.filme.TipoConteudo.TV);
        var resposta = service.salvarConteudos(usuario, new ConteudosPreferidosRequestDTO(List.of(item, item)));
        assertEquals(1, resposta.conteudos().size());
        assertEquals("Série", resposta.conteudos().getFirst().titulo());
        assertEquals("2011-04-17", resposta.conteudos().getFirst().dataLancamento());
        servidor.verify();
    }

    @Test
    void falhaNaSerieNaoModificaHistorico() {
        respostaTmdb(550, "{\"id\":550,\"title\":\"Filme\",\"genres\":[]}");
        servidor.expect(requestTo("https://tmdb.test/3/tv/550?api_key=teste&language=pt-BR"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThrows(ResponseStatusException.class, () -> service.salvarConteudos(usuario,
                new ConteudosPreferidosRequestDTO(List.of(
                        new ConteudoPreferidoRequestDTO(550L, br.com.senai.tv_score.filme.TipoConteudo.MOVIE),
                        new ConteudoPreferidoRequestDTO(550L, br.com.senai.tv_score.filme.TipoConteudo.TV)))));
        verifyNoInteractions(filmes, usuarios);
        servidor.verify();
    }

    @Test
    void buscaDadosNoTmdbEDeduplicaSelecao() {
        respostaTmdb(550, """
                {"id":550,"title":"Clube da Luta","overview":"Sinopse de teste",
                "genres":[{"id":18,"name":"Drama"}],"original_language":"en",
                "release_date":"1999-10-15","vote_average":8.4,"popularity":37.2,
                "poster_path":"/poster.jpg"}
                """);
        var resposta = service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L, 550L)));
        assertEquals(1, resposta.filmes().size());
        var filme = resposta.filmes().getFirst();
        assertEquals("Clube da Luta", filme.titulo());
        assertEquals("Sinopse de teste", filme.sinopse());
        assertEquals(List.of(new GeneroDTO(18L, "Drama")), filme.generos());
        assertEquals("en", filme.idiomaOriginal());
        assertEquals("1999-10-15", filme.dataLancamento());
        assertEquals(8.4, filme.notaTmdb());
        assertEquals(37.2, filme.popularidade());
        servidor.verify();
    }

    @Test
    void falhaNoSegundoFilmeNaoAlteraSelecaoAnterior() {
        respostaTmdb(550, "{\"id\":550,\"title\":\"Filme\",\"genres\":[]}");
        servidor.expect(requestTo("https://tmdb.test/3/movie/999?api_key=teste&language=pt-BR"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L, 999L))));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verifyNoInteractions(filmes, usuarios);
        servidor.verify();
    }

    @Test
    void respostaTmdbInvalidaNaoGravaNada() {
        respostaTmdb(550, "{\"id\":999,\"title\":\"Outro filme\",\"genres\":[]}");
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of(550L))));
        assertEquals(HttpStatus.BAD_GATEWAY, erro.getStatusCode());
        verifyNoInteractions(filmes, usuarios);
        servidor.verify();
    }

    @Test
    void listaVaziaLimpaSelecaoSemConsultarTmdb() {
        FilmePreferido anterior = new FilmePreferido(usuario, 550L);
        when(filmes.findByUsuarioIdOrderByFilmeIdAsc(7L)).thenReturn(List.of(anterior));
        assertTrue(service.salvar(usuario, new FilmesPreferidosRequestDTO(List.of())).filmes().isEmpty());
        verify(filmes).deleteAll(List.of(anterior));
        servidor.verify();
    }

    @Test
    void consultaUsaSomenteDadosPersistidosDoUsuarioAutenticado() {
        assertTrue(service.consultar(usuario).filmes().isEmpty());
        verify(filmes).findByUsuarioIdOrderByFilmeIdAsc(7L);
        verifyNoInteractions(usuarios);
        servidor.verify();
    }

    @Test
    void exigeUsuarioAutenticadoAntesDeConsultarOuSalvar() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> service.consultar(null)).getStatusCode());
        assertThrows(ResponseStatusException.class,
                () -> service.salvar(null, new FilmesPreferidosRequestDTO(List.of(550L))));
        verifyNoInteractions(filmes, usuarios);
        servidor.verify();
    }
}
