package br.com.senai.tv_score.filme;
import br.com.senai.tv_score.dto.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.time.LocalDate;
import org.springframework.web.util.UriComponentsBuilder;
class EpisodioServiceTest {
 private EpisodioDTO episodio(int numero,int votos,double nota,LocalDate data) {
    return new EpisodioDTO((long)numero,"E","",1,numero,data,20,null,nota,votos);
 }
 @Test void cacheCompartilhadoEFiltrosDeRanking() {
    var tmdb=mock(TmdbClient.class);
    when(tmdb.consulta(anyString())).thenAnswer(i -> UriComponentsBuilder.fromUriString("http://localhost"+i.getArgument(0)));
    when(tmdb.buscar(any(UriComponentsBuilder.class),eq(TemporadasSerieDTO.class)))
      .thenReturn(new TemporadasSerieDTO(List.of(new TemporadasSerieDTO.ResumoTemporada(0),new TemporadasSerieDTO.ResumoTemporada(1))));
    when(tmdb.buscar(any(UriComponentsBuilder.class),eq(TemporadaDTO.class)))
      .thenReturn(new TemporadaDTO(1L,"T","",1,null,List.of(
        episodio(1,10,8,LocalDate.of(2020,1,1)), episodio(2,20,8,LocalDate.of(2020,1,1)),
        episodio(3,30,10,LocalDate.of(2999,1,1)),episodio(4,1,10,LocalDate.of(2020,1,1)))));
    var servico=new EpisodioService(tmdb);
    try {
      var painel=servico.buscarPainel(1L,10,5,false);
      assertEquals(List.of(2,1),painel.topRated().stream().map(EpisodioDTO::numero).toList());
      assertEquals(1,painel.temporadas().size());
      servico.buscarPainel(1L,1,5,false);
      verify(tmdb,times(1)).buscar(any(UriComponentsBuilder.class),eq(TemporadaDTO.class));
    } finally {servico.encerrar();}
 }
}
