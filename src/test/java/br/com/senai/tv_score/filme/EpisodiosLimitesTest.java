package br.com.senai.tv_score.filme;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.*;
import br.com.senai.tv_score.dto.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
class EpisodiosLimitesTest {
    @Test void prazoTotalCancelaCargaBloqueada() {
        var tmdb=mock(TmdbClient.class);var interrompido=new CountDownLatch(1);
        when(tmdb.consulta(anyString())).thenAnswer(i->UriComponentsBuilder.fromUriString("http://tmdb.test"+i.getArgument(0)));
        when(tmdb.buscar(any(UriComponentsBuilder.class),eq(TemporadasSerieDTO.class))).thenAnswer(i->{try{Thread.sleep(5000);}catch(InterruptedException ex){interrompido.countDown();throw new IllegalStateException();}return null;});
        var service=new EpisodioService(tmdb);ReflectionTestUtils.setField(service,"prazoMs",50L);
        try {
            var erro=assertThrows(ResponseStatusException.class,()->service.carregarTemporadas(1L,false));assertEquals(504,erro.getStatusCode().value());
            try{assertTrue(interrompido.await(2,TimeUnit.SECONDS));}catch(InterruptedException ex){throw new AssertionError(ex);}
        }finally{service.encerrar();}
    }
    @Test void limiteEntreRequisicoesRejeitaSemPoolIlimitado() throws Exception {
        var tmdb=mock(TmdbClient.class);var entrou=new CountDownLatch(4);var liberar=new CountDownLatch(1);
        when(tmdb.consulta(anyString())).thenAnswer(i->UriComponentsBuilder.fromUriString("http://tmdb.test"+i.getArgument(0)));
        when(tmdb.buscar(any(UriComponentsBuilder.class),eq(TemporadasSerieDTO.class))).thenAnswer(i->{entrou.countDown();liberar.await(5,TimeUnit.SECONDS);return new TemporadasSerieDTO(List.of());});
        var service=new EpisodioService(tmdb);
        try(var pool=Executors.newFixedThreadPool(4)) {
            var tarefas=java.util.stream.LongStream.range(1,5).mapToObj(id->pool.submit(()->service.carregarTemporadas(id,false))).toList();
            assertTrue(entrou.await(3,TimeUnit.SECONDS));
            assertEquals(503,assertThrows(ResponseStatusException.class,()->service.carregarTemporadas(10L,false)).getStatusCode().value());
            liberar.countDown();for(var t:tarefas)assertTrue(t.get(3,TimeUnit.SECONDS).isEmpty());
        }finally{liberar.countDown();service.encerrar();}
    }
}
