-- PROPOSTA: não faz parte de V001/V002. Somente após comparar EXPLAIN e medir na cópia isolada.
-- Conferir índices equivalentes antes de adicionar. Não aplicado nesta tarefa.
CREATE INDEX ix_avaliacao_conteudo_recente ON avaliacao(filme_serie_id,tipo_conteudo,data_criacao DESC,id DESC);
CREATE INDEX ix_avaliacao_usuario_recente ON avaliacao(usuario_id,data_criacao DESC,id DESC);
