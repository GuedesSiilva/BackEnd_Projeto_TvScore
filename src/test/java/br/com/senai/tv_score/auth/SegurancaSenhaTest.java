package br.com.senai.tv_score.auth;
import br.com.senai.tv_score.usuarios.*;
import br.com.senai.tv_score.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
@SpringBootTest
class SegurancaSenhaTest {
    @Autowired UsuarioRepository usuarios;
    @Autowired SenhaService senhas;
    @Autowired RecuperacaoService recuperacao;
    @Autowired ResetSenhaRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired FotoService fotos;
    @Autowired PlatformTransactionManager transacoes;
    @MockitoBean EmailRecuperacao email;
    @Autowired org.springframework.web.context.WebApplicationContext contexto;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("springSecurityFilterChain") jakarta.servlet.Filter seguranca;
    Usuario u;
    @BeforeEach void criar() { u=usuarios.saveAndFlush(new Usuario("Teste",java.util.UUID.randomUUID()+"@example.test",encoder.encode("antiga123"))); }
    @ParameterizedTest
    @ValueSource(strings = {"https://tvscore.pages.dev", "http://localhost:5173", "http://127.0.0.1:5173"})
    void preflightPermiteFlutterWebSemJwt(String origem) throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(seguranca).build();
        mvc.perform(options("/api/usuarios/eu")
                        .header("Origin", origem)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origem));
    }

    @Test void preflightBloqueiaOutroProjetoPages() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(seguranca).build();
        mvc.perform(options("/api/usuarios/eu")
                        .header("Origin", "https://outro-projeto.pages.dev")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void trocaRevogaTokensEValidaSemMutar() {
        String anterior=jwt.gerarToken(u.getId());
        assertThrows(ResponseStatusException.class,()->senhas.alterar(u,new AlterarSenhaRequest("errada","nova12345")));
        assertEquals(0,usuarios.findById(u.getId()).orElseThrow().getVersaoCredenciais());
        assertThrows(IllegalArgumentException.class,()->senhas.alterar(u,new AlterarSenhaRequest("antiga123","antiga123")));
        senhas.alterar(u,new AlterarSenhaRequest("antiga123","nova12345"));
        var salvo=usuarios.findById(u.getId()).orElseThrow();
        assertTrue(encoder.matches("nova12345",salvo.getSenha()));
        assertNotEquals(u.getSenha(),salvo.getSenha());
        assertFalse(jwt.versaoValida(anterior,salvo.getVersaoCredenciais()));
        assertTrue(jwt.versaoValida(jwt.gerarToken(u.getId(),salvo.getVersaoCredenciais()),salvo.getVersaoCredenciais()));
    }
    @Test void limitesUtf8SemTruncarOuRemoverEspacos() {
        assertThrows(IllegalArgumentException.class,()->PoliticaSenha.validar("á".repeat(37)));
        PoliticaSenha.validar("á".repeat(36)); PoliticaSenha.validar("        ");
        assertThrows(IllegalArgumentException.class,()->PoliticaSenha.validar("1234567"));
    }
    private String emitir(String ip) {
        recuperacao.solicitar(u.getEmail(),ip);
        var captor=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email).enviar(eq(u.getEmail()),captor.capture(),any(Instant.class));
        return captor.getValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"á#*12", "𐐀abcd", "\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0"})
    void resetRejeitaSenhaCurtaOuEmBranco(String senha) throws Exception {
        verificarSenhaDeRedefinicao(senha, "A senha deve ter pelo menos 6 caracteres e não pode conter apenas espaços.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12😀", "ABC1#️⃣", "ABC12©️"})
    void resetRejeitaEmojisSemConsumirToken(String senha) throws Exception {
        verificarSenhaDeRedefinicao(senha, "Emojis não são permitidos na senha.");
    }

    @Test void resetRejeitaSenhaAcimaDoLimiteDoBcrypt() throws Exception {
        verificarSenhaDeRedefinicao("á".repeat(36) + "a",
                "A senha está muito longa. Escolha uma senha mais curta.");
    }

    @Test void resetAceitaSeisCaracteresComAcentoESimbolos() throws Exception {
        verificarSenhaDeRedefinicao("𐐀á#*12", null);
    }

    @Test void resetAceitaSenhaNoLimiteDoBcrypt() throws Exception {
        verificarSenhaDeRedefinicao("á".repeat(36), null);
    }

    private void verificarSenhaDeRedefinicao(String senha, String mensagemEsperada) throws Exception {
        String identificador = java.util.UUID.randomUUID().toString();
        String token = emitir(identificador);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(seguranca).build();
        String corpo = "{\"token\":\"%s\",\"novaSenha\":\"%s\"}".formatted(token, senha);
        var resposta = mvc.perform(post("/api/auth/senha/redefinicao")
                .with(request -> { request.setRemoteAddr(identificador); return request; })
                .contentType("application/json").content(corpo));
        if (mensagemEsperada == null) {
            resposta.andExpect(status().isNoContent());
            var auth = new AuthService(usuarios, encoder, jwt, fotos);
            assertNotNull(auth.login(new LoginRequest(u.getEmail(), senha)).token());
        } else {
            resposta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value(mensagemEsperada));
            assertNull(tokens.findByHash(RecuperacaoService.hash(token)).orElseThrow().getConsumidoEm());
        }
    }
    @Test void resetGuardaSomenteHashEImpedeReplay() throws Exception {
        String token=emitir("127.1.0.1");
        var registro=tokens.findByHash(RecuperacaoService.hash(token)).orElseThrow();
        assertNotEquals(token,registro.getHash());
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(seguranca).build();
        String corpo = "{\"token\":\"%s\",\"novaSenha\":\"nova12345\"}".formatted(token);
        mvc.perform(post("/api/auth/senha/redefinicao").contentType("application/json").content(corpo))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/senha/redefinicao").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest());
        assertEquals(1,usuarios.findById(u.getId()).orElseThrow().getVersaoCredenciais());
    }
    @Test void soEnfileiraDepoisDoCommitExterno() {
        var tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            recuperacao.solicitar(u.getEmail(), "127.4.0.1");
            verifyNoInteractions(email);
            status.setRollbackOnly();
        });
        verifyNoInteractions(email);
        tx.executeWithoutResult(status -> {
            recuperacao.solicitar(u.getEmail(), "127.4.0.2");
            verifyNoInteractions(email);
        });
        var bruto = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email).enviar(eq(u.getEmail()), bruto.capture(), any(Instant.class));
        assertTrue(tokens.findByHash(RecuperacaoService.hash(bruto.getValue())).isPresent());
    }

    @Test void falhaAposConsumoReverteTokenESenha() {
        String token = emitir("127.5.0.1");
        assertThrows(IllegalArgumentException.class,
                () -> recuperacao.redefinir(token, "antiga123", "127.5.0.2"));
        var registro = tokens.findByHash(RecuperacaoService.hash(token)).orElseThrow();
        assertNull(registro.getConsumidoEm());
        var usuario = usuarios.findById(u.getId()).orElseThrow();
        assertTrue(encoder.matches("antiga123", usuario.getSenha()));
        assertEquals(0, usuario.getVersaoCredenciais());
        recuperacao.redefinir(token, "nova12345", "127.5.0.3");
        assertTrue(encoder.matches("nova12345", usuarios.findById(u.getId()).orElseThrow().getSenha()));
    }
    @Test void apenasUmConsumoConcorrente() throws Exception {
        String token=emitir("127.2.0.1");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var iniciar=new CountDownLatch(1);
            Callable<Boolean> tarefa=()->{iniciar.await();try{recuperacao.redefinir(token,"nova12345",Thread.currentThread().getName());return true;}catch(ResponseStatusException ex){return false;}};
            var a=pool.submit(tarefa);var b=pool.submit(tarefa);iniciar.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,usuarios.findById(u.getId()).orElseThrow().getVersaoCredenciais());
    }
    @Test void desconhecidoNaoEnviaEExpiradoNaoFunciona() {
        recuperacao.solicitar("ausente@example.test","127.3.0.1"); verifyNoInteractions(email);
        String token=emitir("127.3.0.2");
        var registro=tokens.findByHash(RecuperacaoService.hash(token)).orElseThrow();
        registro.setExpiraEm(java.time.Instant.now().minusSeconds(1));tokens.saveAndFlush(registro);
        assertThrows(ResponseStatusException.class,()->recuperacao.redefinir(token,"nova12345","127.3.0.3"));
    }
    @Test void limiteConfigurado() {
        var limite=new LimiteRecuperacao(2,900);limite.verificar("teste");limite.verificar("teste");
        assertEquals(429,assertThrows(ResponseStatusException.class,()->limite.verificar("teste")).getStatusCode().value());
    }
    @Test void filtroRealRecusaSessaoAnteriorEPermiteNovoLogin() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguranca).build();
        String antigo=jwt.gerarToken(u.getId());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usuarios/eu").header("Authorization","Bearer "+antigo))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/usuarios/eu/senha").header("Authorization","Bearer "+antigo)
                .contentType("application/json").content("{\"senhaAtual\":\"antiga123\",\"novaSenha\":\"nova12345\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usuarios/eu").header("Authorization","Bearer "+antigo))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.mensagem").exists());
        var auth=new AuthService(usuarios,encoder,jwt,fotos);
        var login=auth.login(new LoginRequest(u.getEmail(),"nova12345"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usuarios/eu").header("Authorization","Bearer "+login.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
    @Test void trocaConcorrenteNaoSobrescreveHash() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var iniciar=new CountDownLatch(1);
            Callable<Boolean> tarefa=()->{iniciar.await();try{senhas.alterar(u,new AlterarSenhaRequest("antiga123","nova12345"));return true;}catch(ResponseStatusException ex){return false;}};
            var a=pool.submit(tarefa);var b=pool.submit(tarefa);iniciar.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,usuarios.findById(u.getId()).orElseThrow().getVersaoCredenciais());
    }
    @Test void recuperacaoPublicaMesmoStatusECorpoParaExistenteEDesconhecido() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguranca).build();
        var conhecido=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/senha/recuperacao")
            .contentType("application/json").content("{\"email\":\""+u.getEmail()+"\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted()).andReturn().getResponse().getContentAsString();
        var desconhecido=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/senha/recuperacao")
            .contentType("application/json").content("{\"email\":\"desconhecido-publico@example.test\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertEquals(conhecido,desconhecido);
    }
}
