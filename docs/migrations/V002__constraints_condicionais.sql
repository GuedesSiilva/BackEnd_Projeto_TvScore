-- Executar via cliente mysql (DELIMITER é comando do cliente).
-- Preferidos: primeiro revisar/aplicar preferidos-tipo-conteudo.sql somente nas etapas faltantes.
-- A aplicação deve estar sem escritas. Não apaga registros nem índices existentes.
DELIMITER $$
CREATE PROCEDURE tvscore_v002_constraints()
BEGIN
    IF EXISTS (SELECT 1 FROM avaliacao WHERE usuario_id IS NULL OR filme_serie_id IS NULL OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV'))
       OR EXISTS (SELECT 1 FROM favorito WHERE usuario_id IS NULL OR filme_serie_id IS NULL OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV'))
       OR EXISTS (SELECT 1 FROM usuario_filme_preferido WHERE usuario_id IS NULL OR filme_id IS NULL OR tipo_conteudo IS NULL OR tipo_conteudo NOT IN ('MOVIE','TV')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Nulos/tipos incompatíveis: interromper';
    END IF;
    IF EXISTS (SELECT 1 FROM avaliacao GROUP BY usuario_id, filme_serie_id, tipo_conteudo HAVING COUNT(*) > 1)
       OR EXISTS (SELECT 1 FROM favorito GROUP BY usuario_id, filme_serie_id, tipo_conteudo HAVING COUNT(*) > 1)
       OR EXISTS (SELECT 1 FROM usuario_filme_preferido GROUP BY usuario_id, filme_id, tipo_conteudo HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicatas: interromper e revisar manualmente';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM (SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) cols
        FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='avaliacao' AND non_unique=0
        GROUP BY index_name) i WHERE cols='usuario_id,filme_serie_id,tipo_conteudo') THEN
        ALTER TABLE avaliacao ADD CONSTRAINT uk_avaliacao_usuario_conteudo UNIQUE(usuario_id,filme_serie_id,tipo_conteudo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM (SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) cols
        FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='favorito' AND non_unique=0
        GROUP BY index_name) i WHERE cols='usuario_id,filme_serie_id,tipo_conteudo') THEN
        ALTER TABLE favorito ADD CONSTRAINT uk_favorito_usuario_conteudo UNIQUE(usuario_id,filme_serie_id,tipo_conteudo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM (SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) cols
        FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='usuario_filme_preferido' AND non_unique=0
        GROUP BY index_name) i WHERE cols='usuario_id,filme_id,tipo_conteudo') THEN
        ALTER TABLE usuario_filme_preferido ADD CONSTRAINT uk_usuario_conteudo_preferido UNIQUE(usuario_id,filme_id,tipo_conteudo);
    END IF;
END$$
CALL tvscore_v002_constraints()$$
DROP PROCEDURE tvscore_v002_constraints$$
DELIMITER ;
