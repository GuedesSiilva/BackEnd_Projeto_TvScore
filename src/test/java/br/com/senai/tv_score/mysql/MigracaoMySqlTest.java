package br.com.senai.tv_score.mysql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: banco MySQL vazio, descartável e exclusivamente loopback. Não lê DB_URL. */
@EnabledIfEnvironmentVariable(named="TVSCORE_MYSQL_TEST_URL", matches="jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/tvscore_test_[A-Za-z0-9_]+")
class MigracaoMySqlTest {
    Connection conectar() throws SQLException {
        String url=System.getenv("TVSCORE_MYSQL_TEST_URL");
        if(!url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/tvscore_test_[A-Za-z0-9_]+"))throw new IllegalStateException("Somente MySQL descartável em loopback.");
        return DriverManager.getConnection(url,"root",System.getenv().getOrDefault("TVSCORE_MYSQL_TEST_PASSWORD",""));
    }
    void executar(Connection c,String sql) throws SQLException {try(var s=c.createStatement()){s.execute(sql);}}
    long numero(Connection c,String sql) throws SQLException {try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    String script(String nome) throws Exception {
        try(var entrada=getClass().getResourceAsStream("/db/migration/"+nome)) {
            assertNotNull(entrada,"Migração precisa acompanhar o artefato Java");
            return new String(entrada.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    @Test void migraLegadoPreservaDadosConstraintsConsumoConcorrenteEFk() throws Exception {
        try(var c=conectar()) {
            assertFalse(c.getMetaData().getDatabaseProductVersion().contains("MariaDB"),"Exige MySQL real.");
            assertEquals(0,numero(c,"select count(*) from information_schema.tables where table_schema=database()"),"Banco deve estar vazio; não limpar automaticamente.");
            executar(c,"create table usuario(id bigint primary key auto_increment,nome varchar(255) not null,email varchar(255) not null unique,senha varchar(255) not null) engine=InnoDB");
            executar(c,"create table usuario_genero(usuario_id bigint not null,codigo_genero varchar(40) not null,primary key(usuario_id,codigo_genero),constraint fk_usuario_genero_usuario foreign key(usuario_id) references usuario(id)) engine=InnoDB");
            executar(c,"create table avaliacao(id bigint primary key auto_increment,usuario_id bigint not null,filme_serie_id bigint not null,tipo_conteudo enum('MOVIE','TV') not null,data_criacao datetime(6),nota double not null,comentario text,foreign key(usuario_id) references usuario(id)) engine=InnoDB");
            executar(c,"create table favorito(id bigint primary key auto_increment,usuario_id bigint not null,filme_serie_id bigint not null,tipo_conteudo enum('MOVIE','TV') not null,data_adicionado datetime(6),foreign key(usuario_id) references usuario(id)) engine=InnoDB");
            executar(c,"create table usuario_filme_preferido(id bigint primary key auto_increment,usuario_id bigint not null,filme_id bigint not null,tipo_conteudo enum('MOVIE','TV') not null,titulo varchar(1000) not null,sinopse text,idioma_original varchar(20),data_lancamento varchar(10),nota_tmdb double,popularidade double,poster_path varchar(1000),selecionado_em datetime(6) not null,foreign key(usuario_id) references usuario(id)) engine=InnoDB");
            executar(c,"create table usuario_filme_preferido_genero(filme_preferido_id bigint not null,genero_id bigint not null,nome varchar(255) not null,primary key(filme_preferido_id,genero_id),foreign key(filme_preferido_id) references usuario_filme_preferido(id)) engine=InnoDB");
            executar(c,"insert into usuario values(1,'Fixture','fixture@example.test','hash'),(2,'Outro','outro@example.test','hash')");
            executar(c,"insert into avaliacao(id,usuario_id,filme_serie_id,tipo_conteudo,data_criacao,nota) values(1,1,550,'MOVIE',now(6),8)");
            executar(c,"insert into favorito(id,usuario_id,filme_serie_id,tipo_conteudo) values(1,1,550,'MOVIE')");
            executar(c,"insert into usuario_filme_preferido(id,usuario_id,filme_id,tipo_conteudo,titulo,selecionado_em) values(1,1,550,'MOVIE','Fixture',now(6))");
            executar(c,"insert into usuario_genero values(1,'drama')");
            executar(c,"insert into usuario_filme_preferido_genero values(1,18,'Drama')");
            String v1=script("V001__conta_foto_reset.sql");
            for(String sql:v1.split(";"))if(!sql.isBlank())executar(c,sql);
            String v2=script("V002__constraints_condicionais.sql").replaceAll("(?m)^DELIMITER.*$","");
            for(String sql:v2.split("\\$\\$"))if(!sql.isBlank())executar(c,sql);
            assertEquals(2,numero(c,"select count(*) from usuario"));
            assertEquals(0,numero(c,"select sum(versao_credenciais) from usuario"));
            for(String tabela:new String[]{"avaliacao","favorito","usuario_filme_preferido"})assertEquals(1,numero(c,"select count(*) from "+tabela));
            assertEquals(1,numero(c,"select count(*) from usuario_genero"));
            assertEquals(1,numero(c,"select count(*) from usuario_filme_preferido_genero"));
            validarMapeamentosJpa();
            executar(c,"insert into favorito(usuario_id,filme_serie_id,tipo_conteudo) values(1,550,'TV')");
            assertThrows(SQLException.class,()->executar(c,"insert into favorito(usuario_id,filme_serie_id,tipo_conteudo) values(1,550,'TV')"));
            executar(c,"insert into reset_senha(usuario_id,hash,criado_em,expira_em) values(1,repeat('a',64),now(6),date_add(now(6),interval 20 minute))");
            try(var pool=Executors.newFixedThreadPool(2)) {
                var iniciar=new CountDownLatch(1);
                Callable<Integer> consumir=()->{iniciar.await();try(var x=conectar()){
                    x.setAutoCommit(false);
                    try(var s=x.createStatement()) {
                        try(var r=s.executeQuery("select id from usuario where id=1 for update")){r.next();}
                        int alterados=s.executeUpdate("update reset_senha set consumido_em=now(6) where hash=repeat('a',64) and consumido_em is null and expira_em>now(6)");
                        if(alterados==1)s.executeUpdate("update usuario set versao_credenciais=versao_credenciais+1 where id=1");
                        x.commit();return alterados;
                    }
                }};
                var a=pool.submit(consumir);var b=pool.submit(consumir);iniciar.countDown();
                assertEquals(1,a.get(10,TimeUnit.SECONDS)+b.get(10,TimeUnit.SECONDS));
            }
            assertEquals(1,numero(c,"select versao_credenciais from usuario where id=1"));
            executar(c,"insert into reset_senha(usuario_id,hash,criado_em,expira_em) values(2,repeat('b',64),now(6),date_add(now(6),interval 20 minute))");
            executar(c,"delete from usuario where id=2");assertEquals(0,numero(c,"select count(*) from reset_senha where usuario_id=2"));
        }
    }
    void validarMapeamentosJpa() {
        var registry=new org.hibernate.boot.registry.StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.url",System.getenv("TVSCORE_MYSQL_TEST_URL"))
            .applySetting("hibernate.connection.username","root")
            .applySetting("hibernate.connection.password",System.getenv().getOrDefault("TVSCORE_MYSQL_TEST_PASSWORD",""))
            .applySetting("hibernate.hbm2ddl.auto","validate").build();
        try {
            var fontes=new org.hibernate.boot.MetadataSources(registry);
            fontes.addAnnotatedClass(br.com.senai.tv_score.usuarios.Usuario.class);
            fontes.addAnnotatedClass(br.com.senai.tv_score.usuarios.FilmePreferido.class);
            fontes.addAnnotatedClass(br.com.senai.tv_score.avaliacao.Avaliacao.class);
            fontes.addAnnotatedClass(br.com.senai.tv_score.favorito.Favorito.class);
            fontes.addAnnotatedClass(br.com.senai.tv_score.auth.ResetSenha.class);
            fontes.addAnnotatedClass(br.com.senai.tv_score.usuarios.FotoLimpeza.class);
            // Mesmas estratégias físicas do Spring Boot; nenhum create/update/drop.
            var metadata=fontes.getMetadataBuilder()
                .applyPhysicalNamingStrategy(new org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy()).build();
            try(var factory=metadata.buildSessionFactory()) { assertTrue(factory.isOpen()); }
        } finally { org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registry); }
    }
}
