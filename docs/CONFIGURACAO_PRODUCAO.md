# Configuração e ativação

Nada foi implantado, enviado por SMTP real, carregado em bucket ou executado no Aiven. Configurar e validar em ambiente de homologação antes de ativar. Java 21 é a versão alvo; Spring Boot 4.1.1 foi preservado.

## Variáveis

| Variável | Padrão/função |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` na implantação; adicionar `diagnostico` somente durante medição controlada |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | conexão existente, fornecida externamente; nunca usar nos testes |
| `JWT_SECRET` | segredo existente, Base64 de ao menos 32 bytes aleatórios; não compartilhar com Python |
| `JWT_EXPIRATION_SECONDS` | 2592000 (30 dias); não foi reduzido |
| `TMDB_API_KEY` | catálogo Java/Python; fornecer externamente |
| `RECOMMENDATION_API_URL` | `http://localhost:8000`, Java → Python |
| `RECOMMENDATION_TIMEOUT_MS` | 60000; cliente qualificado separado do TMDB |
| `JAVA_API_URL` | `http://localhost:8080`, Python → Java |
| `RESET_EMAIL_HABILITADO` | false; só habilitar após configurar SMTP e tela Flutter |
| `RESET_URL` | `http://localhost:3000/redefinir-senha`; HTTPS obrigatório fora de localhost; sem query, fragmento ou userinfo |
| `RESET_REMETENTE` | `no-reply@localhost`; substituir por remetente autorizado |
| `RESET_VALIDADE_SEGUNDOS` | 1200 |
| `RESET_LIMITE`, `RESET_JANELA_SEGUNDOS` | 5 pedidos por chave em 900 s; chaves IP e e-mail, inclusive inexistente |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | localhost:1025 em desenvolvimento |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | vazios por padrão; segredo externo |
| `SMTP_AUTH`, `SMTP_TLS` | false / true; em provedor real usar autenticação e TLS exigido |
| `FOTO_STORAGE` | `desabilitado`, `local` (somente dev) ou `s3` |
| `FOTO_URL_BASE` | `http://localhost:8080/api/fotos` no dev; URL HTTPS pública estável de CDN/storage no S3 |
| `FOTO_LOCAL_DIRETORIO` | `./fotos-dev` ignorado pelo Git; não é solução de produção |
| `FOTO_S3_ENDPOINT` | vazio para AWS; endpoint HTTPS do serviço compatível com S3 para outros provedores |
| `FOTO_S3_REGIAO`, `FOTO_S3_BUCKET` | us-east-1 / vazio; ajustar ao provedor |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | cadeia padrão do SDK AWS, ou identidade IAM; não incluir em repositório/URL |
| `CATALOGO_MAX_CHAMADAS`, `CATALOGO_PRAZO_MS` | 6 páginas upstream por operação / 10000 ms; gênero frio pode exigir também até duas consultas de catálogo de gêneros, dentro do mesmo prazo |
| `CATALOGO_CURSOR_TTL_MS` | 900000; estado local, 1000 cursores, janela dedup de 2000 pares |
| `CATALOGO_RESUMO_TTL_SEGUNDOS`, `CATALOGO_RESUMO_CAPACIDADE` | 900 / 512; caches de resumo/detalhes incluem URI/idioma e tipo |
| `EPISODIOS_PRAZO_MS`, `EPISODIOS_MAX_TEMPORADAS` | 20000 / 100 por operação; série + até 100 temporadas |

Configurações Spring complementares: multipart 5 MiB/arquivo e 6 MiB/requisição; timeouts SMTP de conexão/leitura/escrita 5 s. TMDB mantém conexão 5 s/leitura 10 s. `foto.limpeza-intervalo-ms` padrão 60000 e `senha.reset.limpeza-intervalo-ms` padrão 3600000 podem ser configurados via propriedade Spring externa. Limites devem ser positivos; não aumentar pools arbitrariamente.

## SMTP e recuperação

Implementação local usa `JavaMailSender`, fila volátil de um worker e 100 mensagens, token bruto apenas em memória. Banco guarda SHA-256, criação, expiração e consumo; não há outbox com segredo em texto claro. Falha, saturação, envio desabilitado e expiração na fila geram aviso sanitizado. Não há retry persistente: reinício pode perder mensagens ainda na fila; usuário solicita novo link. 202 não comprova entrega. Cada novo pedido invalida tokens anteriores; reset/troca incrementam versão e invalidam resets pendentes.

Para teste manual local, usar SMTP de captura como Mailpit, `RESET_EMAIL_HABILITADO=true`, host localhost/porta de captura, `SMTP_AUTH=false`, `SMTP_TLS=false`. Não usar destinatários/provedores reais nos testes. A tela Flutter/deep link precisa ser entregue separadamente; o backend não inventa uma UI de recuperação.

Contadores são locais, TTL e 10.000 chaves; não confiam em `X-Forwarded-For`. Revisar a topologia do proxy: não configurar encaminhamento arbitrário de IP confiável. Várias instâncias multiplicam os limites; acrescentar limite distribuído no gateway ou implementar armazenamento compartilhado antes de prometer limite global. Respostas não revelam existência da conta, mas não se promete igualdade criptográfica de tempo entre consulta simples e gravações SQL.

## Fotos

S3 recebe somente PNG sanitizado, chave aleatória versionada, sem nome/e-mail do usuário. URL de leitura é pública estável (política explícita desta implementação), sem URL assinada persistida. Configurar CDN/política de leitura somente dos objetos de foto, sem listagem pública ou escrita anônima. Permissões do backend: put/delete restritos ao bucket/prefixo apropriado. Não fornecer credenciais ao Flutter.

Antes do upload, persiste-se tarefa de limpeza órfã. Upload falho/crash deixa a tarefa; transação confirmada remove a tarefa nova e agenda foto anterior. Usuário e tarefa são bloqueados para impedir que limpeza ative/apague foto de outra atualização. Limpeza começa após uma hora, processa até 50 tarefas/ciclo; falha mantém tarefa para nova tentativa. Excluir conta agenda o objeto na mesma transação. A chamada de remoção ao storage acontece sob lock da tarefa para impedir corrida com ativação tardia; seu timeout S3 de 20 s limita ocupação SQL no worker de limpeza. Isso não ocorre nas leituras/enriquecimento de páginas.

No modo local a API fornece GET público dos PNGs. O perfil `prod` rejeita `FOTO_STORAGE=local`. Para produção, S3 e URL pública precisam ser configurados e testados com credenciais de homologação. Storage desabilitado gera 503 no upload; login e perfis sem foto continuam funcionando.

## Python

Os seis arquivos de `docs/python/overlay` foram aplicados à cópia adjacente `../recomendador-TvScore-main/recomendador-TvScore-main`. Originais ficam em `docs/python/base`. Não é repositório Git verificado por commit; a referência usada é o conteúdo realmente lido. Consulte [python/README.md](python/README.md).

São necessárias as dependências do `requirements.txt` real: fastapi, uvicorn, httpx, python-dotenv, transformers, torch, sentence-transformers e sentencepiece. Usar ambiente virtual próprio e versões homologadas pelo projeto; não instalar no Python de outra aplicação. Não compartilhar JWT_SECRET ou MySQL com Python: o token do usuário é encaminhado em chamadas autenticadas ao Java.

## Ordem de ativação

1. Backup restaurável e preflight em cópia isolada; validar migrations e testes MySQL descartável.
2. Revisar constraints/índices e aplicar somente SQL faltante. DDL MySQL possui commit implícito; não há rollback automático do schema.
3. Configurar variáveis/SMTP de captura/storage de homologação e executar smoke tests. `ddl-auto=validate` em todos os perfis de aplicação; teste H2 usa `create-drop` exclusivamente em memória.
4. Atualizar Python e dependências; validar inferência de filmes/séries com duas contas, onboarding sem histórico e preservação de sentimentos/similaridade/RAM.
5. Entregar telas Flutter e migrar gradualmente pesquisa/gênero, mantendo rotas antigas durante transição.
6. Medir latência SQL/HTTP/cache frio/quente, revisar EXPLAIN e índices candidatos; só então planejar implantação autorizada. Esta tarefa não inclui deploy.
