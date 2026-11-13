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

@WebMvcTest({FilmesPreferidosController.class, ConteudosPreferidosController.class, OpcoesFilmesPreferidosController.class})
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

    @Test
    void rotasGenericasExigemJwt() throws Exception {
        String rota = "/api/usuarios/eu/conteudos-preferidos";
        mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        mvc.perform(get(rota + "/opcoes")).andExpect(status().isUnauthorized());
        mvc.perform(put(rota).contentType(MediaType.APPLICATION_JSON).content("{\"conteudos\":[]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, opcoes);
    }

    @Test
    void contratoGenericoValidaItensAninhados() throws Exception {
        for (String corpo : List.of("{}", "{\"conteudos\":null}", "{\"conteudos\":[null]}",
                "{\"conteudos\":[{\"id\":0,\"tipoConteudo\":\"TV\"}]}",
                "{\"conteudos\":[{\"id\":1}]}", "{\"conteudos\":[{\"tipoConteudo\":\"MOVIE\"}]}",
                "{\"conteudos\":[{\"id\":1,\"tipoConteudo\":\"INVALIDO\"}]}",
                "{\"conteudos\":[{\"id\":1,\"tipoConteudo\":\"TV\"},{\"id\":2,\"tipoConteudo\":\"TV\"},{\"id\":3,\"tipoConteudo\":\"TV\"},{\"id\":4,\"tipoConteudo\":\"TV\"},{\"id\":5,\"tipoConteudo\":\"TV\"},{\"id\":6,\"tipoConteudo\":\"TV\"}]}")) {
            mvc.perform(put("/api/usuarios/eu/conteudos-preferidos").header("Authorization", "Bearer teste")
                    .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void contratoGenericoUsaUsuarioDoToken() throws Exception {
        var dto = new br.com.senai.tv_score.dto.ConteudosPreferidosRequestDTO(List.of(
                new br.com.senai.tv_score.dto.ConteudoPreferidoRequestDTO(550L, br.com.senai.tv_score.filme.TipoConteudo.TV)));
        when(service.salvarConteudos(usuario, dto)).thenReturn(new br.com.senai.tv_score.dto.ConteudosPreferidosResponseDTO(List.of()));
        mvc.perform(put("/api/usuarios/eu/conteudos-preferidos").header("Authorization", "Bearer teste")
                .contentType(MediaType.APPLICATION_JSON).content("{\"conteudos\":[{\"id\":550,\"tipoConteudo\":\"TV\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudos").isArray());
        verify(service).salvarConteudos(usuario, dto);
        when(service.consultarConteudos(usuario)).thenReturn(new br.com.senai.tv_score.dto.ConteudosPreferidosResponseDTO(List.of()));
        mvc.perform(get("/api/usuarios/eu/conteudos-preferidos").header("Authorization", "Bearer teste"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudos").isArray());
        verify(service).consultarConteudos(usuario);
    }

    @BeforeEach
    void autenticar() {
        // Registra somente a cadeia real: evita executar o filtro JWT duas vezes no slice MVC.
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(seguranca).build();
        usuario = new Usuario();
        usuario.setId(7L);
        when(jwt.extrairUsuarioIdSeValido("teste")).thenReturn(Optional.of(7L));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
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
