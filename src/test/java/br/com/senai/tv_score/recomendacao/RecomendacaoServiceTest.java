package br.com.senai.tv_score.recomendacao;

import br.com.senai.tv_score.filme.TipoConteudo;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.net.SocketTimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RecomendacaoServiceTest {
    private static final String INTERNO = "teste-interno-nao-secreto-000000000000";
    private static final String ITEM = "{\"id\":1,\"titulo\":\"Filme\",\"nota\":8,\"generos\":[18],\"score_recomendacao\":80,\"similaridade_sinopse\":0.7}";

    @Test void preservaAnoOpcionalNaRespostaDeFilmesESeries() throws Exception {
        for (boolean serie : new boolean[]{false, true}) {
            for (String campo : new String[]{",\"ano\":2024", ",\"ano\":null", ""}) {
                var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
                server.expect(anything()).andRespond(withSuccess(
                        "{\"total\":1,\"recomendacoes\":[" + ITEM.replace("}", campo + "}") + "]}",
                        MediaType.APPLICATION_JSON));
                var service = new RecomendacaoService("http://localhost", http, INTERNO);
                var resposta = serie ? service.buscarSeries("Bearer teste") : service.buscarFilmes("Bearer teste");
                Integer esperado = campo.contains("2024") ? 2024 : null;
                assertEquals(esperado, resposta.recomendacoes().getFirst().ano());
                var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
                var json = mapper.readTree(mapper.writeValueAsString(resposta));
                assertEquals(mapper.valueToTree(esperado), json.get("recomendacoes").get(0).get("ano"));
                server.verify();
            }
        }
    }

    @Test void encaminhaBearerECredencialInterna() {
        var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://localhost/recomendacoes/filmes/java"))
                .andExpect(header("Authorization", "Bearer conta-a"))
                .andExpect(header("X-TVScore-Internal-Token", INTERNO))
                .andRespond(withSuccess("{\"total\":1,\"recomendacoes\":["+ITEM+"]}", MediaType.APPLICATION_JSON));
        var r = new RecomendacaoService("http://localhost", http, INTERNO).buscarFilmes("Bearer conta-a");
        assertEquals(TipoConteudo.MOVIE, r.recomendacoes().getFirst().tipoConteudo());
        assertEquals(0.7, r.recomendacoes().getFirst().similaridadeSinopse());
        server.verify();
    }

    @Test void seriesUsaEndpointProprio() {
        var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://localhost/recomendacoes/series/java"))
                .andExpect(header("Authorization", "Bearer conta-b"))
                .andRespond(withSuccess("{\"total\":0,\"recomendacoes\":[]}", MediaType.APPLICATION_JSON));
        assertTrue(new RecomendacaoService("http://localhost", http, INTERNO).buscarSeries("Bearer conta-b").recomendacoes().isEmpty());
        server.verify();
    }

    @Test void preservaErrosDeAutorizacaoIndisponibilidadeEPrazo() {
        for (int codigo : new int[]{401,403,429,503,504}) {
            var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
            server.expect(anything()).andRespond(withStatus(HttpStatusCode.valueOf(codigo)));
            var e = assertThrows(ResponseStatusException.class,
                    () -> new RecomendacaoService("http://localhost", http, INTERNO).buscarFilmes("Bearer teste"));
            assertEquals(codigo == 429 ? 503 : codigo, e.getStatusCode().value());
            server.verify();
        }
    }

    @Test void rejeitaContratoInvalido() {
        for (String json : new String[]{"{}", "{", "{\"total\":1,\"recomendacoes\":[]}",
                "{\"total\":1,\"recomendacoes\":[null]}",
                "{\"total\":2,\"recomendacoes\":["+ITEM+","+ITEM+"]}",
                "{\"total\":1,\"recomendacoes\":["+ITEM.replace("\"nota\":8", "\"nota\":11")+"]}"}) {
            var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
            server.expect(anything()).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
            var e = assertThrows(ResponseStatusException.class,
                    () -> new RecomendacaoService("http://localhost", http, INTERNO).buscarFilmes("Bearer teste"));
            assertEquals(502, e.getStatusCode().value());
            server.verify();
        }
    }

    @Test void traduzTimeoutDeTransporte() {
        var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
        server.expect(anything()).andRespond(withException(new SocketTimeoutException()));
        var e = assertThrows(ResponseStatusException.class,
                () -> new RecomendacaoService("http://localhost", http, INTERNO).buscarFilmes("Bearer teste"));
        assertEquals(504, e.getStatusCode().value());
    }

    @Test void rejeitaTokenAusenteECredencialInternaVazia() {
        assertThrows(IllegalArgumentException.class, () -> new RecomendacaoService("http://localhost", new RestTemplate(), ""));
        var service = new RecomendacaoService("http://localhost", new RestTemplate(), INTERNO);
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> service.buscarFilmes(null)).getStatusCode().value());
    }
}
