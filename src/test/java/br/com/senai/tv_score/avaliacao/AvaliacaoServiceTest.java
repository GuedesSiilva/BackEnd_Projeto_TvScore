package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.dto.AtualizarAvaliacaoDTO;
import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvaliacaoServiceTest {

    private final AvaliacaoRepository repositorio = mock(AvaliacaoRepository.class);
    private final AvaliacaoService service = new AvaliacaoService(repositorio);
    private final Usuario usuario = usuario(1L);

    @Test
    void aplicaLimiteDeComentarioNaCriacaoENaEdicao() {
        String comentario = "x".repeat(1001);
        assertThrows(IllegalArgumentException.class,
                () -> service.avaliar(usuario, entrada(TipoConteudo.MOVIE, comentario)));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(usuario, 1L, new AtualizarAvaliacaoDTO(8.0, comentario)));
        verifyNoInteractions(repositorio);
    }

    @Test
    void impedeQueOutroUsuarioEditeOuRemova() {
        Avaliacao avaliacao = new Avaliacao(usuario(2L), 550L, 8.0, "Original", TipoConteudo.MOVIE);
        when(repositorio.findById(12L)).thenReturn(Optional.of(avaliacao));

        ResponseStatusException editar = assertThrows(ResponseStatusException.class,
                () -> service.atualizar(usuario, 12L, new AtualizarAvaliacaoDTO(9.0, "Alterado")));
        ResponseStatusException remover = assertThrows(ResponseStatusException.class,
                () -> service.removerAvaliacao(usuario, 12L));

        assertEquals(HttpStatus.FORBIDDEN, editar.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, remover.getStatusCode());
        assertEquals("Original", avaliacao.getComentario());
    }

    @Test
    void serieNaoSobrescreveFilmeComOMesmoIdTmdb() {
        Avaliacao filme = new Avaliacao(usuario, 550L, 8.0, "Original", TipoConteudo.MOVIE);
        when(repositorio.findByUsuarioAndFilmeSerieIdAndTipoConteudo(usuario, 550L, TipoConteudo.MOVIE))
                .thenReturn(Optional.of(filme));
        when(repositorio.findByUsuarioAndFilmeSerieIdAndTipoConteudo(usuario, 550L, TipoConteudo.TV))
                .thenReturn(Optional.empty());
        when(repositorio.save(any(Avaliacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        AvaliacaoDTO serie = service.avaliar(usuario, entrada(TipoConteudo.TV, "  Série  "));

        assertEquals(TipoConteudo.TV, serie.tipoConteudo());
        assertEquals("Série", serie.comentario());
        assertEquals("Original", filme.getComentario());
    }

    private static AvaliacaoDTO entrada(TipoConteudo tipo, String comentario) {
        return new AvaliacaoDTO(null, 550L, 8.0, comentario, null, null, tipo);
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário " + id);
        return usuario;
    }
}
