package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.GeneroConteudoDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosRequestDTO;
import br.com.senai.tv_score.filme.GeneroService;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreferenciasGenerosTmdbTest {
    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final GeneroService generos = mock(GeneroService.class);
    private final PreferenciasUsuarioService service = new PreferenciasUsuarioService(repositorio, generos, mock(org.springframework.transaction.PlatformTransactionManager.class));

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.getGenerosPreferidos().add("drama");
        return usuario;
    }

    @Test
    void salvaGeneroDoTmdbQueNaoExistiaNaListaFixa() {
        Usuario usuario = usuario();
        when(generos.resolverGenero(" NOTICIAS ")).thenReturn(
                new GeneroService.GeneroDisponivel("tmdb-10763", "Notícias", null, 10763L));
        when(repositorio.buscarPorIdComBloqueio(1L)).thenReturn(Optional.of(usuario));
        var genero = new GeneroConteudoDTO("tmdb-10763", "Notícias");
        when(generos.listarGenerosDisponiveis()).thenReturn(List.of(new GeneroService.GeneroDisponivel("tmdb-10763", genero.nome(), null, 10763L)));
        var resposta = service.salvar(usuario,
                new PreferenciasGenerosRequestDTO(List.of(" NOTICIAS ", " NOTICIAS ")));
        assertEquals(Set.of("tmdb-10763"), usuario.getGenerosPreferidos());
        assertEquals(List.of(genero), resposta.generos());
    }

    @Test
    void codigoInvalidoPreservaPreferenciasAnteriores() {
        Usuario usuario = usuario();
        when(generos.resolverGenero("inexistente")).thenThrow(IllegalArgumentException.class);
        assertThrows(IllegalArgumentException.class, () -> service.salvar(usuario,
                new PreferenciasGenerosRequestDTO(List.of("inexistente"))));
        assertEquals(Set.of("drama"), usuario.getGenerosPreferidos());
        verifyNoInteractions(repositorio);
    }

    @Test
    void consultaPreferenciasJaSalvasComNomesDoTmdb() {
        Usuario usuario = usuario();
        when(repositorio.findById(1L)).thenReturn(Optional.of(usuario));
        var drama = new GeneroConteudoDTO("drama", "Drama");
        when(generos.listarGenerosDisponiveis()).thenReturn(List.of(new GeneroService.GeneroDisponivel("drama", "Drama", 18L, 18L),
                new GeneroService.GeneroDisponivel("tmdb-10763", "Notícias", null, 10763L)));
        assertEquals(List.of(drama), service.consultar(usuario).generos());
    }
    @Test
    void leituraDeAliasRemotoNaoPerdeCodigoPersistido() {
        Usuario usuario = usuario();
        usuario.getGenerosPreferidos().clear();
        usuario.getGenerosPreferidos().add("noticias");
        when(repositorio.findById(1L)).thenReturn(Optional.of(usuario));
        when(generos.listarGenerosDisponiveis()).thenReturn(List.of(
            new GeneroService.GeneroDisponivel("tmdb-10763", "Noticias", null, 10763L)));
        assertEquals(List.of(new GeneroConteudoDTO("noticias", "Noticias")), service.consultar(usuario).generos());
        assertEquals(Set.of("noticias"), usuario.getGenerosPreferidos());
    }
}
