-- SOMENTE em cópia descartável nesta implementação. Guardar resultados e contagens antes de migrar.
SELECT DATABASE(), VERSION();
SELECT LOWER(TRIM(email)) email_canonico,COUNT(*) quantidade FROM usuario GROUP BY LOWER(TRIM(email)) HAVING COUNT(*)>1;
SHOW CREATE TABLE usuario;
SHOW CREATE TABLE avaliacao;
SHOW CREATE TABLE favorito;
SHOW CREATE TABLE usuario_filme_preferido;
SHOW CREATE TABLE usuario_filme_preferido_genero;
SHOW INDEX FROM usuario;
SHOW INDEX FROM avaliacao;
SHOW INDEX FROM favorito;
SHOW INDEX FROM usuario_filme_preferido;
SELECT table_name, column_name, column_type, is_nullable FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name IN ('usuario','avaliacao','favorito','usuario_filme_preferido','reset_senha','foto_limpeza');
SELECT 'usuarios' entidade, COUNT(*) quantidade FROM usuario
UNION ALL SELECT 'avaliacoes',COUNT(*) FROM avaliacao
UNION ALL SELECT 'favoritos',COUNT(*) FROM favorito
UNION ALL SELECT 'preferidos',COUNT(*) FROM usuario_filme_preferido
UNION ALL SELECT 'generos_preferidos',COUNT(*) FROM usuario_filme_preferido_genero;
SELECT usuario_id,filme_serie_id,tipo_conteudo,COUNT(*) FROM avaliacao GROUP BY usuario_id,filme_serie_id,tipo_conteudo HAVING COUNT(*)>1;
SELECT usuario_id,filme_serie_id,tipo_conteudo,COUNT(*) FROM favorito GROUP BY usuario_id,filme_serie_id,tipo_conteudo HAVING COUNT(*)>1;
SELECT COUNT(*) invalidas FROM avaliacao WHERE usuario_id IS NULL OR filme_serie_id IS NULL OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV') OR data_criacao IS NULL;
SELECT COUNT(*) invalidos FROM favorito WHERE usuario_id IS NULL OR filme_serie_id IS NULL OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV');
SELECT COUNT(*) orfaos FROM avaliacao a LEFT JOIN usuario u ON u.id=a.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) orfaos FROM favorito f LEFT JOIN usuario u ON u.id=f.usuario_id WHERE u.id IS NULL;
SELECT COUNT(*) orfaos FROM usuario_filme_preferido p LEFT JOIN usuario u ON u.id=p.usuario_id WHERE u.id IS NULL;
-- Se tipo_conteudo ainda não existe em preferidos, seguir o preflight legado antes destas consultas:
SELECT usuario_id,filme_id,tipo_conteudo,COUNT(*) FROM usuario_filme_preferido GROUP BY usuario_id,filme_id,tipo_conteudo HAVING COUNT(*)>1;
SELECT COUNT(*) invalidos FROM usuario_filme_preferido WHERE usuario_id IS NULL OR filme_id IS NULL OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV');
-- Parar se houver duplicata/nulo/órfão/tipo inválido; não corrigir dados automaticamente.
