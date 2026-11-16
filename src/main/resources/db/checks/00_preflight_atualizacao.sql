-- SOMENTE CONFERÊNCIA. Não aplica DDL/DML. Executar em cópia MySQL descartável primeiro.
-- Selecionar explicitamente o schema no cliente. Não continuar com schema/base inesperados.
SELECT DATABASE() AS schema_alvo, VERSION() AS versao_mysql, @@session.time_zone AS timezone_sessao;

-- Estas seis tabelas já pertenciam à referência Java 169bf4a. Se faltar alguma,
-- parar: falta uma etapa anterior do schema, não criar uma base vazia sobre os dados atuais.
SELECT e.tabela, IF(t.table_name IS NULL, 'BASE AUSENTE: PARAR', 'PRESENTE') AS estado,
       t.engine, t.table_collation
FROM (
    SELECT 'usuario' tabela UNION ALL SELECT 'usuario_genero'
    UNION ALL SELECT 'avaliacao' UNION ALL SELECT 'favorito'
    UNION ALL SELECT 'usuario_filme_preferido' UNION ALL SELECT 'usuario_filme_preferido_genero'
) e LEFT JOIN information_schema.tables t ON t.table_schema=DATABASE() AND t.table_name=e.tabela;

-- Flyway: apenas detectar a existência; não criar/alterar histórico ou baseline.
SELECT table_name FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name='flyway_schema_history';
-- Se existir, executar separadamente e revisar versões/checksums/success:
-- SELECT installed_rank,version,description,type,script,checksum,success
-- FROM flyway_schema_history ORDER BY installed_rank;

-- Manifesto de TODAS as colunas novas da atualização. Resets é relação inversa, não coluna.
-- Em V001 novo: AUSENTE é esperado. Se parte já existir, NÃO executar V001 integralmente.
-- Conferir COLUMN_TYPE (inclusive signed/unsigned), precisão temporal e defaults.
SELECT e.tabela,e.coluna,e.tipo AS tipo_jpa,e.tamanho AS tamanho_jpa,e.nulo AS nullable_jpa,
       c.column_type,c.is_nullable,c.column_default,c.column_key,c.extra,c.datetime_precision,
       CASE WHEN c.column_name IS NULL THEN 'AUSENTE: MIGRAR'
            WHEN c.data_type<>e.tipo OR c.is_nullable<>e.nulo
              OR (e.tamanho IS NOT NULL AND c.character_maximum_length<>e.tamanho)
              OR (e.tipo='datetime' AND c.datetime_precision<>6)
              OR (e.tipo='bigint' AND c.column_type LIKE '%unsigned%')
            THEN 'DIVERGENTE: REVISAR ANTES DO DDL'
            ELSE 'COMPATIVEL: CONFERIR CHAVES/DEFAULTS' END AS estado
FROM (
    SELECT 'usuario' tabela,'versao_credenciais' coluna,'bigint' tipo,NULL tamanho,'NO' nulo
    UNION ALL SELECT 'usuario','foto_chave','varchar',200,'YES'
    UNION ALL SELECT 'reset_senha','id','bigint',NULL,'NO'
    UNION ALL SELECT 'reset_senha','usuario_id','bigint',NULL,'NO'
    UNION ALL SELECT 'reset_senha','hash','varchar',64,'NO'
    UNION ALL SELECT 'reset_senha','criado_em','datetime',NULL,'NO'
    UNION ALL SELECT 'reset_senha','expira_em','datetime',NULL,'NO'
    UNION ALL SELECT 'reset_senha','consumido_em','datetime',NULL,'YES'
    UNION ALL SELECT 'foto_limpeza','chave','varchar',200,'NO'
    UNION ALL SELECT 'foto_limpeza','depois_de','datetime',NULL,'NO'
) e LEFT JOIN information_schema.columns c
ON c.table_schema=DATABASE() AND c.table_name=e.tabela AND c.column_name=e.coluna
ORDER BY e.tabela,e.coluna;

-- Tipos e constraints da base (inclui todas as entidades e coleções JPA).
SELECT table_name,column_name,column_type,is_nullable,column_default,extra
FROM information_schema.columns WHERE table_schema=DATABASE()
AND table_name IN ('usuario','usuario_genero','avaliacao','favorito','usuario_filme_preferido',
                  'usuario_filme_preferido_genero','reset_senha','foto_limpeza')
ORDER BY table_name,ordinal_position;
SELECT table_name,index_name,non_unique,seq_in_index,column_name,sub_part
FROM information_schema.statistics WHERE table_schema=DATABASE()
AND table_name IN ('usuario','usuario_genero','avaliacao','favorito','usuario_filme_preferido',
                  'usuario_filme_preferido_genero','reset_senha','foto_limpeza')
ORDER BY table_name,index_name,seq_in_index;
SELECT k.table_name,k.constraint_name,k.column_name,k.referenced_table_name,k.referenced_column_name,
       r.delete_rule,r.update_rule
FROM information_schema.key_column_usage k
JOIN information_schema.referential_constraints r
ON r.constraint_schema=k.constraint_schema AND r.table_name=k.table_name AND r.constraint_name=k.constraint_name
WHERE k.table_schema=DATABASE() ORDER BY k.table_name,k.constraint_name,k.ordinal_position;

-- Continuar daqui apenas se as seis tabelas da base existirem. Guardar também o resultado fora da sessão.
SELECT COUNT(*) INTO @tvscore_usuarios_antes FROM usuario;
SELECT COUNT(*) INTO @tvscore_generos_antes FROM usuario_genero;
SELECT COUNT(*) INTO @tvscore_avaliacoes_antes FROM avaliacao;
SELECT COUNT(*) INTO @tvscore_favoritos_antes FROM favorito;
SELECT COUNT(*) INTO @tvscore_preferidos_antes FROM usuario_filme_preferido;
SELECT COUNT(*) INTO @tvscore_preferidos_generos_antes FROM usuario_filme_preferido_genero;
SELECT @tvscore_usuarios_antes usuarios,@tvscore_generos_antes generos,
       @tvscore_avaliacoes_antes avaliacoes,@tvscore_favoritos_antes favoritos,
       @tvscore_preferidos_antes preferidos,@tvscore_preferidos_generos_antes preferidos_generos;

SELECT COUNT(*) emails_canonicos_duplicados FROM (
    SELECT LOWER(TRIM(email)) FROM usuario GROUP BY LOWER(TRIM(email)) HAVING COUNT(*)>1
) d;
SELECT COUNT(*) invalidas FROM avaliacao WHERE usuario_id IS NULL OR filme_serie_id IS NULL
OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV');
SELECT COUNT(*) invalidos FROM favorito WHERE usuario_id IS NULL OR filme_serie_id IS NULL
OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV');
SELECT usuario_id,filme_serie_id,tipo_conteudo,COUNT(*) quantidade FROM avaliacao
GROUP BY usuario_id,filme_serie_id,tipo_conteudo HAVING COUNT(*)>1;
SELECT usuario_id,filme_serie_id,tipo_conteudo,COUNT(*) quantidade FROM favorito
GROUP BY usuario_id,filme_serie_id,tipo_conteudo HAVING COUNT(*)>1;
SELECT COUNT(*) orfaos FROM avaliacao a LEFT JOIN usuario u ON u.id=a.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) orfaos FROM favorito f LEFT JOIN usuario u ON u.id=f.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) orfaos FROM usuario_genero g LEFT JOIN usuario u ON u.id=g.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) orfaos FROM usuario_filme_preferido p LEFT JOIN usuario u ON u.id=p.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) orfaos FROM usuario_filme_preferido_genero g
LEFT JOIN usuario_filme_preferido p ON p.id=g.filme_preferido_id WHERE p.id IS NULL;

-- Preferidos legado pode ainda não possuir tipo_conteudo: não consultar coluna inexistente.
SET @tvscore_sql=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='usuario_filme_preferido' AND column_name='tipo_conteudo'),
    'SELECT usuario_id,filme_id,tipo_conteudo,COUNT(*) quantidade FROM usuario_filme_preferido GROUP BY usuario_id,filme_id,tipo_conteudo HAVING COUNT(*)>1',
    'SELECT ''PARAR: revisar migração legada preferidos-tipo-conteudo.sql'' AS pendencia');
PREPARE tvscore_conferencia FROM @tvscore_sql;
EXECUTE tvscore_conferencia;
DEALLOCATE PREPARE tvscore_conferencia;

-- Guardar contagens das tabelas novas se já existirem, sem assumir migração completa.
SET @tvscore_sql=IF(EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='reset_senha'),
    'SELECT COUNT(*) INTO @tvscore_resets_antes FROM reset_senha','SET @tvscore_resets_antes=0');
PREPARE tvscore_conferencia FROM @tvscore_sql;
EXECUTE tvscore_conferencia;
DEALLOCATE PREPARE tvscore_conferencia;
SET @tvscore_sql=IF(EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='foto_limpeza'),
    'SELECT COUNT(*) INTO @tvscore_fotos_limpeza_antes FROM foto_limpeza','SET @tvscore_fotos_limpeza_antes=0');
PREPARE tvscore_conferencia FROM @tvscore_sql;
EXECUTE tvscore_conferencia;
DEALLOCATE PREPARE tvscore_conferencia;
SELECT @tvscore_resets_antes resets,@tvscore_fotos_limpeza_antes fotos_limpeza;
-- Parar em qualquer duplicata, nulo de identidade, órfão ou tipo incompatível. Nunca deduplicar/apagar automaticamente.
