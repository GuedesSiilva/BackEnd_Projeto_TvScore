package br.com.senai.tv_score.filme;
import br.com.senai.tv_score.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import java.util.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;
class ResumoConteudoServiceTest {
    @Test void vinteAvaliacoesMesmoTituloUmaConsultaCacheQuenteZeroEFalhaParcial() {
        var http=new RestTemplate();var server=MockRestServiceServer.bindTo(http).build();
        var service=new ResumoConteudoService(new TmdbClient(http,"http://tmdb.test","x","pt-BR"),"pt-BR",900,10);
        try {
            server.expect(requestTo(org.hamcrest.Matchers.containsString("/movie/7"))).andRespond(withSuccess("{\"id\":7,\"title\":\"Título\",\"vote_average\":8.4}",MediaType.APPLICATION_JSON));
            var itens=java.util.stream.LongStream.range(1,21).mapToObj(id->new AvaliacaoDTO(id,7L,9.0,null,"Nome",null,TipoConteudo.MOVIE)).toList();
            var a=service.avaliacoes(new PageImpl<>(itens));assertEquals(20,a.getNumberOfElements());assertEquals("Título",a.getContent().getFirst().conteudo().titulo());
            assertEquals(9.0,a.getContent().getFirst().nota());service.avaliacoes(new PageImpl<>(itens));server.verify();
            server.reset();server.expect(requestTo(org.hamcrest.Matchers.containsString("/tv/7"))).andRespond(withServerError());
            var b=service.favoritos(new PageImpl<>(List.of(new FavoritoDTO(1L,7L,null,TipoConteudo.TV))));assertNull(b.getContent().getFirst().conteudo());server.verify();
        }finally{service.fechar();}
    }
}
