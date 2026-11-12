package br.com.senai.tv_score.filme;
import br.com.senai.tv_score.dto.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
class GeneroServiceTest {
 @Test void dinamicosLegadosETiposUsamMesmoCache() {
    var tmdb = mock(TmdbClient.class);
    when(tmdb.consulta(anyString())).thenAnswer(i -> UriComponentsBuilder.fromUriString("http://localhost"+i.getArgument(0)));
    when(tmdb.buscar(any(UriComponentsBuilder.class), eq(GeneroResponseDTO.class)))
      .thenAnswer(i -> new GeneroResponseDTO(((UriComponentsBuilder)i.getArgument(0)).build().toUriString().contains("movie")
        ? List.of(new GeneroDTO(18L,"Drama"),new GeneroDTO(777L,"Novo"))
        : List.of(new GeneroDTO(18L,"Drama"),new GeneroDTO(888L,"S? TV"))));
    var servico = new GeneroService(tmdb);
    assertEquals("drama",servico.resolverGenero(" DRAMA ").codigo());
    assertEquals(777L,servico.resolverGenero("tmdb-777").generoFilmeId());
    assertNull(servico.resolverGenero("tmdb-888").generoFilmeId());
    assertEquals(888L,servico.resolverGenero("tmdb-888").generoSerieId());
    assertEquals("tmdb-777", servico.resolverGenero(" NOVO ").codigo());
    assertEquals("drama", servico.resolverGenero("tmdb-18").codigo());
    assertEquals(List.of(18L, 777L), servico.idsFilmes(java.util.Set.of("drama", "tmdb-777")));
    assertEquals(List.of(18L, 777L), servico.idsFilmes(java.util.Set.of("tmdb-18", "novo")));
    assertTrue(servico.idsFilmes(java.util.Set.of("tmdb-888")).isEmpty());
    verify(tmdb,times(2)).buscar(any(UriComponentsBuilder.class),eq(GeneroResponseDTO.class));
 }
}
