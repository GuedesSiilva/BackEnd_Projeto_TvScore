# Migração completa da atualização de conta, fotos e reset

Revisão de 16/11/2026. O erro `Schema validation: missing table [foto_limpeza]` indica que o schema selecionado não contém todos os objetos exigidos pelo JPA. `validate` não cria tabelas. A entrega anterior tinha DDL em `docs/migrations`, sem execução automática, e `src/main/resources/db/migration` estava vazio.

Não foi executado SQL no Aiven nem em outro MySQL nesta revisão. Mantido `ddl-auto=validate` nos perfis padrão e prod. Git inicial limpo, HEAD `c3fca12`. Não foi ativado Flyway.

## Cobertura conferida contra JPA e histórico Java

A comparação com `169bf4a` confirmou **duas entidades novas e duas colunas em usuario**. DTOs de avatar, resumos, cursores e caches não criam tabelas.

| Mapeamento | DDL em V001 | Observação |
|---|---|---|
| `Usuario.versaoCredenciais` | `usuario.versao_credenciais BIGINT NOT NULL DEFAULT 0` | Backfill zero dos nulos da coluna nova; nunca zerar versões positivas existentes |
| `Usuario.fotoChave` | `usuario.foto_chave VARCHAR(200) NULL` | Referência opcional, preserva dados anteriores |
| `ResetSenha.id` | `reset_senha.id BIGINT AUTO_INCREMENT PRIMARY KEY` | Nova tabela |
| `ResetSenha.usuario` | `usuario_id BIGINT NOT NULL`, FK para usuario.id, ON DELETE CASCADE | Confirmar signed/unsigned do ID real antes de executar |
| `ResetSenha.hash` | `hash VARCHAR(64) NOT NULL UNIQUE` | Só hash SHA-256, nunca token bruto |
| `ResetSenha.criadoEm`, `expiraEm` | `DATETIME(6) NOT NULL` | `Instant`; sem default que altere a expiração definida no código |
| `ResetSenha.consumidoEm` | `DATETIME(6) NULL` | Consumo opcional |
| `FotoLimpeza.chave` | `foto_limpeza.chave VARCHAR(200) PRIMARY KEY` | Sem auto_increment; sem FK de usuário |
| `FotoLimpeza.depoisDe` | `depois_de DATETIME(6) NOT NULL` | Prazo preenchido pelo código |
| `Usuario.resets` | Nenhuma coluna `resets` | Associação inversa `mappedBy`; FK está em reset_senha |

V001 inclui índices para expiração/usuário do reset, prazo da limpeza e chave da foto em usuario. V002 confere unicidade tripla por usuário+ID+MOVIE/TV nas três listas pessoais. O teste MySQL preparado valida as seis entidades e suas coleções com a estratégia física do Spring, mas essa execução ainda está pendente.

As seis tabelas legadas já existiam no mapeamento de `169bf4a`: usuario, usuario_genero, avaliacao, favorito, usuario_filme_preferido e usuario_filme_preferido_genero. Se alguma faltar ou estiver incompleta no schema real, parar e revisar a migração legada correspondente; não criar uma base vazia sobre os dados atuais.

## SQL encontrado no projeto

Busca recursiva encontrou os scripts abaixo, além de cópias de build em target (que não são fontes). Não havia schema.sql/data.sql nem outro migrador de produção configurado.

- `docs/migrations/V001__conta_foto_reset.sql` e `V002__constraints_condicionais.sql`: DDL da entrega anterior, agora copiado para a fonte canônica `src/main/resources/db/migration/`. **Não executar as duas cópias.** Os históricos foram preservados.
- `docs/migrations/preferidos-tipo-conteudo.sql`: migração legada, somente etapas faltantes após revisar o schema.
- `docs/sql/preflight-manual.sql`, `preflight-v001.sql`, `pos-verificacao-v001.sql`: conferências anteriores; para esta revisão, usar os novos checks completos abaixo.
- `docs/sql/indices-avaliacao-candidatos.sql`: proposta opcional, depende de EXPLAIN/medição; fora da sequência obrigatória.

## Ordem manual de aplicação

Primeiro testar em backup restaurado em **MySQL 8.x descartável**. Antes da aplicação real autorizada: backup restaurável, janela sem escritas e workers parados. DDL MySQL faz commits implícitos; START TRANSACTION não assegura rollback de schema. O cliente deve parar no primeiro erro, sem `--force`.

1. **`src/main/resources/db/checks/00_preflight_atualizacao.sql`**: confirmar DATABASE/VERSION, existência da base legada, manifesto de todas as colunas novas, tipos/precisão/nullability, índices/FKs e eventual histórico Flyway. Guardar contagens fora da sessão. Interromper se houver base ausente, duplicatas, tipos desconhecidos, nulos de identidade ou órfãos. As consultas da base pressupõem as seis tabelas existentes; não avançar se o inventário apontar ausência.
2. **Somente se necessário**, revisar `docs/migrations/preferidos-tipo-conteudo.sql`. Backfill MOVIE só vale para onboarding antigo comprovadamente apenas de filmes; nunca inferir o tipo de avaliações/favoritos ou de dados mistos desconhecidos.
3. **`src/main/resources/db/migration/V001__conta_foto_reset.sql`**: acrescenta as duas colunas e cria **reset_senha e foto_limpeza**, FKs e índices. Executar integralmente apenas quando todos os objetos novos ainda estiverem ausentes. O primeiro erro citado pelo Hibernate não comprova que os demais objetos estejam corretos.
4. **`src/main/resources/db/migration/V002__constraints_condicionais.sql`**: após a base e tipo de preferidos estarem prontos. Requer cliente que interprete DELIMITER e permissão CREATE ROUTINE. Rejeita nulos/tipos/duplicatas e só cria UNIQUE quando não detecta equivalente pelas colunas. Um UNIQUE antigo duplo pode ainda bloquear MOVIE/TV de mesmo ID: identificar o nome real e planejar remoção manual apenas após confirmar a constraint tripla e suporte das FKs. Nenhum índice antigo é removido automaticamente.
5. **`src/main/resources/db/checks/03_pos_verificacao_atualizacao.sql`**: comparar contagens das oito tabelas, versões de credenciais, órfãos, hashes únicos e estruturas reais. Usar a mesma sessão do preflight ou o relatório guardado; variáveis nulas não provam preservação. Reexecutar o manifesto de colunas do preflight: nenhuma coluna nova deve permanecer AUSENTE/DIVERGENTE.
6. Só depois iniciar a aplicação com `validate`, conferindo o schema efetivamente selecionado pela conexão. Validar login, troca/reset, foto e exclusão. Não foi feito contra produção nesta revisão.

### Se a atualização já foi parcialmente aplicada

V001 não usa IF NOT EXISTS para esconder estrutura incompatível e **não é um reconciliador idempotente**. ADD COLUMN/CREATE TABLE repetidos falham. Não reaplicar tudo, não apagar tabelas e não reiniciar versões para fazê-lo passar.

Comparar SHOW CREATE TABLE e o manifesto; montar na cópia isolada a sequência de **somente os statements ausentes**, na ordem de V001. Se apenas foto_limpeza faltar e os demais objetos forem compatíveis, aplicar somente seu bloco CREATE TABLE (inclui índice do prazo), não recriar reset_senha nem repetir as colunas. Se um objeto existir com estrutura diferente, preparar ALTER específico após inspecionar tipos/dados reais. Registrar exatamente os trechos aplicados e repetir as conferências. O backfill nunca pode substituir versões positivas de credenciais por zero.

## Flyway e baseline

O POM contém `spring-boot-starter-flyway-test` com escopo **test**. Isso não instala migrations de produção. Não foi adicionado runtime/módulo MySQL, bean migrador ou baseline. A configuração agora explicita `spring.flyway.enabled=false` e `spring.flyway.baseline-on-migrate=false`; prod herda. Os arquivos no classpath, por si só, não executam SQL com essa configuração.

Estratégia proposta caso se decida adotar Flyway posteriormente, fora da aplicação manual atual:

- Se flyway_schema_history existir: revisar versões, checksums e success. Respeitar o histórico; não criar outro baseline nem editar migration aplicada.
- Legado sem histórico, auditado como equivalente à referência anterior, com todos os objetos novos ausentes: após homologar em cópia isolada, baseline **explícito 0** representa somente esse legado e deixa V001/V002 pendentes. Só então instalar runtime/módulo MySQL e executar migração controlada. Não usar baseline-on-migrate=true como atalho.
- Schema parcial/atualizado sem histórico: concluir manualmente os trechos faltantes e todas as verificações. Apenas quando o estado completo de **V001 e V002** estiver comprovado, baseline **explícito 2** pode representá-lo; versões futuras devem ser maiores. Baseline 2 antes de criar foto_limpeza apenas esconderia o problema.

Esses números pertencem a este conjunto novo sem histórico; conflito com um histórico real exige ajustar o plano antes de habilitar. Não foi configurado baseline-version nem executado baseline. Homologar também o parsing de DELIMITER/rotinas pelo módulo MySQL e permissões do provedor antes de automatizar.

## Propriedades corrigidas

Removidas oito repetições: SMTP timeout/writetimeout e seis propriedades de foto/storage. URL padrão única: `http://localhost:8080/api/fotos`, conforme FotoService/FotoLocalController; FOTO_URL_BASE permanece configurável. Região única: FOTO_S3_REGIAO, com fallback AWS_REGION (compatibilidade com a configuração efetiva anterior) e depois us-east-1. Storage segue desabilitado por padrão. Nenhum segredo foi incluído; validate permanece padrão/prod.

## Validação executada e pendências

Executados compilação e `ConfiguracaoMigracoesTest`: **3 testes aprovados**, verificando duplicatas/defaults/proteções, SQL no classpath e isolamento da URL MySQL. `MigracaoMySqlTest` compilou, mas foi **ignorado** sem TVSCORE_MYSQL_TEST_URL. Docker não está no PATH. Não é prova de SQL MySQL ou do schema real. Log: `migracoes-conferencia-testes.log`.

O teste MySQL agora usa a fonte SQL do classpath, fixture completa das seis tabelas legadas com dados e Hibernate **validate** nas seis entidades após V001/V002. Mantém verificações de contagens, unicidade/MOVIE+TV, consumo concorrente e FK de reset. Foram corrigidos os escapes do filtro de URL e do separador da rotina que impediam o opt-in/parser corretos. Falta executar:

```powershell
# Somente instância descartável nova, nunca DB_URL/Aiven:
docker run --rm --name tvscore-mysql-test -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=tvscore_test_migracao -p 127.0.0.1:33079:3306 mysql:8.4
# Outro terminal, após o servidor ficar pronto:
$env:TVSCORE_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33079/tvscore_test_migracao'
$env:TVSCORE_MYSQL_TEST_PASSWORD=''
.\mvnw.cmd -Dtest=MigracaoMySqlTest test
```

O teste recusa banco não vazio, MariaDB e URL fora de loopback/tvscore_test_NOME, sem query; não lê DB_URL e não limpa tabelas. A fixture não substitui restaurar o schema real.

Ainda depende de conferência real: DATABASE/versão/engine/collation; BIGINT signed de usuario.id e compatibilidade da FK; quais objetos já existem; base legada/colunas de preferidos; FKs/índices equivalentes ou duplos; nulos/duplicatas/órfãos; histórico Flyway; permissões ALTER/CREATE ROUTINE; preservação de contagens/versões; validação Hibernate/MySQL/timezone. Não se presume nenhum desses resultados sem acesso autorizado ao schema.
