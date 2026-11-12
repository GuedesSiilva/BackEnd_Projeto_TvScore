package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.filme.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpcoesFilmesPreferidosServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Usuario usuario = new Usuario();
    private MockRestServiceServer servidor;
    private OpcoesFilmesPreferidosService service;

    @BeforeEach
    void configurar() {
        usuario.setId(7L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        var rest = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(rest).build();
        var tmdb = new TmdbClient(rest, "https://tmdb.test/3", "teste", "pt-BR");
        service = new OpcoesFilmesPreferidosService(usuarios, new FilmeService(tmdb, new GeneroService(tmdb)));
    }

    private void catalogo() {
        servidor.expect(requestTo("https://tmdb.test/3/genre/movie/list?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess("""
                        {"genres":[{"id":18,"name":"Drama"},{"id":35,"name":"Comédia"}]}
                        """, MediaType.APPLICATION_JSON));
    }

    private void descoberta(String filtro, int pagina) {
        servidor.expect(request -> {
            assertEquals("/3/discover/movie", request.getURI().getPath());
            String query = request.getURI().getQuery();
            assertTrue(query.contains("include_adult=false"));
            assertTrue(query.contains("vote_count.gte=100"));
            assertTrue(query.contains("page=" + pagina));
            if (filtro == null) assertFalse(query.contains("with_genres="));
            else assertTrue(query.contains("with_genres=" + filtro));
        }).andRespond(withSuccess("""
                {"results":[{"id":550,"title":"Filme de teste","genre_ids":[18]}],
                "page":%d,"total_pages":3,"total_results":60}
                """.formatted(pagina), MediaType.APPLICATION_JSON));
    }

    @Test
    void filtraPorQualquerGeneroEscolhidoEPaginaOsFilmes() {
        usuario.getGenerosPreferidos().addAll(java.util.Set.of("drama", "comedia"));
        catalogo();
        descoberta("18|35", 2);
        var resposta = service.consultar(usuario, 2);
        assertEquals(2, resposta.page());
        assertEquals(TipoConteudo.MOVIE, resposta.resultados().getFirst().tipoConteudo());
        servidor.verify();
    }

    @Test
    void semGenerosOferecePopulares() {
        descoberta(null, 1);
        catalogo();
        assertEquals(550L, service.consultar(usuario, 1).resultados().getFirst().id());
        servidor.verify();
    }

    @Test
    void generoExclusivoDeSerieNaoImpedeSelecaoDeFilmes() {
        usuario.getGenerosPreferidos().add("noticias");
        catalogo();
        descoberta(null, 1);
        service.consultar(usuario, 1);
        servidor.verify();
    }

    @Test
    void rejeitaPaginaInvalidaSemConsultarTmdb() {
        assertThrows(IllegalArgumentException.class, () -> service.consultar(usuario, 0));
        servidor.verify();
    }

    @Test
    void exigeUsuarioAntesDeBuscarPreferencias() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.consultar(null, 1));
        verifyNoInteractions(usuarios);
        servidor.verify();
    }
}
