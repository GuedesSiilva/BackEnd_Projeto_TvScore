package br.com.senai.tv_score.filme;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class NavegacaoCatalogoServiceTest {
    RestTemplate http=new RestTemplate(); MockRestServiceServer server;
    GeneroService generos=mock(GeneroService.class);CatalogoExecutor executor=new CatalogoExecutor();
    NavegacaoCatalogoService service;
    @BeforeEach void configurar(){server=MockRestServiceServer.bindTo(http).ignoreExpectOrder(true).build();service=criar(6);}
    @AfterEach void fechar(){executor.fechar();}
    NavegacaoCatalogoService criar(int chamadas){return new NavegacaoCatalogoService(new TmdbClient(http,"http://tmdb.test","teste","pt-BR"),generos,executor,"pt-BR",chamadas,5000);}
    void pagina(String caminho,int pagina,String resultado,int total){server.expect(requestTo(org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.startsWith("http://tmdb.test"+caminho),org.hamcrest.Matchers.containsString("page="+pagina))))
        .andRespond(withSuccess("{\"results\":"+resultado+",\"total_pages\":"+total+"}",MediaType.APPLICATION_JSON));}
    String item(long id,String tipo,int popularidade){return "{\"id\":"+id+",\"title\":\"X\",\"name\":\"X\",\"media_type\":\""+tipo+"\",\"popularity\":"+popularidade+"}";}
    @Test void removePessoasPreenchePaginaMantemIdentidadeECursor() {
        pagina("/search/multi",1,"["+item(5,"person",0)+"]",3);
        pagina("/search/multi",2,"["+item(7,"movie",0)+","+item(7,"tv",0)+","+item(7,"movie",0)+"]",3);
        pagina("/search/multi",3,"["+item(9,"movie",0)+"]",3);
        var a=service.pesquisar("  ação & luz  ",2,null);
        assertEquals(List.of(TipoConteudo.MOVIE,TipoConteudo.TV),a.resultados().stream().map(x->x.tipoConteudo()).toList());
        assertTrue(a.temMais());assertNull(a.totalResultados());assertFalse(a.totalExato());
        var b=service.pesquisar("ação & luz",2,a.proximoCursor());
        assertEquals(List.of(9L),b.resultados().stream().map(x->x.id()).toList());assertFalse(b.temMais());
        assertThrows(ResponseStatusException.class,()->service.pesquisar("outra",2,a.proximoCursor()));
        assertThrows(ResponseStatusException.class,()->service.pesquisar("ação & luz",2,a.proximoCursor()+"x"));
        server.verify();
    }
    @Test void orcamentoPermiteContinuarSemAfirmarTituloExistente() {
        service=criar(1);
        pagina("/search/multi",1,"["+item(1,"person",0)+"]",2);
        pagina("/search/multi",2,"["+item(2,"movie",0)+"]",2);
        var a=service.pesquisar("a",2,null);assertTrue(a.resultados().isEmpty());assertNull(a.temMais());assertTrue(a.podeContinuar());
        var b=service.pesquisar("a",2,a.proximoCursor());assertEquals(2L,b.resultados().getFirst().id());assertFalse(b.podeContinuar());server.verify();
    }
    @Test void mergeDesbalanceadoAtravesDeFronteiras() {
        when(generos.resolverGenero("drama")).thenReturn(new GeneroService.GeneroDisponivel("drama","Drama",18L,18L));
        pagina("/discover/movie",1,"["+item(1,"movie",100)+","+item(2,"movie",90)+"]",2);
        pagina("/discover/movie",2,"["+item(3,"movie",80)+"]",2);
        pagina("/discover/tv",1,"["+item(1,"tv",95)+","+item(2,"tv",85)+","+item(3,"tv",75)+"]",1);
        List<String> ids=new ArrayList<>();String cursor=null;
        do {var p=service.genero("drama",2,cursor);p.resultados().forEach(x->ids.add(x.tipoConteudo()+":"+x.id()));cursor=p.proximoCursor();}while(cursor!=null);
        assertEquals(List.of("MOVIE:1","TV:1","MOVIE:2","TV:2","MOVIE:3","TV:3"),ids);server.verify();
    }
    @Test void generoExclusivoNaoConsultaOutroTipoEEmpataPorId() {
        when(generos.resolverGenero("exclusivo")).thenReturn(new GeneroService.GeneroDisponivel("exclusivo","Exclusivo",null,99L));
        pagina("/discover/tv",1,"["+item(9,"tv",10)+","+item(2,"tv",10)+"]",1);
        var p=service.genero("exclusivo",20,null);assertEquals(List.of(2L,9L),p.resultados().stream().map(x->x.id()).toList());assertFalse(p.temMais());server.verify();
    }
    @Test void cursorExpiradoEPaginaVazia() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(service,"cursorTtlMs",100L);
        pagina("/search/multi",1,"["+item(1,"movie",1)+","+item(2,"tv",1)+"]",1);
        pagina("/search/multi",1,"[]",1);
        var p=service.pesquisar("x",1,null);Thread.sleep(150);
        assertThrows(ResponseStatusException.class,()->service.pesquisar("x",1,p.proximoCursor()));
        assertFalse(service.pesquisar("outra",20,null).podeContinuar());server.verify();
    }
    @Test void limite500SinalizaTruncamentoSemConsultar501() {
        pagina("/search/multi",1,"["+item(1,"movie",1)+","+item(2,"movie",1)+"]",501);
        pagina("/search/multi",500,"["+item(500,"movie",1)+"]",501);
        var a=service.pesquisar("limite",1,null);
        var cursores=(Map<?,?>)org.springframework.test.util.ReflectionTestUtils.getField(service,"cursores");
        var e=cursores.get(a.proximoCursor());var pos=org.springframework.test.util.ReflectionTestUtils.getField(e,"movie");
        org.springframework.test.util.ReflectionTestUtils.setField(pos,"pagina",500);
        org.springframework.test.util.ReflectionTestUtils.setField(pos,"offset",0);
        var b=service.pesquisar("limite",1,a.proximoCursor());assertEquals(500L,b.resultados().getFirst().id());assertTrue(b.truncado());assertFalse(b.podeContinuar());server.verify();
    }
}
