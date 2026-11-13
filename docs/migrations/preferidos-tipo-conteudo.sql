-- MANUAL, MySQL. Não executar em lote sem conferir cada etapa.
-- Fazer backup verificável e interromper escritas da aplicação antes de iniciar.
-- DDL MySQL faz commits implícitos: START TRANSACTION não garante rollback do schema.
-- Selecionar explicitamente o banco correto no cliente antes de executar.
SELECT DATABASE();
SHOW CREATE TABLE usuario_filme_preferido;
SHOW INDEX FROM usuario_filme_preferido;
SHOW CREATE TABLE usuario_filme_preferido_genero;
SELECT COUNT(*) AS preferidos_antes FROM usuario_filme_preferido;
SELECT COUNT(*) AS generos_antes FROM usuario_filme_preferido_genero;

-- Executar somente se tipo_conteudo ainda não existir; caso exista, inspecionar seus dados e definição.
ALTER TABLE usuario_filme_preferido ADD COLUMN tipo_conteudo VARCHAR(5) NULL;
-- O fluxo anterior somente salvava filmes. Não sobrescrever tipos já preenchidos.
UPDATE usuario_filme_preferido SET tipo_conteudo = 'MOVIE' WHERE tipo_conteudo IS NULL;
SELECT tipo_conteudo, COUNT(*) FROM usuario_filme_preferido GROUP BY tipo_conteudo;
-- Parar se houver valores diferentes de MOVIE/TV ou duplicatas.
SELECT usuario_id, filme_id, tipo_conteudo, COUNT(*) AS quantidade
FROM usuario_filme_preferido GROUP BY usuario_id, filme_id, tipo_conteudo HAVING COUNT(*) > 1;
ALTER TABLE usuario_filme_preferido MODIFY COLUMN tipo_conteudo VARCHAR(5) NOT NULL;
-- Executar somente se uma restrição equivalente ainda não existir.
ALTER TABLE usuario_filme_preferido ADD CONSTRAINT uk_usuario_conteudo_preferido
    UNIQUE (usuario_id, filme_id, tipo_conteudo);
SHOW INDEX FROM usuario_filme_preferido;

-- NÃO descomentar sem conferir o nome REAL do índice antigo no SHOW INDEX.
-- Remover todos os índices UNIQUE apenas de usuario_id/filme_id que impeçam os dois tipos.
-- A nova chave começa por usuario_id e mantém suporte ao índice da FK do usuário.
-- ALTER TABLE usuario_filme_preferido DROP INDEX `NOME_REAL_DO_INDICE_ANTIGO`;

-- Pós-verificação: contagens devem coincidir com as anteriores; IDs internos e FKs não mudam.
SHOW CREATE TABLE usuario_filme_preferido;
SHOW INDEX FROM usuario_filme_preferido;
SELECT COUNT(*) AS preferidos_depois FROM usuario_filme_preferido;
SELECT COUNT(*) AS generos_depois FROM usuario_filme_preferido_genero;
SELECT tipo_conteudo, COUNT(*) FROM usuario_filme_preferido GROUP BY tipo_conteudo;
SELECT COUNT(*) AS generos_orfaos FROM usuario_filme_preferido_genero g
LEFT JOIN usuario_filme_preferido p ON p.id = g.filme_preferido_id WHERE p.id IS NULL;
