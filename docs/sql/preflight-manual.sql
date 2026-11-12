-- Somente leitura; NAO executado nesta tarefa.
-- Aplicar manualmente apenas em copia isolada primeiro. Nao configura Flyway.
SHOW CREATE TABLE usuario;
SHOW CREATE TABLE usuario_genero;
SHOW CREATE TABLE avaliacao;
SHOW CREATE TABLE favorito;
SELECT LOWER(TRIM(email)) AS email_canonico, COUNT(*) AS quantidade
FROM usuario GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
-- Revisar nomes reais e constraints do schema antes de preparar ALTER/UPDATE.
