package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.dto.FilmesPreferidosRequestDTO;
import br.com.senai.tv_score.dto.FilmesPreferidosResponseDTO;
import br.com.senai.tv_score.security.*;
import br.com.senai.tv_score.usuarios.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.Filter;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FilmesPreferidosController.class, OpcoesFilmesPreferidosController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class FilmesPreferidosControllerTest {
    private static final String ROTA = "/api/usuarios/eu/filmes-preferidos";
    private MockMvc mvc;
    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter seguranca;
    @MockitoBean private FilmesPreferidosService service;
    @MockitoBean private OpcoesFilmesPreferidosService opcoes;
    @MockitoBean private JwtService jwt;
    @MockitoBean private UsuarioRepository usuarios;
    private Usuario usuario;

    @BeforeEach
    void autenticar() {
        // Registra somente a cadeia real: evita executar o filtro JWT duas vezes no slice MVC.
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(seguranca).build();
        usuario = new Usuario();
        usuario.setId(7L);
        when(jwt.extrairEmailSeValido("teste")).thenReturn(Optional.of("teste@tvscore.com"));
        when(usuarios.findByEmail("teste@tvscore.com")).thenReturn(Optional.of(usuario));
    }

    @Test
    void opcoesExigemTokenEUsamUsuarioDaSessao() throws Exception {
        mvc.perform(get(ROTA + "/opcoes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(opcoes);
        when(opcoes.consultar(usuario, 2)).thenReturn(
                new br.com.senai.tv_score.dto.TmdbResponseDTO(List.of(), 2, 3, 60));
        mvc.perform(get(ROTA + "/opcoes").param("pagina", "2")
                        .header("Authorization", "Bearer teste"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2));
        verify(opcoes).consultar(usuario, 2);
    }
    @Test
    void getEPutExigemLogin() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(ROTA).contentType(MediaType.APPLICATION_JSON).content("{\"filmes\":[550]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void recebeSelecaoComTokenERetornaContrato() throws Exception {
        var dto = new FilmesPreferidosRequestDTO(List.of(550L));
        when(service.salvar(usuario, dto)).thenReturn(new FilmesPreferidosResponseDTO(List.of()));
        mvc.perform(put(ROTA).header("Authorization", "Bearer teste")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"filmes\":[550]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.filmes").isArray())
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(service).salvar(usuario, dto);
    }

    @Test
    void consultaSelecaoDoUsuarioDoToken() throws Exception {
        when(service.consultar(usuario)).thenReturn(new FilmesPreferidosResponseDTO(List.of()));
        mvc.perform(get(ROTA).header("Authorization", "Bearer teste"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.filmes").isEmpty());
        verify(service).consultar(usuario);
    }

    @Test
    void rejeitaMaisDeCincoFilmesIdsInvalidosEListaAusente() throws Exception {
        for (String corpo : List.of("{\"filmes\":[1,2,3,4,5,6]}", "{\"filmes\":[0]}",
                "{\"filmes\":[-1]}", "{\"filmes\":[null]}", "{\"filmes\":null}", "{}")) {
            mvc.perform(put(ROTA).header("Authorization", "Bearer teste")
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
