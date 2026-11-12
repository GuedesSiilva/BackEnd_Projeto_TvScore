package br.com.senai.tv_score.recomendacao;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;
class RecomendacaoServiceTest {
 @Test void encaminhaBearerEPreservaContrato() {
    var http = new RestTemplate();
    var server = MockRestServiceServer.bindTo(http).build();
    server.expect(requestTo("http://localhost/recomendacoes/filmes/java"))
      .andExpect(header("Authorization","Bearer teste"))
      .andRespond(withSuccess("{\"total\":1,\"recomendacoes\":[{\"id\":1,\"titulo\":\"Filme\",\"similaridade_sinopse\":0.7}]}",MediaType.APPLICATION_JSON));
    var resultado = new RecomendacaoService("http://localhost",http).buscarFilmes("Bearer teste");
    assertEquals("Filme",resultado.recomendacoes().getFirst().titulo());
    assertEquals(0.7,resultado.recomendacoes().getFirst().similaridadeSinopse());
    server.verify();
 }
 @Test void traduzIndisponibilidade() {
    var http = new RestTemplate(); var server = MockRestServiceServer.bindTo(http).build();
    server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
      () -> new RecomendacaoService("http://localhost",http).buscarFilmes("Bearer teste"));
    assertEquals(503,erro.getStatusCode().value());
 }
}
