# Validação local — 16/11/2026

Implementação local concluída no escopo verificável deste ambiente; ativação externa ainda pendente. Não houve commit, push, merge, deploy, SMTP real, upload em bucket, conexão ao Aiven ou execução de migrations em produção. A especificação já staged foi preservada. Referência Java: `169bf4a4e74722bc5c3e495ab4beffd8c6c2c7c1`; versão do projeto 0.0.1-SNAPSHOT, Spring Boot 4.1.1, alvo Java 21. Nenhum AGENTS.md foi encontrado na árvore/ancestrais consultados. Não foram substituídos arquivos pela referência histórica.

## Matriz dos 13 itens

“Implementado local” não significa ativado em produção. Evidências abaixo são testes locais/H2/transporte falso, exceto quando explicitado.

| # | Item | Estado | Evidência executada | Dependência/pendência de ativação |
|---|---|---|---|---|
| 1 | Senha autenticada | Implementado local | `SegurancaSenhaTest`, `JwtServiceTest`: senha incorreta sem mutação, hash, login, filtro real recusa token anterior, claim ausente zero, trocas concorrentes | Migração da versão; Flutter descarta sessão; validar MySQL concorrente |
| 2 | Recuperação por e-mail | Implementado local | Testes de resposta pública idêntica, expiração, replay, dois consumidores, rate limit; `EmailRecuperacaoTest` usa provedor falso que falha | SMTP, URL/tela Flutter, teste de entrega em captura; fila volátil sem retry persistente |
| 3 | Foto de perfil | Implementado local | `FotoValidacaoTest`, `FotoServiceTest`: reencoda/limita dimensão, rejeita falso/MIME/tamanho, troca/remoção, concorrência, falha externa, limpeza protege foto ativa | Migração, S3/CDN público de homologação, testar falhas reais e política de objetos |
| 4 | Avatar em avaliações | Implementado local | `avatarDeAvaliacaoVemDoAutorPersistido`: nome/foto do cliente não prevalecem | Flutter usa avatar padrão quando null; ativar fotos |
| 5 | Pesquisa multiconteúdo | Implementado local | `NavegacaoCatalogoServiceTest`: pessoas/mistas/vazias, mesmo ID em tipos diferentes, cursor vinculado/adulterado/expirado, orçamento, continuidade e limite 500 | Migrar Flutter; smoke TMDB; afinidade/estado compartilhado em múltiplas instâncias |
| 6 | Avaliações recentes | Implementado local | `OrdemEDiagnosticoTest`: data DESC/id DESC, desempate, próxima/vazia, isolamento por tipo; contratos completos preservados | Validar plano/ordem no MySQL representativo |
| 7 | Gêneros ordenados conjuntamente | Implementado local com limite upstream documentado | Merge incremental em fronteiras, listas desbalanceadas, gênero exclusivo e empate por ID | Migração opcional Flutter; TMDB não garante snapshot nem desempates globais entre páginas |
| 8 | Metadados nas listas | Implementado local | `ResumoConteudoServiceTest`: 20 avaliações do mesmo título = 1 chamada fria, 0 novas quentes; TV do mesmo ID não usa cache MOVIE; falha mantém registro | Smoke TMDB; avaliar orçamento de 20 chaves/5 s e fallback no Flutter |
| 9 | Diagnóstico de latência | Instrumentado; diagnóstico representativo parcial | H2: duas consultas por Page com autores, baseline e p50/p95 locais abaixo; perfil `diagnostico` compila/carrega | MySQL/EXPLAIN, autenticação/rede/serialização isoladas e p50/p95 HTTP representativos pendentes |
| 10 | Painel de episódios | Implementado local; benchmark externo pendente | `EpisodioServiceTest`, `EpisodiosLimitesTest`: caches/filtros, prazo cancela tarefa, quatro operações concorrentes e quinta recebe 503 | Medir séries curtas/longas no TMDB frio/quente; socket pode terminar após cancelamento cooperativo |
| 11 | Integração Python | Código Java e Python local implementado; integração completa **não certificada** | Proxy de séries/filmes com Bearer; cinco testes Python com módulos/transporte falsos; AST válido dos seis arquivos | Instalar dependências no ambiente Python próprio, levar ao clone oficial, executar API/modelos, duas contas, inferência/RAM/sentimentos/similaridade reais |
| 12 | Lotes/cache | Implementado local com limites explícitos | `CacheTmdbTest`: hit/TTL/capacidade/concorrência/falha; resumo sem N+1 externo, caches separados por identidade e detalhes reutilizáveis | Medir carga real/colisões de locks; lote é agregação local, não endpoint nativo TMDB |
| 13 | Constraints e migrations MySQL | SQL e teste preparados; **execução real pendente** | `MigracaoMySqlTest` compilado e ignorado sem URL opt-in; testes H2 de unicidade/exclusão preservados | Docker/MySQL descartável, restauração de schema legado, EXPLAIN, validar migração/FKs/concorrência reais; nenhum SQL publicado foi executado |

## Comandos e resultados reais

Configuração de testes revisada: JDBC H2 em memória, `create-drop` só no teste, TMDB/Python loopback porta 1, e-mail desabilitado, SMTP loopback, storage desabilitado ou falso. Nenhum teste aponta a DB_URL de aplicação. A suíte MySQL exige variável separada de opt-in, banco vazio e loopback.

1. `mvnw.cmd clean test`: falhou no bootstrap PowerShell do Wrapper (`NullArray`, “Cannot start maven from wrapper”). A mesma falha já apareceu na tentativa inicial de compile. Não foi alterado o Wrapper para contornar com exemplos antigos.
2. Maven em cache inicialmente sem repositório explícito tentou `C:\.m2\repository` e falhou por acesso. Passou-se a `-Dmaven.repo.local=C:/Users/25163231/Documents/tv_score/.maven-test-cache`.
3. Compile offline inicialmente faltou starter-mail/S3 no cache do projeto; dependências locais existentes foram copiadas do cache do usuário. Uma tentativa restrita falhou ao ler JAR (`AccessDeniedException`); execução local com acesso autorizado e JDK disponível do IntelliJ permitiu compilar. Erros de integração encontrados nos testes foram corrigidos antes da rodada final (mocks da versão JWT, injeção de foto, headers Retry-After e expectativas do transporte falso).
4. **Rodada final**, 16/11/2026 10:10:15 BRT, JBR 25.0.2 compilando com `release 21`:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\jbr'
$tvMaven=Get-ChildItem $env:USERPROFILE/.m2/wrapper/dists -Recurse -Filter mvn.cmd |
    Select-Object -First 1 -ExpandProperty FullName
& $tvMaven -o '-Dmaven.repo.local=C:/Users/25163231/Documents/tv_score/.maven-test-cache' clean test
```

**BUILD SUCCESS** em 28,598 s. Surefire: 82 descobertos, **81 executados e aprovados**, 0 falhas, 0 erros, **1 ignorado** (`MigracaoMySqlTest`, sem MySQL descartável). Log local `implementacao-testes.log`; XMLs em `target/surefire-reports`. A execução não substitui validar sob JDK 21 de produção.

5. `package -DskipTests` offline falhou porque dependências do plugin JAR não estavam no cache (file-management/plexus/maven-archiver/commons e outras). A tentativa online seguinte buscou somente dependências Maven Central e **passou**:

```powershell
& $tvMaven '-Dmaven.repo.local=C:/Users/25163231/Documents/tv_score/.maven-test-cache' `
  '-Daether.connector.connectTimeout=5000' '-Daether.connector.requestTimeout=10000' -DskipTests package
```

**BUILD SUCCESS**, 10:11:23 BRT, 9,792 s. JAR local `target/tv_score-0.0.1-SNAPSHOT.jar`, repackaged Spring Boot. Não foi executado/deployado. Logs `implementacao-package.log` (offline falhou) e `implementacao-package-online.log` (passou).

6. `python --version` não estava disponível no PATH. Foi encontrado Python **3.12.0** embarcado em outra aplicação, sem `httpx`/`fastapi`; não foram instaladas dependências nele. Executado apenas AST e contrato com módulos falsos:

```powershell
& 'C:/Program Files/SOLIDWORKS Corp/SOLIDWORKS/Simulation/Topology/tools/smapy/python/python.exe' `
  -B docs/python/test_contrato.py
```

**5 testes aprovados**, 0,073 s; AST dos seis arquivos válido. `implementacao-python-testes.log`. O PowerShell classifica saída stderr de unittest como NativeCommandError no redirecionamento, mas o processo encerrou com código zero e `OK`. Não certifica instalação de dependências, FastAPI, modelo, inferência, disponibilidade ou consumo de RAM.

7. `git diff --check`: sem erros de whitespace; avisos de conversão LF→CRLF não são falhas. Nenhuma mudança foi staged por esta implementação; o único arquivo originalmente staged permanece a especificação. Inventário em [ARQUIVOS_ALTERADOS.md](ARQUIVOS_ALTERADOS.md).

## Medições disponíveis e seus limites

`OrdemEDiagnosticoTest`, H2 em memória, JVM 25.0.2, 25 avaliações/25 autores, página de 5, 30 amostras aquecidas. Comparação local de consulta do repositório com ID ASC versus serviço com criação DESC/ID DESC; não é replay do backend histórico inteiro.

| Consulta local | p50 | p95 |
|---|---:|---:|
| ID ASC, repositório | 2,1911 ms | 3,9176 ms |
| Criação DESC/ID DESC, serviço | 2,2046 ms | 4,0847 ms |

Uma página com total: **2 statements SQL**, seleção com autor e count, sem N+1. Os números não demonstram redução de latência. Arquivo gerado: `target/latencia-avaliacoes-h2.txt`. Enriquecimento com transporte falso: uma consulta para 20 avaliações do mesmo título em cache frio e zero adicionais na segunda leitura. Não mede latência externa.

O perfil `diagnostico` usa `LatenciaFilter`, `StatementInspector` sanitizado e eventos Hibernate: tempo HTTP agregado, quantidade/duração JDBC por categorias (usuário, avaliações, count, resumo, outros) e aquisição de conexão. Não registra SQL, bind values, IDs, token, e-mail nem URLs externas. Duração JDBC inclui driver/rede/banco; aquisição inclui pool/conexão. Tempo HTTP não é RTT do dispositivo. Mapeamento/serialização não estão separados em timers próprios: comparar HTTP total com fases JDBC e medir CPU/serialização com profiler em homologação; não atribuir todo residual a JSON.

Roteiro pendente: restaurar volume representativo no MySQL descartável, guardar EXPLAIN de seleção/count/agregação, aquecer JVM, coletar ao menos 100 requisições por cenário (cache frio/quente, página inicial/seguinte, série curta/longa), computar p50/p95, número de SQL/calls, concorrência, ambiente e tamanho de payload. Comparar com mesmo volume/configuração; não inventar baseline de produção. O fluxo leve legado de avaliações não chama TMDB; páginas enriquecidas novas podem aguardar até 5 s. Não atribuir latência antiga ao TMDB.

## Pendências para produção

Executar a suíte MySQL opt-in e preflight/pós-verificação; revisar constraints duplas antigas e índices candidatos; validar JPA no schema migrado; configurar SMTP/S3/CDN de homologação; entregar Flutter/reset/avatares/paginação; executar Python real e validar modelos/RAM; medir SQL/rede/episódios reais e limites em múltiplas instâncias; planejar implantação autorizada. A fila de e-mail é volátil e os contadores/cursores são locais, conforme [CONFIGURACAO_PRODUCAO.md](CONFIGURACAO_PRODUCAO.md). Nenhuma dessas ativações foi apresentada como concluída.
