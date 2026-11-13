# Preferidos de filmes e séries

Todas as chamadas usam `Authorization: Bearer <JWT>` do usuário. Não enviar ID de usuário.

## Flutter

- `GET /api/usuarios/eu/conteudos-preferidos/opcoes?pagina=1`: retorna o `TmdbResponseDTO` atual, com `id` e `tipoConteudo` (`MOVIE`/`TV`) por resultado, além dos metadados públicos e nomes de gêneros.
- `GET /api/usuarios/eu/conteudos-preferidos`: retorna `{"conteudos":[...]}` com `id`, `tipoConteudo`, `titulo`, `sinopse`, `generos`, `idiomaOriginal`, `dataLancamento`, `notaTmdb`, `popularidade`, `posterPath` e `selecionadoEm`. Consulta apenas os dados persistidos do autenticado.
- `PUT /api/usuarios/eu/conteudos-preferidos`: substitui a seleção completa e retorna o mesmo formato do GET.

```json
{"conteudos":[{"id":550,"tipoConteudo":"MOVIE"},{"id":1399,"tipoConteudo":"TV"}]}
```

Armazenar a identidade como par `(id, tipoConteudo)` em seleção, chaves de widgets e deduplicação. IDs iguais de tipos diferentes representam títulos diferentes. Enviar somente identidade; o backend obtém metadados do TMDB. Máximo de cinco entradas, contando ambos os tipos; pares repetidos são deduplicados após validar o limite de entradas. Lista nula, item nulo, ID não positivo ou tipo ausente/inválido retorna 400. `{"conteudos":[]}` limpa tudo.

Uma página mista intercala a mesma página das duas origens (até 40 itens); não é uma ordenação global por popularidade. `totalResults` soma as origens e `totalPages` usa o máximo. Páginas válidas: 1–500. Metadados da primeira página usam cache de cinco minutos com até 128 filtros, por tipo e IDs de gêneros ordenados; páginas esgotadas não são consultadas novamente. Sem gêneros, ambos os tipos são populares; gênero exclusivo não inclui títulos sem relação do outro tipo.

## Compatibilidade

`GET /api/usuarios/eu/filmes-preferidos/opcoes` usa a mesma lista mista. O GET antigo `/api/usuarios/eu/filmes-preferidos` continua com `filmes` e `filmeId`, apenas MOVIE. O PUT antigo aceita `{"filmes":[550]}` e substitui somente filmes, preservando séries e respeitando cinco no total; `{"filmes":[]}` remove somente filmes. Flutter novo deve editar a seleção completa pelas rotas genéricas.

## Banco: aplicação manual coordenada

O script [preferidos-tipo-conteudo.sql](migrations/preferidos-tipo-conteudo.sql) fica fora das pastas de migração automática. Não foi executado. Não foi habilitado Flyway nem alterado ddl-auto.

1. Fazer backup com restauração verificada e parar escritas concorrentes; coordenar a nova versão Java com a migração antes de iniciar a aplicação.
2. Executar o preflight (`SELECT DATABASE`, `SHOW CREATE TABLE`, `SHOW INDEX`, contagens das duas tabelas); guardar resultados. Confirmar nomes físicos e existência da coluna/índices, adaptando o script ao schema real.
3. Adicionar coluna nullable somente se ausente; preencher registros anteriores com MOVIE; verificar tipos/duplicatas; tornar NOT NULL.
4. Criar a chave única `(usuario_id, filme_id, tipo_conteudo)` antes de remover a antiga. Conferir o nome real e remover manualmente a restrição antiga e qualquer outra unicidade em apenas usuário/ID que impeça coexistência. Não confiar em `ddl-auto=update` para isso.
5. Verificar contagens, tipos, índices e ausência de gêneros órfãos. Não apagar/recriar tabelas nem alterar IDs ou FKs. Iniciar a versão nova somente com schema compatível.

DDL MySQL tem commits implícitos; não há promessa de rollback transacional estrutural. Em falha, manter escritas interrompidas e recuperar pelo backup ou correção revisada. Reexecutar apenas etapas comprovadamente pendentes, pois o script é deliberadamente manual.

Os testes de persistência usam H2 isolado: não comprovam execução da migração em MySQL. Validar o script em MySQL descartável antes da aplicação manual no banco de destino.

Salvar preferidos de séries **não faz o recomendador Python utilizá-los automaticamente**. Esse consumidor exige adaptação posterior, fora desta tarefa; o Python não foi alterado.
