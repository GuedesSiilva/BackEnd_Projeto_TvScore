-- MANUAL, MySQL 8.0+. Aplicar uma vez, após preflight e revisão do schema legado.
-- Não ativa Flyway. Não usar em produção antes da validação descartável.
ALTER TABLE usuario ADD COLUMN versao_credenciais BIGINT NULL DEFAULT 0;
UPDATE usuario SET versao_credenciais = 0 WHERE versao_credenciais IS NULL;
ALTER TABLE usuario MODIFY COLUMN versao_credenciais BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN foto_chave VARCHAR(200) NULL;

CREATE TABLE reset_senha (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    hash VARCHAR(64) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    consumido_em DATETIME(6) NULL,
    CONSTRAINT uk_reset_hash UNIQUE (hash),
    CONSTRAINT fk_reset_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    INDEX ix_reset_expira (expira_em),
    INDEX ix_reset_usuario (usuario_id)
) ENGINE=InnoDB;

-- Sem FK para usuario: a limpeza deve sobreviver à exclusão da conta.
CREATE TABLE foto_limpeza (
    chave VARCHAR(200) NOT NULL PRIMARY KEY,
    depois_de DATETIME(6) NOT NULL,
    INDEX ix_foto_limpeza_prazo (depois_de)
) ENGINE=InnoDB;
CREATE INDEX ix_usuario_foto ON usuario (foto_chave);
