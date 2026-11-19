# TV Score — resumos das rotas no Swagger

O diff acrescenta 44 resumos curtos em português. Os 55 métodos de controller com mapeamento na versão de referência `3c34ed3` ficam com resumo. As descrições que já existiam, como “Trocar senha e revogar sessões”, foram preservadas.

## Prompt para o Codex no IntelliJ

```text
Leia TVScore_Resumos_Swagger.md. Confira HEAD e git status do backend e
mescle o diff preservando alterações locais. Adicione @Operation(summary)
aos endpoints sem resumo, com frases curtas em português como no exemplo
de troca de senha. Se houver rotas novas, use o mesmo estilo. Preserve
descrições existentes, caminhos, autenticação, payloads e status HTTP.
Rode a compilação e confira em /v3/api-docs se cada operação exposta tem
summary. Confira o Swagger local e mostre o diff antes de publicar.
```

## Diff para revisão

```diff
diff --git a/src/main/java/br/com/senai/tv_score/auth/AuthController.java b/src/main/java/br/com/senai/tv_score/auth/AuthController.java
index 63d5668..3c3dbb2 100644
--- a/src/main/java/br/com/senai/tv_score/auth/AuthController.java
+++ b/src/main/java/br/com/senai/tv_score/auth/AuthController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.auth;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import jakarta.validation.Valid;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
@@ -19,12 +21,14 @@ public class AuthController {
     }
 
     @PostMapping("/login")
+    @Operation(summary = "Entrar na conta e obter o token de acesso")
     public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
         LoginResponse response = authService.login(request);
         return ResponseEntity.ok(response);
     }
 
     @PostMapping("/cadastrar")
+    @Operation(summary = "Criar uma conta e obter o token de acesso")
     public ResponseEntity<LoginResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
         LoginResponse response = authService.cadastrar(request);
         return ResponseEntity.status(HttpStatus.CREATED).body(response);
diff --git a/src/main/java/br/com/senai/tv_score/auth/PaginaRedefinirSenhaController.java b/src/main/java/br/com/senai/tv_score/auth/PaginaRedefinirSenhaController.java
index be7fa79..5982763 100644
--- a/src/main/java/br/com/senai/tv_score/auth/PaginaRedefinirSenhaController.java
+++ b/src/main/java/br/com/senai/tv_score/auth/PaginaRedefinirSenhaController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.auth;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import org.springframework.core.io.ClassPathResource;
 import org.springframework.core.io.Resource;
 import org.springframework.http.CacheControl;
@@ -12,6 +14,7 @@ import org.springframework.web.bind.annotation.RestController;
 public class PaginaRedefinirSenhaController {
 
     @GetMapping("/redefinir-senha")
+    @Operation(summary = "Abrir a página de redefinição de senha")
     public ResponseEntity<Resource> pagina() {
         return ResponseEntity.ok()
                 .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
diff --git a/src/main/java/br/com/senai/tv_score/controller/AvaliacaoController.java b/src/main/java/br/com/senai/tv_score/controller/AvaliacaoController.java
index 383dd20..182d55f 100644
--- a/src/main/java/br/com/senai/tv_score/controller/AvaliacaoController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/AvaliacaoController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 
 import br.com.senai.tv_score.avaliacao.Avaliacao;
 import br.com.senai.tv_score.avaliacao.AvaliacaoService;
@@ -30,6 +32,7 @@ public class AvaliacaoController {
     }
 
     @GetMapping("/filme/{filmeSerieId}")
+    @Operation(summary = "Listar avaliações de um filme ou série")
     public ResponseEntity<List<AvaliacaoDTO>> listarPorFilme(
             @PathVariable Long filmeSerieId,
             @RequestParam("tipoConteudo") TipoConteudo tipoConteudo
@@ -43,11 +46,13 @@ public class AvaliacaoController {
     }
 
     @GetMapping("/minhas")
+    @Operation(summary = "Listar minhas avaliações")
     public ResponseEntity<List<AvaliacaoDTO>> listarMinhasAvaliacoes(@AuthenticationPrincipal Usuario usuario) {
         return ResponseEntity.ok(avaliacaoService.listarPorUsuario(usuario));
     }
 
     @PostMapping
+    @Operation(summary = "Registrar uma avaliação de filme ou série")
     public ResponseEntity<AvaliacaoDTO> avaliar(
             @AuthenticationPrincipal Usuario usuario,
             @Valid @RequestBody AvaliacaoDTO dto
@@ -56,6 +61,7 @@ public class AvaliacaoController {
         return ResponseEntity.status(HttpStatus.CREATED).body(salva);
     }
     @PutMapping("/{id}")
+    @Operation(summary = "Atualizar minha avaliação")
     public ResponseEntity<AvaliacaoDTO> atualizarAvaliacao(
             @AuthenticationPrincipal Usuario usuario,
             @PathVariable("id") Long id,
@@ -67,6 +73,7 @@ public class AvaliacaoController {
     }
 
     @DeleteMapping("/{id}")
+    @Operation(summary = "Excluir minha avaliação")
     public ResponseEntity<Void> removerAvaliacao(
             @AuthenticationPrincipal Usuario usuario,
             @PathVariable Long id
@@ -76,6 +83,7 @@ public class AvaliacaoController {
     }
 
     @GetMapping("/conteudo/{filmeSerieId}/resumo")
+    @Operation(summary = "Consultar o resumo das avaliações de um título")
     public ResponseEntity<ResumoAvaliacaoDTO> buscarResumo(
             @PathVariable("filmeSerieId") Long filmeSerieId,
             @RequestParam("tipoConteudo") TipoConteudo tipoConteudo
@@ -88,11 +96,13 @@ public class AvaliacaoController {
         );
     }
     @GetMapping("/minhas/paginadas")
+    @Operation(summary = "Listar minhas avaliações com paginação")
     public org.springframework.data.domain.Page<AvaliacaoDTO> paginados(@AuthenticationPrincipal Usuario usuario,
             @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
         return resumos.avaliacoes(avaliacaoService.listarPorUsuario(usuario, pagina, tamanho));
     }
     @GetMapping("/filme/{filmeSerieId}/paginadas")
+    @Operation(summary = "Listar avaliações de um título com paginação")
     public org.springframework.data.domain.Page<AvaliacaoDTO> porConteudoPaginado(@PathVariable Long filmeSerieId,
         @RequestParam TipoConteudo tipoConteudo, @RequestParam(defaultValue = "0") int pagina,
         @RequestParam(defaultValue = "20") int tamanho) {
diff --git a/src/main/java/br/com/senai/tv_score/controller/ConteudoController.java b/src/main/java/br/com/senai/tv_score/controller/ConteudoController.java
index e93ce53..56b3494 100644
--- a/src/main/java/br/com/senai/tv_score/controller/ConteudoController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/ConteudoController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.GeneroConteudoDTO;
 import br.com.senai.tv_score.dto.TmdbResponseDTO;
 import br.com.senai.tv_score.filme.FilmeService;
@@ -19,6 +21,7 @@ public class ConteudoController {
     }
 
     @GetMapping("/generos")
+    @Operation(summary = "Listar gêneros de filmes e séries")
     public ResponseEntity<List<GeneroConteudoDTO>> listarGeneros() {
         return ResponseEntity.ok(
                 filmeService.listarGenerosConteudo()
@@ -26,6 +29,7 @@ public class ConteudoController {
     }
 
     @GetMapping("/por-genero")
+    @Operation(summary = "Buscar filmes e séries por gênero")
     public ResponseEntity<TmdbResponseDTO> buscarPorGenero(
             @RequestParam("genero") String genero,
             @RequestParam(name = "pagina", defaultValue = "1")
diff --git a/src/main/java/br/com/senai/tv_score/controller/EpisodioController.java b/src/main/java/br/com/senai/tv_score/controller/EpisodioController.java
index f8db594..5fb863c 100644
--- a/src/main/java/br/com/senai/tv_score/controller/EpisodioController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/EpisodioController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.EpisodioDTO;
 import br.com.senai.tv_score.dto.PainelEpisodiosDTO;
 import br.com.senai.tv_score.dto.TemporadaDTO;
@@ -17,6 +19,7 @@ public class EpisodioController {
     }
 
     @GetMapping("/{serieId}/episodios/painel")
+    @Operation(summary = "Consultar o painel de episódios de uma série")
     public PainelEpisodiosDTO buscarPainel(
             @PathVariable("serieId") Long serieId,
             @RequestParam(name = "limite", defaultValue = "10")
@@ -35,6 +38,7 @@ public class EpisodioController {
     }
 
     @GetMapping("/{serieId}/temporadas/{temporada}")
+    @Operation(summary = "Consultar uma temporada e seus episódios")
     public TemporadaDTO buscarTemporada(
             @PathVariable("serieId") Long serieId,
             @PathVariable("temporada") int temporada
@@ -46,6 +50,7 @@ public class EpisodioController {
     }
 
     @GetMapping("/{serieId}/temporadas/{temporada}/episodios/{episodio}")
+    @Operation(summary = "Consultar os detalhes de um episódio")
     public EpisodioDTO buscarDetalhes(
             @PathVariable("serieId") Long serieId,
             @PathVariable("temporada") int temporada,
@@ -59,6 +64,7 @@ public class EpisodioController {
     }
 
     @GetMapping("/{serieId}/episodios/grade")
+    @Operation(summary = "Consultar a grade de episódios de uma série")
     public br.com.senai.tv_score.dto.GradeEpisodiosDTO grade(@PathVariable Long serieId,
             @RequestParam(defaultValue = "false") boolean incluirEspeciais) {
         var temporadas = episodioService.carregarTemporadas(serieId, incluirEspeciais);
@@ -68,6 +74,7 @@ public class EpisodioController {
                 e.temporada(), e.numero(), e.nota(), e.quantidadeVotos(), e.dataExibicao())).toList());
     }
     @GetMapping("/{serieId}/episodios/ranking")
+    @Operation(summary = "Consultar o ranking de episódios de uma série")
     public java.util.List<EpisodioDTO> ranking(@PathVariable Long serieId,
             @RequestParam(defaultValue = "10") int limite,
             @RequestParam(defaultValue = "1") int minimoVotos,
diff --git a/src/main/java/br/com/senai/tv_score/controller/FavoritoController.java b/src/main/java/br/com/senai/tv_score/controller/FavoritoController.java
index f095a54..ae7b7a5 100644
--- a/src/main/java/br/com/senai/tv_score/controller/FavoritoController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/FavoritoController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.FavoritoDTO;
 import br.com.senai.tv_score.exceptions.ErroResponse;
 import br.com.senai.tv_score.favorito.FavoritoService;
@@ -26,11 +28,13 @@ public class FavoritoController {
     }
 
     @GetMapping
+    @Operation(summary = "Listar meus favoritos")
     public ResponseEntity<List<FavoritoDTO>> listarFavoritos(@AuthenticationPrincipal Usuario usuario) {
         return ResponseEntity.ok(favoritoService.listarFavoritos(usuario));
     }
 
     @PostMapping
+    @Operation(summary = "Adicionar um filme ou série aos favoritos")
     public ResponseEntity<FavoritoDTO> adicionarFavorito(
             @AuthenticationPrincipal Usuario usuario,
             @Valid @RequestBody FavoritoDTO dto
@@ -40,6 +44,7 @@ public class FavoritoController {
     }
 
     @DeleteMapping("/{filmeSerieId}")
+    @Operation(summary = "Remover um filme ou série dos favoritos")
     public ResponseEntity<Void> removerFavorito(
             @AuthenticationPrincipal Usuario usuario,
             @PathVariable Long filmeSerieId,
@@ -54,6 +59,7 @@ public class FavoritoController {
         return ResponseEntity.noContent().build();
     }
     @GetMapping("/paginados")
+    @Operation(summary = "Listar meus favoritos com paginação")
     public org.springframework.data.domain.Page<FavoritoDTO> paginados(@AuthenticationPrincipal Usuario usuario,
             @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
         return resumos.favoritos(favoritoService.listarFavoritos(usuario, pagina, tamanho));
diff --git a/src/main/java/br/com/senai/tv_score/controller/FilmeController.java b/src/main/java/br/com/senai/tv_score/controller/FilmeController.java
index 9c0dd13..9db0e29 100644
--- a/src/main/java/br/com/senai/tv_score/controller/FilmeController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/FilmeController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.FilmeDTO;
 import br.com.senai.tv_score.dto.TmdbResponseDTO;
 import br.com.senai.tv_score.filme.FilmeService;
@@ -21,6 +23,7 @@ public class FilmeController {
 
     // Rota: GET /api/filmes/em-alta
     @GetMapping("/em-alta")
+    @Operation(summary = "Listar filmes em alta")
     public ResponseEntity<TmdbResponseDTO> getFilmesEmAlta() {
         TmdbResponseDTO response = filmeService.buscarFilmesEmAlta();
         return ResponseEntity.ok(response);
@@ -28,6 +31,7 @@ public class FilmeController {
 
     // Rota: GET /api/filmes/pesquisar?q=Batman
     @GetMapping("/pesquisar")
+    @Operation(summary = "Pesquisar filmes por título")
     public ResponseEntity<TmdbResponseDTO> pesquisarFilmes(
             @RequestParam("q") String query,
             @RequestParam(name = "pagina", defaultValue = "1") int pagina
@@ -39,6 +43,7 @@ public class FilmeController {
 
     // Rota: GET /api/filmes/123
     @GetMapping("/{id}")
+    @Operation(summary = "Consultar detalhes e onde assistir a um filme")
     public ResponseEntity<FilmeDTO> getDetalhesFilme(@PathVariable Long id) {
         FilmeDTO filme = filmeService.buscarDetalhesFilme(id);
         return ResponseEntity.ok(filme);
@@ -46,12 +51,14 @@ public class FilmeController {
 
     // GET /api/filmes/generos
     @GetMapping("/generos")
+    @Operation(summary = "Listar gêneros de filmes")
     public ResponseEntity<GeneroResponseDTO> listarGeneros() {
         return ResponseEntity.ok(filmeService.listarGeneros());
     }
 
     // GET /api/filmes/generos/{generoId}?pagina=1
     @GetMapping("/generos/{generoId}")
+    @Operation(summary = "Buscar filmes por gênero")
     public ResponseEntity<TmdbResponseDTO> buscarFilmesPorGenero(
             @PathVariable("generoId") Long generoId,
             @RequestParam(name = "pagina", defaultValue = "1") int pagina
@@ -63,6 +70,7 @@ public class FilmeController {
 
     // GET /api/filmes/streamings
     @GetMapping("/streamings")
+    @Operation(summary = "Listar plataformas de streaming de filmes")
     public ResponseEntity<StreamingResponseDTO> listarStreamings() {
         return ResponseEntity.ok(
                 filmeService.listarStreamings()
@@ -71,6 +79,7 @@ public class FilmeController {
 
     // GET /api/filmes/streaming/{streamingId}?pagina=1
     @GetMapping("/streaming/{streamingId}")
+    @Operation(summary = "Listar filmes de uma plataforma de streaming")
     public ResponseEntity<TmdbResponseDTO> buscarPorStreaming(
             @PathVariable("streamingId") Long streamingId,
             @RequestParam(name = "pagina", defaultValue = "1") int pagina
@@ -82,6 +91,7 @@ public class FilmeController {
 
     // GET /api/filmes/por-nota?minima=7&maxima=10&pagina=1
     @GetMapping("/por-nota")
+    @Operation(summary = "Listar filmes por nota")
     public ResponseEntity<TmdbResponseDTO> buscarPorNota(
             @RequestParam(name = "minima", defaultValue = "0") double minima,
             @RequestParam(name = "maxima", defaultValue = "10") double maxima,
@@ -94,6 +104,7 @@ public class FilmeController {
 
     // GET /api/filmes/por-ano/2024?pagina=1
     @GetMapping("/por-ano/{ano}")
+    @Operation(summary = "Listar filmes por ano de lançamento")
     public ResponseEntity<TmdbResponseDTO> buscarPorAno(
             @PathVariable("ano") int ano,
             @RequestParam(name = "pagina", defaultValue = "1") int pagina
@@ -105,6 +116,7 @@ public class FilmeController {
 
     // GET /api/filmes/catalogo?pagina=1
     @GetMapping("/catalogo")
+    @Operation(summary = "Consultar o catálogo de filmes")
     public ResponseEntity<TmdbResponseDTO> buscarCatalogo(
             @RequestParam(name = "pagina", defaultValue = "1") int pagina
     ) {
diff --git a/src/main/java/br/com/senai/tv_score/controller/FotoController.java b/src/main/java/br/com/senai/tv_score/controller/FotoController.java
index e47c9da..e6eaf00 100644
--- a/src/main/java/br/com/senai/tv_score/controller/FotoController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/FotoController.java
@@ -1,4 +1,6 @@
 package br.com.senai.tv_score.controller;
+
+import io.swagger.v3.oas.annotations.Operation;
 import br.com.senai.tv_score.usuarios.*;
 import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
 import org.springframework.web.bind.annotation.*;
@@ -15,5 +17,6 @@ public class FotoController {
         return new UsuarioLogadoResponse(usuario.getNome(),usuario.getEmail(),fotos.atualizar(usuario,arquivo));
     }
     @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
+    @Operation(summary = "Remover minha foto de perfil")
     public void remover(@AuthenticationPrincipal Usuario usuario) { fotos.remover(usuario); }
 }
diff --git a/src/main/java/br/com/senai/tv_score/controller/FotoLocalController.java b/src/main/java/br/com/senai/tv_score/controller/FotoLocalController.java
index 487f1df..d30574e 100644
--- a/src/main/java/br/com/senai/tv_score/controller/FotoLocalController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/FotoLocalController.java
@@ -1,4 +1,6 @@
 package br.com.senai.tv_score.controller;
+
+import io.swagger.v3.oas.annotations.Operation;
 import org.springframework.web.bind.annotation.*;
 import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
 import org.springframework.beans.factory.annotation.Value;
@@ -12,6 +14,7 @@ public class FotoLocalController {
     private final Path raiz;
     public FotoLocalController(@Value("${foto.local.diretorio:./fotos-dev}") String diretorio){raiz=Path.of(diretorio).toAbsolutePath().normalize();}
     @GetMapping("/api/fotos/{chave}")
+    @Operation(summary = "Consultar uma foto armazenada localmente")
     public ResponseEntity<FileSystemResource> ler(@PathVariable String chave) {
         if(!chave.matches("[a-f0-9-]{36}\\.png"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Foto não encontrada.");
         Path arquivo=raiz.resolve(chave).normalize();
diff --git a/src/main/java/br/com/senai/tv_score/controller/PreferenciasUsuarioController.java b/src/main/java/br/com/senai/tv_score/controller/PreferenciasUsuarioController.java
index ff69e85..ae729ba 100644
--- a/src/main/java/br/com/senai/tv_score/controller/PreferenciasUsuarioController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/PreferenciasUsuarioController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.PreferenciasGenerosRequestDTO;
 import br.com.senai.tv_score.dto.PreferenciasGenerosResponseDTO;
 import br.com.senai.tv_score.usuarios.PreferenciasUsuarioService;
@@ -22,6 +24,7 @@ public class PreferenciasUsuarioController {
     }
 
     @GetMapping
+    @Operation(summary = "Consultar meus gêneros preferidos")
     public ResponseEntity<PreferenciasGenerosResponseDTO> consultar(
             @AuthenticationPrincipal Usuario usuario
     ) {
@@ -31,6 +34,7 @@ public class PreferenciasUsuarioController {
     }
 
     @PutMapping
+    @Operation(summary = "Salvar meus gêneros preferidos")
     public ResponseEntity<PreferenciasGenerosResponseDTO> salvar(
             @AuthenticationPrincipal Usuario usuario,
             @Valid @RequestBody PreferenciasGenerosRequestDTO dto
diff --git a/src/main/java/br/com/senai/tv_score/controller/RecomendacaoController.java b/src/main/java/br/com/senai/tv_score/controller/RecomendacaoController.java
index 4f55ac4..3992a60 100644
--- a/src/main/java/br/com/senai/tv_score/controller/RecomendacaoController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/RecomendacaoController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.RecomendacoesDTO;
 import br.com.senai.tv_score.recomendacao.RecomendacaoService;
 import org.springframework.http.CacheControl;
@@ -20,6 +22,7 @@ public class RecomendacaoController {
     }
 
     @GetMapping("/filmes")
+    @Operation(summary = "Obter recomendações personalizadas de filmes")
     public ResponseEntity<RecomendacoesDTO> buscarFilmes(
             @RequestHeader(HttpHeaders.AUTHORIZATION)
             String authorization
@@ -29,6 +32,7 @@ public class RecomendacaoController {
                 .body(recomendacaoService.buscarFilmes(authorization));
     }
     @GetMapping("/series")
+    @Operation(summary = "Obter recomendações personalizadas de séries")
     public ResponseEntity<RecomendacoesDTO> buscarSeries(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
         return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recomendacaoService.buscarSeries(authorization));
     }
diff --git a/src/main/java/br/com/senai/tv_score/controller/SerieController.java b/src/main/java/br/com/senai/tv_score/controller/SerieController.java
index d6a580d..a7b1dee 100644
--- a/src/main/java/br/com/senai/tv_score/controller/SerieController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/SerieController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.dto.FilmeDTO;
 import br.com.senai.tv_score.dto.TmdbResponseDTO;
 import br.com.senai.tv_score.filme.FilmeService;
@@ -17,6 +19,7 @@ public class SerieController {
     }
 
     @GetMapping("/em-alta")
+    @Operation(summary = "Listar séries em alta")
     public ResponseEntity<TmdbResponseDTO> buscarEmAlta(
             @RequestParam(name = "pagina", defaultValue = "1")
             int pagina
@@ -28,6 +31,7 @@ public class SerieController {
 
 
 @GetMapping("/por-nota")
+@Operation(summary = "Listar séries por nota")
 public ResponseEntity<TmdbResponseDTO> buscarSeriesPorNota(
         @RequestParam(name = "minima", defaultValue = "0") double minima,
         @RequestParam(name = "maxima", defaultValue = "10") double maxima,
@@ -40,6 +44,7 @@ public ResponseEntity<TmdbResponseDTO> buscarSeriesPorNota(
 
 
     @GetMapping("/{id}")
+    @Operation(summary = "Consultar os detalhes de uma série")
     public ResponseEntity<FilmeDTO> buscarDetalhes(
             @PathVariable("id") Long id
     ) {
diff --git a/src/main/java/br/com/senai/tv_score/controller/UsuariosController.java b/src/main/java/br/com/senai/tv_score/controller/UsuariosController.java
index 01dc390..7332f00 100644
--- a/src/main/java/br/com/senai/tv_score/controller/UsuariosController.java
+++ b/src/main/java/br/com/senai/tv_score/controller/UsuariosController.java
@@ -1,5 +1,7 @@
 package br.com.senai.tv_score.controller;
 
+import io.swagger.v3.oas.annotations.Operation;
+
 import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
 import br.com.senai.tv_score.dto.AtualizarUsuarioDTO;
 import br.com.senai.tv_score.dto.ExcluirContaDTO;
@@ -23,6 +25,7 @@ public class UsuariosController {
     }
 
     @GetMapping("/eu")
+    @Operation(summary = "Consultar meu perfil")
     public UsuarioLogadoResponse usuarioLogado(
             @AuthenticationPrincipal Usuario usuario
     ) {
@@ -33,6 +36,7 @@ public class UsuariosController {
     }
 
     @PutMapping("/eu")
+    @Operation(summary = "Atualizar meu perfil")
     public UsuarioLogadoResponse atualizarUsuario(
             @AuthenticationPrincipal Usuario usuario,
             @Valid @RequestBody AtualizarUsuarioDTO dto
@@ -42,6 +46,7 @@ public class UsuariosController {
 
     @DeleteMapping("/eu")
     @ResponseStatus(HttpStatus.NO_CONTENT)
+    @Operation(summary = "Excluir minha conta")
     public void excluirConta(
             @AuthenticationPrincipal Usuario usuario,
             @Valid @RequestBody ExcluirContaDTO dto
```

## Verificação

Foram conferidos os mapeamentos e o conteúdo do diff, que contém apenas adições de imports e anotações. O Maven parou antes da compilação por falha de acesso ao Maven Central. Confira o resultado no Swagger local após aplicar no seu checkout.
