package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.GeneroConteudoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GenerosTmdbTest {
    private MockRestServiceServer servidor;
    private FilmeService service;

    @BeforeEach
    void configurar() {
        RestTemplate rest = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(rest).build();
        var tmdb = new TmdbClient(rest, "https://tmdb.test/3", "teste", "pt-BR");
        service = new FilmeService(tmdb, new GeneroService(tmdb));
    }

    private void catalogos() {
        servidor.expect(requestTo("https://tmdb.test/3/genre/movie/list?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess("""
                        {"genres":[{"id":18,"name":"Drama"},{"id":878,"name":"Ficção científica"}]}
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://tmdb.test/3/genre/tv/list?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess("""
                        {"genres":[{"id":18,"name":"Drama"},{"id":10763,"name":"Notícias"}]}
                        """, MediaType.APPLICATION_JSON));
    }

    private void descoberta(String tipo, long id, int total) {
        servidor.expect(requestTo("https://tmdb.test/3/discover/" + tipo
                        + "?api_key=teste&language=pt-BR&with_genres=" + id
                        + "&sort_by=popularity.desc&include_adult=false&page=1"))
                .andRespond(withSuccess("{\"results\":[],\"page\":1,\"total_pages\":1,\"total_results\":"
                        + total + "}", MediaType.APPLICATION_JSON));
    }

    @Test
    void uneListasSemDuplicarEReutilizaCache() {
        catalogos();
        var esperado = List.of(new GeneroConteudoDTO("drama", "Drama"),
                new GeneroConteudoDTO("tmdb-878", "Ficção científica"),
                new GeneroConteudoDTO("tmdb-10763", "Notícias"));
        assertEquals(esperado, service.listarGenerosConteudo());
        assertEquals(esperado, service.listarGenerosConteudo());
        assertEquals("drama", service.listarGenerosConteudo().getFirst().codigo());
        servidor.verify();
    }

    @Test
    void generoExclusivoDeFilmeNaoConsultaSeriesSemFiltro() {
        catalogos();
        descoberta("movie", 878, 7);
        assertEquals(7, service.buscarConteudosPorGenero("ficcao-cientifica", 1).totalResults());
        servidor.verify();
    }

    @Test
    void generoExclusivoDeSerieNaoConsultaFilmesSemFiltro() {
        catalogos();
        descoberta("tv", 10763, 3);
        assertEquals(3, service.buscarConteudosPorGenero("noticias", 1).totalResults());
        servidor.verify();
    }

    @Test
    void generoCompartilhadoConsultaOsDoisTipos() {
        catalogos();
        descoberta("movie", 18, 7);
        descoberta("tv", 18, 3);
        assertEquals(10, service.buscarConteudosPorGenero("drama", 1).totalResults());
        servidor.verify();
    }

    @Test
    void generoDesconhecidoNaoIniciaDescoberta() {
        catalogos();
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarConteudosPorGenero("inexistente", 1));
        servidor.verify();
    }

    @Test
    void respostaInvalidaNaoViraListaFixaOuVazia() {
        servidor.expect(requestTo("https://tmdb.test/3/genre/movie/list?api_key=teste&language=pt-BR"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(ResponseStatusException.class, service::listarGenerosConteudo);
        servidor.verify();
    }
}
