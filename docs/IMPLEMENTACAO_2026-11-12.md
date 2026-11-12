# Implementacao local de 12/11/2026

O relatorio foi lido integralmente. As alteracoes locais de FilmeService e PreferenciasUsuarioService foram usadas como ponto de partida. Python, analise de comentarios e similaridade de sinopses foram preservados. Nenhum banco externo foi acessado.

## Autenticacao e usuarios

JWT agora identifica o ID interno com subject usuario:{id}; a leitura rejeita subjects antigos de email. Nao existe fallback por email. Tokens existentes exigem novo login. Validade configuravel, padrao 2592000 segundos (30 dias). Nao reutilizar IDs de contas excluidas em restauracoes/manutencoes.
Login invalido gera excecao 401 e o advice preserva mensagem. Cadastro e edicao usam normalizarNome: strip, obrigatorio e maximo 60; validacao HTTP tambem limita 60. Politica de email deliberadamente conservadora: preservar o endereco informado e a consulta existente, respeitando a collation do banco. Nao foi aplicado lowercase/strip unilateral, pois isso pode bloquear registros antigos ou colidir contas. Canonicalizacao depende de auditoria e migracao aprovada.

Favorito existente retorna 409, inclusive conflito de integridade concorrente pelo advice; inexistente retorna 404. A constraint por usuario + ID TMDB + tipo foi preservada. A exclusao de conta preserva senha, bloqueio, transacao e flushes: H2 testa dependentes e rollback, mas nao justifica assumir a ordem das FKs do MySQL publicado.

## Generos e catalogo

GeneroService concentra catalogos dinamicos MOVIE/TV, cache de seis horas, nomes e aliases antigos. O enum conserva somente codigos/IDs necessarios. Nao existe lista fixa substituindo o TMDB.
Se atualizar o catalogo falhar, usa o ultimo valor conhecido, quando disponivel. Em cache frio, preferencia falha explicitamente; nao retorna selecao vazia. Codigos persistidos ausentes no catalogo sao exibidos com o proprio codigo como nome. A validacao de gravacao usa a mesma resolucao dos filtros e acontece antes da transacao de escrita, aberta explicitamente com TransactionTemplate (sem depender de chamada interna de metodo anotado).
Listagens recebem genres com nomes, preservando genre_ids. Pesquisa e detalhes preservam formatos existentes. Enriquecimento nao consulta detalhes por item. Se nao houver catalogo disponivel para enriquecimento, a lista continua com os IDs; nomes podem ficar vazios. DTOs de pesquisa/detalhes do TMDB existentes foram mantidos.

## Episodios

Painel antigo preservado. Novas rotas GET /api/series/{serieId}/episodios/grade e /ranking compartilham os caches de serie e temporada. Grade: serieId e episodios com temporada, numero, nota, votos, data. Ranking retorna array de EpisodioDTO e aceita limite (1..50), minimoVotos (>0), incluirEspeciais; os padroes sao 10, 1, false. Grade aceita incluirEspeciais=false.
Carregamento frio usa lotes de ate quatro tarefas em pool fixo de quatro threads por instancia. Mantem ordem das temporadas, especiais, datas futuras, votos e desempates. Timeouts continuam por chamada: nao ha SLA de tempo total para series muito longas. Menos JSON na grade nao reduz o numero 1+N de chamadas; cache compartilhado reduz repeticoes entre rotas. TTL 15 minutos e capacidade 128 por cache permanecem.

## Paginacao e integracoes

Rotas completas antigas nao foram truncadas. Novas rotas GET /api/avaliacoes/minhas/paginadas, /api/avaliacoes/filme/{filmeSerieId}/paginadas?tipoConteudo=MOVIE e /api/favoritos/paginados aceitam pagina (base zero, padrao 0) e tamanho (1..100, padrao 20). Retornam Page com content, totalElements, totalPages e ordenacao crescente por ID. Totais referem-se a consulta do banco.
Pesquisa multi continua removendo pessoas da pagina, mas totalResults/totalPages sao os totais originais TMDB, incluindo pessoas. Busca por genero intercala filmes e series da mesma pagina: soma totais das fontes e usa maior totalPages; nao representa ordenacao global por popularidade. Nao inventamos total filtrado a partir de uma pagina.
Clientes HTTP distintos sao qualificados: restTemplate (TMDB, conexao 5s/leitura 10s) e recomendadorHttp (conexao 5s/leitura configuravel 60s). TMDB traduz erros na fronteira; recomendador preserva traducoes e encaminhamento Bearer. Nenhum cache de recomendacoes foi criado. O contrato Python titulo/sinopse/poster/similaridade_sinopse permanece. Erros continuam com mensagem. Diagnostico interno inclui referencia, classe e primeiro frame, sem mensagem da excecao, corpo, tokens ou URL com chave.

## Producao e migracao (sem execucao)

application-prod.properties desativa SQL e usa ddl-auto=none: ativar o perfil exige schema previamente provisionado. Nao habilita validate nem Flyway. Porta usa PORT com padrao 8080. CORS foi preservado; Flutter Web publicado exige origem conhecida.
Script docs/sql/preflight-manual.sql e somente leitura e NAO foi executado. Plano: obter DDL numa copia isolada; conferir FKs e constraints por tipo; identificar colisao de email; aprovar baseline e backfills; ensaiar backup/restauracao e migracao; so entao preparar DDL versionado e considerar validate. Nao e seguro fabricar ALTERs sem schema real. Aliases de genero permanecem ate migracao aprovada.

## Validacao e limites

Testes usam application.properties de teste, H2 em memoria e URLs loopback fechadas; integracoes testadas com mocks. H2 foi adicionado somente em escopo test. Nenhuma versao existente foi atualizada. Smoke test de contexto preservado. Maven unifica processamento Lombok para producao e testes e versao JJWT numa propriedade.
Docker nao esta disponivel no PATH; nao foi possivel comprovar constraints MySQL. A semantica de collation e concorrencia real MySQL continua pendente de teste descartavel. Nao foi auditado Flutter nem Python atualizados. A mudanca de status de favorito duplicado comum (400 para 409) e inexistente (400 para 404) e intencional; clientes devem ler mensagem e status.

Os flushes de exclusao e os DTOs compartilhados de criacao/resposta foram mantidos onde a alteracao nao demonstrou beneficio suficiente. Persistencia em lote, migracao de aliases e normalizacao de emails dependem de evidencia do schema, nao de remocao de dados. Nenhum arquivo de codigo foi removido.

## Resultado final e inventario

Com JDK 21 e cache Maven local explicitados, mvnw.cmd -B -ntp -Dmaven.repo.local=<cache-local> clean test executou: 15 testes, zero falhas, zero erros, zero ignorados (BUILD SUCCESS). As primeiras tentativas falharam por ambiente (JAVA_HOME, user.home, permissoes de JARs/target); a execucao final ocorreu com permissao ampliada. Logs locais: test-wrapper.log e test-execution.log. Nenhum teste usou banco publicado.

Arquivos alterados nesta implementacao (inclui arquivos com alteracoes preexistentes):

- .gitignore
- pom.xml
- src/main/java/br/com/senai/tv_score/auth/AuthController.java
- src/main/java/br/com/senai/tv_score/auth/AuthService.java
- src/main/java/br/com/senai/tv_score/auth/CadastroRequest.java
- src/main/java/br/com/senai/tv_score/avaliacao/AvaliacaoRepository.java
- src/main/java/br/com/senai/tv_score/avaliacao/AvaliacaoService.java
- src/main/java/br/com/senai/tv_score/config/RestTemplateConfig.java
- src/main/java/br/com/senai/tv_score/controller/AvaliacaoController.java
- src/main/java/br/com/senai/tv_score/controller/EpisodioController.java
- src/main/java/br/com/senai/tv_score/controller/FavoritoController.java
- src/main/java/br/com/senai/tv_score/dto/FilmeDTO.java
- src/main/java/br/com/senai/tv_score/exceptions/GlobalExceptionHandler.java
- src/main/java/br/com/senai/tv_score/favorito/FavoritoRepository.java
- src/main/java/br/com/senai/tv_score/favorito/FavoritoService.java
- src/main/java/br/com/senai/tv_score/filme/EpisodioService.java
- src/main/java/br/com/senai/tv_score/filme/FilmeService.java
- src/main/java/br/com/senai/tv_score/filme/GeneroConteudo.java
- src/main/java/br/com/senai/tv_score/filme/TmdbClient.java
- src/main/java/br/com/senai/tv_score/recomendacao/RecomendacaoService.java
- src/main/java/br/com/senai/tv_score/security/JwtAuthenticationFilter.java
- src/main/java/br/com/senai/tv_score/security/JwtService.java
- src/main/java/br/com/senai/tv_score/usuarios/PreferenciasUsuarioService.java
- src/main/java/br/com/senai/tv_score/usuarios/Usuario.java
- src/main/java/br/com/senai/tv_score/usuarios/UsuarioService.java
- src/main/resources/application.properties
- src/test/java/br/com/senai/tv_score/filme/CacheTmdbTest.java

Arquivos criados:

- docs/IMPLEMENTACAO_2026-11-12.md
- docs/sql/preflight-manual.sql
- src/main/java/br/com/senai/tv_score/dto/GradeEpisodiosDTO.java
- src/main/java/br/com/senai/tv_score/filme/GeneroService.java
- src/main/resources/application-prod.properties
- src/test/java/br/com/senai/tv_score/filme/EpisodioServiceTest.java
- src/test/java/br/com/senai/tv_score/filme/GeneroServiceTest.java
- src/test/java/br/com/senai/tv_score/recomendacao/RecomendacaoServiceTest.java
- src/test/java/br/com/senai/tv_score/security/JwtServiceTest.java
- src/test/java/br/com/senai/tv_score/usuarios/ExclusaoContaTest.java
- src/test/resources/application.properties

Arquivos removidos: nenhum. O relatorio original preexistente foi preservado.

README.md tambem foi atualizado com o link para este documento. Verificacao final pelo wrapper: 15 testes aprovados, BUILD SUCCESS; git diff --check sem erros de whitespace.
