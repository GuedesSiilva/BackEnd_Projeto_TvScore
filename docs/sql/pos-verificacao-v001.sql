SELECT DATABASE();
SHOW CREATE TABLE usuario;
SHOW CREATE TABLE reset_senha;
SHOW CREATE TABLE foto_limpeza;
SHOW INDEX FROM avaliacao;
SHOW INDEX FROM favorito;
SHOW INDEX FROM usuario_filme_preferido;
SELECT COUNT(*) usuarios, SUM(versao_credenciais IS NULL) versoes_nulas FROM usuario;
SELECT COUNT(*) FROM avaliacao;
SELECT COUNT(*) FROM favorito;
SELECT COUNT(*) FROM usuario_filme_preferido;
SELECT COUNT(*) FROM usuario_filme_preferido_genero;
SELECT COUNT(*) resets_orfaos FROM reset_senha r LEFT JOIN usuario u ON u.id=r.usuario_id WHERE u.id IS NULL;
-- Usar IDs de fixtures da cópia isolada, nunca IDs reais em logs de rotina.
EXPLAIN SELECT a.*,u.nome FROM avaliacao a JOIN usuario u ON u.id=a.usuario_id
WHERE a.filme_serie_id=550 AND a.tipo_conteudo='MOVIE' ORDER BY a.data_criacao DESC,a.id DESC LIMIT 20;
EXPLAIN SELECT COUNT(*) FROM avaliacao WHERE filme_serie_id=550 AND tipo_conteudo='MOVIE';
EXPLAIN SELECT * FROM avaliacao WHERE usuario_id=1 ORDER BY data_criacao DESC,id DESC LIMIT 20;
