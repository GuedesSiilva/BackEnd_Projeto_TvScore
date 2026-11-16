-- Executar na mesma sessão do preflight e com aplicação/workers parados para comparar contagens.
-- Em outra sessão, usar as contagens guardadas no relatório; variáveis nulas NÃO comprovam preservação.
SELECT DATABASE() schema_alvo,VERSION() versao_mysql;
SHOW CREATE TABLE usuario;
SHOW CREATE TABLE reset_senha;
SHOW CREATE TABLE foto_limpeza;
SHOW INDEX FROM usuario;
SHOW INDEX FROM reset_senha;
SHOW INDEX FROM foto_limpeza;
SHOW INDEX FROM avaliacao;
SHOW INDEX FROM favorito;
SHOW INDEX FROM usuario_filme_preferido;

SELECT 'usuario' tabela,@tvscore_usuarios_antes antes,COUNT(*) depois FROM usuario
UNION ALL SELECT 'usuario_genero',@tvscore_generos_antes,COUNT(*) FROM usuario_genero
UNION ALL SELECT 'avaliacao',@tvscore_avaliacoes_antes,COUNT(*) FROM avaliacao
UNION ALL SELECT 'favorito',@tvscore_favoritos_antes,COUNT(*) FROM favorito
UNION ALL SELECT 'usuario_filme_preferido',@tvscore_preferidos_antes,COUNT(*) FROM usuario_filme_preferido
UNION ALL SELECT 'usuario_filme_preferido_genero',@tvscore_preferidos_generos_antes,COUNT(*) FROM usuario_filme_preferido_genero
UNION ALL SELECT 'reset_senha',@tvscore_resets_antes,COUNT(*) FROM reset_senha
UNION ALL SELECT 'foto_limpeza',@tvscore_fotos_limpeza_antes,COUNT(*) FROM foto_limpeza;

SELECT COUNT(*) versoes_invalidas FROM usuario WHERE versao_credenciais IS NULL OR versao_credenciais<0;
-- Somente uma coluna recém-adicionada deve ter todos os valores zero.
-- Versões positivas pré-existentes DEVEM permanecer iguais; nunca reiniciar versões/revogar a proteção.
SELECT versao_credenciais,COUNT(*) quantidade FROM usuario GROUP BY versao_credenciais;
SELECT COUNT(*) resets_orfaos FROM reset_senha r LEFT JOIN usuario u ON u.id=r.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) resets_invalidos FROM reset_senha
WHERE hash NOT REGEXP '^[0-9a-f]{64}$' OR criado_em IS NULL OR expira_em IS NULL OR expira_em<=criado_em;
SELECT COUNT(*) chaves_duplicadas FROM (SELECT hash FROM reset_senha GROUP BY hash HAVING COUNT(*)>1) d;
SELECT COUNT(*) fotos_limpeza_invalidas FROM foto_limpeza WHERE chave='' OR depois_de IS NULL;
SELECT COUNT(*) invalidos FROM usuario_filme_preferido WHERE tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV');

-- Conferir constraints por colunas, não apenas por nome. UNIQUE duplo antigo pode ainda bloquear MOVIE/TV.
SELECT table_name,index_name,non_unique,GROUP_CONCAT(column_name ORDER BY seq_in_index) colunas
FROM information_schema.statistics WHERE table_schema=DATABASE()
AND table_name IN ('avaliacao','favorito','usuario_filme_preferido','reset_senha','foto_limpeza')
GROUP BY table_name,index_name,non_unique ORDER BY table_name,index_name;
SELECT k.table_name,k.column_name,k.referenced_table_name,k.referenced_column_name,r.delete_rule
FROM information_schema.key_column_usage k JOIN information_schema.referential_constraints r
ON r.constraint_schema=k.constraint_schema AND r.table_name=k.table_name AND r.constraint_name=k.constraint_name
WHERE k.table_schema=DATABASE() AND k.table_name='reset_senha';
-- FK esperada reset_senha.usuario_id -> usuario.id, DELETE CASCADE; foto_limpeza NÃO tem FK de usuário.
-- Reexecutar o manifesto de 00_preflight_atualizacao.sql: nenhuma coluna nova deve estar AUSENTE/DIVERGENTE.
-- Conferir também órfãos/duplicatas da base pelo preflight. Depois iniciar com ddl-auto=validate.
