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

    @BeforeEach void configurar() {
        usuario.setId(7L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        var rest = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
        var tmdb = new TmdbClient(rest, "https://tmdb.test/3", "teste", "pt-BR");
        service = new OpcoesFilmesPreferidosService(usuarios, new FilmeService(tmdb, new GeneroService(tmdb)));
    }

    private void catalogo(String tipo) {
        servidor.expect(requestTo("https://tmdb.test/3/genre/" + tipo + "/list?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess("{\"genres\":[{\"id\":18,\"name\":\"Drama\"}"
                        + (tipo.equals("tv") ? ",{\"id\":10763,\"name\":\"Notícias\"}" : "") + "]}", MediaType.APPLICATION_JSON));
    }

    private void descoberta(String tipo, String filtro, int pagina, int totalPaginas) {
        servidor.expect(request -> {
            assertEquals("/3/discover/" + tipo, request.getURI().getPath());
            String query = request.getURI().getQuery();
            assertTrue(query.contains("include_adult=false"));
            assertTrue(query.contains("vote_count.gte=100"));
            assertTrue(query.contains("sort_by=popularity.desc"));
            assertTrue(query.contains("page=" + pagina));
            if (filtro == null) assertFalse(query.contains("with_genres="));
            else assertTrue(query.contains("with_genres=" + filtro));
        }).andRespond(withSuccess("""
                {"results":[{"id":550,"name":"Título","genre_ids":[18]}],
                "page":%d,"total_pages":%d,"total_results":20}
                """.formatted(pagina, totalPaginas), MediaType.APPLICATION_JSON));
    }

    @Test void generoComumIntercalaTiposECacheiaPrimeiraPagina() {
        usuario.getGenerosPreferidos().add("drama");
        catalogo("movie"); catalogo("tv");
        descoberta("movie", "18", 1, 1); descoberta("tv", "18", 1, 3);
        descoberta("tv", "18", 2, 3);
        var resposta = service.consultar(usuario, 1);
        assertEquals(TipoConteudo.MOVIE, resposta.resultados().get(0).tipoConteudo());
        assertEquals(TipoConteudo.TV, resposta.resultados().get(1).tipoConteudo());
        assertEquals(40, resposta.totalResults()); assertEquals(3, resposta.totalPages());
        assertEquals(resposta, service.consultar(usuario, 1));
        assertEquals(1, service.consultar(usuario, 2).resultados().size());
        servidor.verify();
    }

    @Test void semGenerosOfereceAmbosECacheDiferenciaFiltros() {
        catalogo("movie"); catalogo("tv");
        descoberta("movie", null, 1, 3); descoberta("tv", null, 1, 3);
        descoberta("movie", "18", 1, 3); descoberta("tv", "18", 1, 3);
        assertEquals(2, service.consultar(usuario, 1).resultados().size());
        usuario.getGenerosPreferidos().add("drama");
        assertEquals(2, service.consultar(usuario, 1).resultados().size());
        servidor.verify();
    }

    @Test void generoExclusivoDeSerieNaoOfereceFilmesSemRelacao() {
        usuario.getGenerosPreferidos().add("noticias");
        catalogo("movie"); catalogo("tv"); descoberta("tv", "10763", 1, 3);
        var resposta = service.consultar(usuario, 1);
        assertEquals(1, resposta.resultados().size());
        assertEquals(TipoConteudo.TV, resposta.resultados().getFirst().tipoConteudo());
        servidor.verify();
    }

    @Test void rejeitaPaginaInvalidaSemConsultarTmdb() {
        for (int pagina : new int[]{0, 501}) assertThrows(IllegalArgumentException.class, () -> service.consultar(usuario, pagina));
        servidor.verify();
    }

    @Test void exigeUsuarioAntesDeBuscarPreferencias() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.consultar(null, 1));
        verifyNoInteractions(usuarios); servidor.verify();
    }
}
