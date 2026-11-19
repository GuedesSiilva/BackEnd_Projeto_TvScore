package br.com.senai.tv_score.controller;

import br.com.senai.tv_score.avaliacao.Avaliacao;
import br.com.senai.tv_score.avaliacao.AvaliacaoRepository;
import br.com.senai.tv_score.avaliacao.AvaliacaoService;
import br.com.senai.tv_score.filme.ResumoConteudoService;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.security.JwtAuthenticationFilter;
import br.com.senai.tv_score.security.JwtService;
import br.com.senai.tv_score.security.SecurityConfig;
import br.com.senai.tv_score.usuarios.FotoService;
import br.com.senai.tv_score.usuarios.Usuario;
import br.com.senai.tv_score.usuarios.UsuarioRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvaliacaoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, AvaliacaoService.class})
class AvaliacaoSegurancaTest {
    private static final String ROTA = "/api/avaliacoes";
    private static final String LISTAGEM = ROTA + "/filme/42";

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter seguranca;
    @MockitoBean private AvaliacaoRepository avaliacoes;
    @MockitoBean private UsuarioRepository usuarios;
    @MockitoBean private FotoService fotos;
    @MockitoBean private ResumoConteudoService resumos;
    private MockMvc mvc;

    @BeforeEach
    void configurarMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguranca).build();
    }

    @ParameterizedTest
    @EnumSource(TipoConteudo.class)
    void visitanteLeAvaliacoesEComentariosPorConteudo(TipoConteudo tipo) throws Exception {
        Usuario autor = new Usuario();
        autor.setId(777L);
        autor.setNome("Autora");
        autor.setEmail("privado@example.com");
        var avaliacao = new Avaliacao(autor, 42L, 8.0, "Bom conteúdo", tipo);
        var pagina = PageRequest.of(1, 1, Sort.by(Sort.Order.desc("dataCriacao"), Sort.Order.desc("id")));
        when(avaliacoes.findByFilmeSerieIdAndTipoConteudo(42L, tipo)).thenReturn(List.of(avaliacao));
        when(avaliacoes.findByFilmeSerieIdAndTipoConteudo(42L, tipo, pagina))
                .thenReturn(new PageImpl<>(List.of(avaliacao), pagina, 3));
        when(resumos.avaliacoes(any())).thenAnswer(chamada -> chamada.getArgument(0));

        mvc.perform(get(LISTAGEM).param("tipoConteudo", tipo.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comentario").value("Bom conteúdo"))
                .andExpect(jsonPath("$[0].nomeUsuario").value("Autora"))
                .andExpect(jsonPath("$[0].tipoConteudo").value(tipo.name()))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist())
                .andExpect(jsonPath("$[0].usuario").doesNotExist());

        mvc.perform(get(LISTAGEM + "/paginadas").param("tipoConteudo", tipo.name())
                        .param("pagina", "1").param("tamanho", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comentario").value("Bom conteúdo"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].usuarioId").doesNotExist())
                .andExpect(jsonPath("$.content[0].usuario").doesNotExist())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void visitanteNaoPodeLerAvaliacoesPessoaisNemEscrever() throws Exception {
        mvc.perform(get(ROTA + "/minhas")).andExpect(status().isUnauthorized());
        mvc.perform(get(ROTA + "/minhas/paginadas")).andExpect(status().isUnauthorized());
        mvc.perform(post(ROTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(ROTA + "/1")).andExpect(status().isUnauthorized());
        mvc.perform(patch(ROTA + "/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete(ROTA + "/1")).andExpect(status().isUnauthorized());
        mvc.perform(post(LISTAGEM)).andExpect(status().isUnauthorized());
        verifyNoInteractions(avaliacoes);
    }

    @Test
    void jwtInvalidoSoPermiteAcessoDeVisitante() throws Exception {
        mvc.perform(get(LISTAGEM).param("tipoConteudo", "TV").header("Authorization", "Bearer invalido"))
                .andExpect(status().isOk());
        mvc.perform(get(ROTA + "/minhas").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(ROTA).header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usuarios);
    }
}
