package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.GeneroConteudoDTO;
import br.com.senai.tv_score.dto.PreferenciasGenerosRequestDTO;
import br.com.senai.tv_score.filme.FilmeService;
import br.com.senai.tv_score.filme.GeneroConteudo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreferenciasGenerosTmdbTest {
    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final FilmeService filmes = mock(FilmeService.class);
    private final PreferenciasUsuarioService service = new PreferenciasUsuarioService(repositorio, filmes);

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.getGenerosPreferidos().add("drama");
        return usuario;
    }

    @Test
    void salvaGeneroDoTmdbQueNaoExistiaNaListaFixa() {
        Usuario usuario = usuario();
        when(filmes.buscarGeneroConteudo(" NOTICIAS ")).thenReturn(
                new GeneroConteudo("noticias", "Notícias", null, 10763L));
        when(repositorio.buscarPorIdComBloqueio(1L)).thenReturn(Optional.of(usuario));
        var genero = new GeneroConteudoDTO("noticias", "Notícias");
        when(filmes.listarGenerosConteudo()).thenReturn(List.of(genero));
        var resposta = service.salvar(usuario,
                new PreferenciasGenerosRequestDTO(List.of(" NOTICIAS ", " NOTICIAS ")));
        assertEquals(Set.of("noticias"), usuario.getGenerosPreferidos());
        assertEquals(List.of(genero), resposta.generos());
    }

    @Test
    void codigoInvalidoPreservaPreferenciasAnteriores() {
        Usuario usuario = usuario();
        when(filmes.buscarGeneroConteudo("inexistente")).thenThrow(IllegalArgumentException.class);
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
        when(filmes.listarGenerosConteudo()).thenReturn(List.of(drama,
                new GeneroConteudoDTO("noticias", "Notícias")));
        assertEquals(List.of(drama), service.consultar(usuario).generos());
    }
}
