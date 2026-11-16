# Contratos Flutter — evolução de 16/11/2026

Campos são aditivos; preservar tolerância a campos desconhecidos e `null`. Identidade de conteúdo é sempre `(id, tipoConteudo)`, com `MOVIE` ou `TV`. Favoritos e preferidos do onboarding continuam distintos. Gêneros vêm do catálogo dinâmico; limites atuais do onboarding foram preservados.

## Conta e sessão

`PUT /api/usuarios/eu/senha`, Bearer obrigatório:

```json
{"senhaAtual":"senha anterior","novaSenha":"nova senha segura"}
```

204 sem corpo: apagar token/dados de sessão e apresentar login. Todos os JWT anteriores, inclusive os de outros aparelhos, deixam de funcionar. 403: `{"mensagem":"Não foi possível alterar a senha."}`. 400 para nova senha igual à atual ou fora de 8–72 **bytes UTF-8**; não aplicar trim/strip. 401 sem sessão válida. Não enviar ID do usuário. Login de senhas antigas permanece aceito; a política nova vale para troca/reset. JWT continua `usuario:ID`, com 30 dias configuráveis; claim `cv` ausente equivale a zero somente enquanto a versão persistida também for zero. Tokens por e-mail são recusados.

`POST /api/auth/senha/recuperacao`, público:

```json
{"email":"usuario@exemplo.com"}
```

202, conhecido ou desconhecido:

```json
{"mensagem":"Se o e-mail estiver cadastrado, enviaremos as instruções de recuperação."}
```

429 tem `Retry-After` e `{"mensagem":"Tente novamente mais tarde."}`. Não interpretar 202 como confirmação de entrega. Desenvolver a tela/deep link de redefinição; ela **ainda não foi implementada no Flutter**. URL local padrão: `http://localhost:3000/redefinir-senha?token=...`. Links são construídos com `RESET_URL`, nunca com Host da requisição. Abrir link via GET não altera senha nem consome token.

Na tela, enviar somente após confirmação do usuário:

`POST /api/auth/senha/redefinicao`:

```json
{"token":"token opaco recebido no link","novaSenha":"nova senha segura"}
```

204 sem corpo: apresentar login, sem autenticação automática. 400 para token inválido/expirado/consumido: `{"mensagem":"Token de recuperação inválido ou expirado."}`. Validação de senha também pode produzir 400. Validade padrão: 20 minutos, uso único; novo pedido invalida anteriores. Não registrar token/link/senha em analytics ou logs do app.

## Foto e avatar

`PUT /api/usuarios/eu/foto`, Bearer, multipart com campo **arquivo** (sem ID ou URL de destino). JPEG/PNG, até 5 MiB, no máximo 10.000 por dimensão e 20 milhões de pixels. Reencoda sem metadados em PNG, preservando proporção, lado máximo 512. Nome original não é usado como chave.

200:

```json
{"nome":"Maria","email":"maria@exemplo.com","fotoUrl":"https://cdn.exemplo.com/chave-aleatoria.png"}
```

`DELETE /api/usuarios/eu/foto`: 204, idempotente. Perfil `GET|PUT /api/usuarios/eu`, login e cadastro agora incluem `fotoUrl: null` ou URL. Fotos são publicamente legíveis pela política escolhida; escritas exigem o dono autenticado. Mudança de foto gera URL nova, dispensando cache-busting. Foto antiga é removida de modo assíncrono após confirmação; pode continuar acessível durante o prazo de limpeza/cache do CDN. No modo local, a própria API serve `/api/fotos/{chave}`; não usar disco efêmero em produção.

Erros: 415 MIME não suportado, 413 tamanho, 400 imagem/dimensões, 503 storage indisponível; todos com `mensagem`. Nunca usar campos de avatar enviados junto à avaliação como autoridade.

## Avaliações e favoritos

DTO de avaliação mantém seus campos e acrescenta foto/resumo/estado:

```json
{
  "id":21,"filmeSerieId":550,"tipoConteudo":"MOVIE","nota":9.0,
  "comentario":"Gostei","nomeUsuario":"Maria","dataCriacao":"2026-11-16T10:00:00",
  "fotoUsuarioUrl":null,
  "conteudo":{"id":550,"tipoConteudo":"MOVIE","titulo":"Clube da Luta","posterPath":"/exemplo.jpg","dataLancamento":"1999-10-15","notaTmdb":8.4},
  "estadoConteudo":"DISPONIVEL"
}
```

Favoritos mantêm `id`, `filmeSerieId`, `dataAdicionado`, `tipoConteudo`, acrescentando `conteudo` e `estadoConteudo`. Resumo é somente saída. Não substituir `nota` do usuário por `notaTmdb`. `posterPath` é relativo; para cards, usar `https://image.tmdb.org/t/p/w500` + path quando não nulo. `fotoUsuarioUrl: null` usa avatar padrão.

Enriquecem por padrão apenas `/api/avaliacoes/minhas/paginadas`, `/api/avaliacoes/filme/{id}/paginadas` e `/api/favoritos/paginados`. SQL termina antes da rede. Máximo de 20 chaves únicas por página/operação, quatro threads, fila 64, prazo 5 segundos; itens além do orçamento podem ter resumo nulo. `DISPONIVEL`: cache/valor conhecido; pode ser último valor conhecido em falha. `INDISPONIVEL`: resumo não disponível nesta operação (inclui 404, falha, saturação ou orçamento; não significa registro removido). `NAO_SOLICITADO`: rota leve/gravação. Preservar o card e permitir fallback/retry quando nulo.

`/api/avaliacoes/minhas`, `/api/avaliacoes/filme/{id}` e `GET /api/favoritos` continuam listas completas e leves para compatibilidade/Python; não disparam enriquecimento. Gravações não dependem de sucesso do TMDB. Duas consultas paginadas de avaliações usam `dataCriacao DESC, id DESC`, página zero, tamanho padrão 20, máximo 100. Editar não muda a criação. O envelope Page legado continua igual.

## Pesquisa por cursor e gênero ordenado

Rotas novas:

```text
GET /api/conteudos/pesquisa?q=Batman&tamanho=20
GET /api/conteudos/pesquisa?q=Batman&tamanho=20&cursor=CURSOR
GET /api/conteudos/por-genero/ordenados?genero=drama&tamanho=20
```

200:

```json
{
  "resultados":[{"id":550,"title":"Clube da Luta","overview":"...","vote_average":8.4,"release_date":"1999-10-15","poster_path":"/exemplo.jpg","backdrop_path":null,"tipoConteudo":"MOVIE"}],
  "proximoCursor":"cursor-opaco","temMais":true,"podeContinuar":true,
  "totalResultados":null,"totalExato":false,"truncado":false
}
```

Última página: `proximoCursor:null, temMais:false, podeContinuar:false`. Orçamento insuficiente para confirmar próximo título: `temMais:null, podeContinuar:true` e cursor; página pode vir vazia/menor, mas é possível continuar. Não mostrar total exato nem calcular quantidade de páginas. `truncado:true` sinaliza corte em 500 páginas upstream por fluxo, não catálogo esgotado.

Cursores aleatórios de 256 bits referenciam estado no servidor; não são página/offset editáveis. Vinculam consulta normalizada, idioma, tamanho e versão; TTL padrão 15 minutos desde o início, capacidade 1.000 estados, dedup de até 2.000 identidades recentes. Reinício, expiração ou pressão de capacidade podem invalidar cursor: 400 com `mensagem`, reiniciar navegação. Não persistir cursor como marcador permanente. Em múltiplas instâncias, usar afinidade de sessão ou adotar armazenamento compartilhado antes de prometer navegação entre instâncias.

Pesquisa preserva relevância upstream, remove pessoas e atravessa páginas para preencher. Gêneros fazem merge incremental de `/discover/movie` e `/discover/tv`, cada um com `popularity.desc`, carregando a próxima página antes de decidir. Empates disponíveis: tipo (`MOVIE` antes de `TV`) e ID crescente; empates dentro de cada página são ordenados por ID. Popularidade ausente/não finita fica no fim. Gênero exclusivo consulta somente seu tipo. O upstream pode mudar popularidade/ordem entre páginas; não há snapshot global nem garantia de estabilidade de empates que cruzem páginas upstream.

Rotas antigas `/api/filmes/pesquisar` e `/api/conteudos/por-genero` permanecem: a primeira conserva totais do upstream que incluem pessoas; a segunda intercala filmes/séries. Migrar gradualmente o Flutter; onboarding legado não mudou de ordem automaticamente.

Exemplo Dart (utiliza seu cliente/autenticação existentes):

```dart
final uri = base.resolve('/api/conteudos/pesquisa').replace(queryParameters: {
  'q': consulta, 'tamanho': '20', if (cursor != null) 'cursor': cursor!,
});
final resposta = await http.get(uri);
final pagina = jsonDecode(resposta.body) as Map<String, dynamic>;
final itens = pagina['resultados'] as List;
// Deduplicar visualmente por '${item['tipoConteudo']}:${item['id']}'.
cursor = pagina['proximoCursor'] as String?;
final podeContinuar = pagina['podeContinuar'] == true;
// temMais pode ser null; não converter isso em fim de lista.
```

## Episódios e recomendações

Painel, grade e ranking mantêm rotas/contratos. Compartilham temporadas em cache de 15 minutos; grade não calcula ranking. 503 indica saturação/orçamento de temporadas; 504 prazo total. Cancelamento cooperativo não garante interrupção imediata do socket; timeouts HTTP continuam ativos.

`GET /api/recomendacoes/filmes` preservado; `GET /api/recomendacoes/series` adicionado. Ambos encaminham o Bearer ao Python e retornam o mesmo envelope (`preferencias`, `total`, `recomendacoes`), com `Cache-Control: no-store`. Não enviar JWT_SECRET ou credenciais do banco para Python. 401/403 permanecem erros de autorização; 429/503 upstream viram 503; timeout 504; JSON inválido 502. Inferência real não foi validada neste ambiente.
