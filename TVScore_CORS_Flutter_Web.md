# TV Score — CORS para o Flutter Web

Alteração preparada sobre o backend Java no commit `790a1a9`. A configuração existente já integra CORS ao Spring Security para `/api/**`. O diff permite a origem exata `https://tvscore.pages.dev` e mantém as origens locais. O JWT continua obrigatório nas rotas protegidas.

## Prompt para o Codex do IntelliJ

```text
Leia TVScore_CORS_Flutter_Web.md. Confira o HEAD e o git status local,
preserve minhas mudanças e mescle o diff no backend Java. Reaproveite
CorsConfig e a configuração de CORS existente do Spring Security.
Rode os testes de preflight no perfil de teste, usando o banco de teste.
Mostre o diff para revisão e informe quais testes foram executados.
Mantenha RESET_URL no endereço HTTPS da página de redefinição atual.
```

## Diff

```diff
diff --git a/src/main/java/br/com/senai/tv_score/config/CorsConfig.java b/src/main/java/br/com/senai/tv_score/config/CorsConfig.java
index e0fae21..841fdda 100644
--- a/src/main/java/br/com/senai/tv_score/config/CorsConfig.java
+++ b/src/main/java/br/com/senai/tv_score/config/CorsConfig.java
@@ -15,6 +15,7 @@ public class CorsConfig {
             public void addCorsMappings(CorsRegistry registry){
                 registry.addMapping("/api/**")
                         .allowedOriginPatterns(
+                                "https://tvscore.pages.dev",
                                 "http://localhost:*",
                                 "http://127.0.0.1:*"
                         )
diff --git a/src/test/java/br/com/senai/tv_score/auth/SegurancaSenhaTest.java b/src/test/java/br/com/senai/tv_score/auth/SegurancaSenhaTest.java
index 9c60ca2..5e30a5b 100644
--- a/src/test/java/br/com/senai/tv_score/auth/SegurancaSenhaTest.java
+++ b/src/test/java/br/com/senai/tv_score/auth/SegurancaSenhaTest.java
@@ -15,7 +15,9 @@ import org.junit.jupiter.params.ParameterizedTest;
 import org.junit.jupiter.params.provider.ValueSource;
 import static org.junit.jupiter.api.Assertions.*;
 import static org.mockito.Mockito.*;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
 import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
 import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
 @SpringBootTest
@@ -33,6 +35,29 @@ class SegurancaSenhaTest {
     @Autowired @org.springframework.beans.factory.annotation.Qualifier("springSecurityFilterChain") jakarta.servlet.Filter seguranca;
     Usuario u;
     @BeforeEach void criar() { u=usuarios.saveAndFlush(new Usuario("Teste",java.util.UUID.randomUUID()+"@example.test",encoder.encode("antiga123"))); }
+    @ParameterizedTest
+    @ValueSource(strings = {"https://tvscore.pages.dev", "http://localhost:5173", "http://127.0.0.1:5173"})
+    void preflightPermiteFlutterWebSemJwt(String origem) throws Exception {
+        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
+                .addFilters(seguranca).build();
+        mvc.perform(options("/api/usuarios/eu")
+                        .header("Origin", origem)
+                        .header("Access-Control-Request-Method", "GET")
+                        .header("Access-Control-Request-Headers", "authorization"))
+                .andExpect(status().isOk())
+                .andExpect(header().string("Access-Control-Allow-Origin", origem));
+    }
+
+    @Test void preflightBloqueiaOutroProjetoPages() throws Exception {
+        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
+                .addFilters(seguranca).build();
+        mvc.perform(options("/api/usuarios/eu")
+                        .header("Origin", "https://outro-projeto.pages.dev")
+                        .header("Access-Control-Request-Method", "GET"))
+                .andExpect(status().isForbidden())
+                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
+    }
+
     @Test void trocaRevogaTokensEValidaSemMutar() {
         String anterior=jwt.gerarToken(u.getId());
         assertThrows(ResponseStatusException.class,()->senhas.alterar(u,new AlterarSenhaRequest("errada","nova12345")));
```

## Validação após publicar

No Flutter Web publicado, a base da API deve apontar para `https://tvscore-backend.onrender.com`. Verifique se o login funciona e se o navegador consegue acessar `GET /api/usuarios/eu` com Bearer. Confira também cadastro, favoritos, avaliações, recomendações e a solicitação de recuperação com conta de teste. Atualização de página, navegação, favicon e metadados são itens do frontend.

Para conferir CORS após o deploy, rode:

```cmd
curl.exe -i -X OPTIONS "https://tvscore-backend.onrender.com/api/usuarios/eu" -H "Origin: https://tvscore.pages.dev" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: authorization"
```

O esperado é uma resposta 2xx com `Access-Control-Allow-Origin: https://tvscore.pages.dev`. O preflight não leva JWT. A requisição real continua precisando dele.

O construtor de EmailRecuperacao rejeita fragmentos no RESET_URL. Mantenha `https://tvscore-backend.onrender.com/redefinir-senha`. Migrar o formulário de redefinição para o Flutter exige confirmar a rota e a leitura do token no roteador antes de mudar a URL. O CORS necessário para o Flutter pedir o envio do e-mail já está coberto pelo mapeamento.

O diff passou em `git diff --check`. O Maven não chegou aos testes porque a resolução DNS do Maven Central falhou. Os testes do navegador no domínio publicado ainda precisam ser executados.
