# Migrations MySQL manuais

> **Revisão atual:** veja [MIGRACAO_CONTA_FOTO_RESET.md](MIGRACAO_CONTA_FOTO_RESET.md).
> Os scripts canônicos agora acompanham o artefato em `src/main/resources/db/`.
> Esse guia cobre o erro `missing table [foto_limpeza]`, migração parcial, todas as colunas/entidades novas,
> ordem atual de aplicação e estratégia de baseline. A sequência abaixo registra a entrega anterior;
> não executar as cópias de docs e classpath duas vezes.

Não houve conexão ao Aiven, aplicação de SQL ou validação em MySQL real nesta tarefa. Docker não foi encontrado no PATH. H2 não certifica MySQL. Não foi ativado Flyway/baseline automático; o projeto possuía somente SQL manual. Scripts novos seguem a mesma estratégia e devem ser revisados em cópia descartável antes de qualquer implantação.

## Sequência

1. Executar `docs/sql/preflight-v001.sql` por etapas, guardando `SELECT DATABASE()`, versão, SHOW CREATE TABLE, SHOW INDEX, contagens, duplicatas/nulos/órfãos. Parar diante de incompatibilidades ou nomes/tipos diferentes dos esperados. O script não corrige dados. O ID do usuário deve ser BIGINT compatível com a FK nova (verificar signed/unsigned no schema real).
2. Revisar `docs/migrations/preferidos-tipo-conteudo.sql` já existente. Executar somente etapas faltantes: não duplicar coluna/constraint. Filme/série de mesmo ID devem coexistir. Um índice UNIQUE antigo de `(usuario_id,filme_id)` ou `(usuario_id,filme_serie_id)` impede isso; identificar pelo nome real e preparar remoção manual **somente após** criar a constraint tripla equivalente e revisar suporte de FKs. Nenhum índice antigo é removido automaticamente nesta entrega.
3. Aplicar `V001__conta_foto_reset.sql` **uma vez**, apenas se as colunas/tabelas ainda não existirem. Versão de credenciais recebe backfill zero e NOT NULL; foto é nullable; reset tem hash único, índice de expiração, FK ON DELETE CASCADE; limpeza de objetos não depende da conta para sobreviver à exclusão. DDL possui commits implícitos; interromper escritas e ter backup restaurável.
4. `V002__constraints_condicionais.sql` verifica dados incompatíveis/duplicatas e só cria índices únicos quando não encontra equivalente pela sequência de colunas. Requer cliente MySQL que interprete DELIMITER e permissão temporária CREATE ROUTINE. Não usar opção de continuar após erro. Em erro, parar, inspecionar o que já foi aplicado; não reaplicar V001 às cegas. A rotina temporária não é uma tabela nem remove dados.
5. Executar `docs/sql/pos-verificacao-v001.sql`, comparar contagens/IDs/FKs com o preflight e confirmar ausência de órfãos. Confirmar tipos NOT NULL nas colunas de identidade legadas; V002 não modifica tipos antigos automaticamente. Confirmar unicidade tripla e inexistência de UNIQUE duplo restritivo. As listas não devem perder dados.
6. `docs/sql/indices-avaliacao-candidatos.sql` é proposta separada, **não executada**: comparar os EXPLAIN para seleção, count e busca do usuário com dados representativos, índices existentes e latência. Só adicionar candidatos após essa revisão; não alegar ganho pelo simples fato de criar índice. Nenhum índice de leitura foi aplicado sem plano.
7. Subir aplicação com `ddl-auto=validate`; validar login com JWT por ID, backfill zero, troca/reset, exclusão de conta e fotos. Não usar `update` como migração de produção.

## Teste real descartável preparado

O teste `MigracaoMySqlTest` exige opt-in e aceita exclusivamente `jdbc:mysql://127.0.0.1:PORTA/tvscore_test_NOME`, sem query/userinfo. Não lê DB_URL; recusa banco não vazio e MariaDB. Use um banco novo por execução; não limpa/recria tabelas. O exemplo não foi executado:

```powershell
docker run --rm --name tvscore-mysql-test -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=tvscore_test_migracao -p 127.0.0.1:33079:3306 mysql:8.4
# Em outro terminal, após o MySQL ficar pronto:
$env:TVSCORE_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33079/tvscore_test_migracao'
$env:TVSCORE_MYSQL_TEST_PASSWORD=''
.\mvnw.cmd -Dtest=MigracaoMySqlTest test
```

O teste cria fixtures de schema legado com registros, aplica V001/V002, verifica preservação de contagens/backfill, MOVIE/TV com mesmo ID, rejeição de duplicata, consumo concorrente e FK de resets após exclusão. Não é cópia completa do schema real nem substitui revisão do SQL legado de preferidos. Após executar, encerrar o container descartável é responsabilidade do operador; nenhum comando de limpeza de arquivos/tabelas de usuário foi usado.

Pendente: executar em MySQL 8.x, testar schema legado real restaurado, validação JPA contra schema migrado, unicidade concorrente em todas as três listas, exclusão transacional completa com dependentes e planos EXPLAIN. Testes H2 cobrem parte dessas regras, mas não os certificam no dialeto MySQL. Acesso/revisão do schema publicado exige autorização própria fora desta implementação.
