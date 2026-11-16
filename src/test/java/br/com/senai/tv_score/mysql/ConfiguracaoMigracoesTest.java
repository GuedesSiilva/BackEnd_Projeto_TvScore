package br.com.senai.tv_score.mysql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracaoMigracoesTest {
    @Test void propriedadesSemDuplicatasEValoresAlinhadosAoCodigo() throws Exception {
        for(String nome:List.of("application.properties","application-prod.properties")) {
            var linhas=Files.readAllLines(Path.of("src/main/resources/"+nome),StandardCharsets.UTF_8);
            Set<String> chaves=new HashSet<>();
            for(String linha:linhas) {
                String valor=linha.strip();
                if(valor.isEmpty() || valor.startsWith("#") || valor.startsWith("!"))continue;
                String chave=valor.split("[=:]",2)[0].strip();
                assertTrue(chaves.add(chave),"Propriedade duplicada em "+nome+": "+chave);
            }
        }
        var props=new Properties();
        try(var entrada=Files.newBufferedReader(Path.of("src/main/resources/application.properties"),StandardCharsets.UTF_8)){props.load(entrada);}
        assertEquals("validate",props.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false",props.getProperty("spring.flyway.enabled"));
        assertEquals("false",props.getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals("${FOTO_URL_BASE:http://localhost:8080/api/fotos}",props.getProperty("foto.url-base"));
        assertEquals("${FOTO_S3_REGIAO:${AWS_REGION:us-east-1}}",props.getProperty("foto.s3.regiao"));
    }
    @Test void sqlNovosAcompanhamArtefatoECobremEntidadesEColunas() throws Exception {
        String v1=ler("/db/migration/V001__conta_foto_reset.sql");
        for(String esperado:List.of("versao_credenciais BIGINT","foto_chave VARCHAR(200)",
                "CREATE TABLE reset_senha","hash VARCHAR(64)","criado_em DATETIME(6)",
                "expira_em DATETIME(6)","consumido_em DATETIME(6)","REFERENCES usuario(id)",
                "CREATE TABLE foto_limpeza","chave VARCHAR(200)","depois_de DATETIME(6)"))
            assertTrue(v1.contains(esperado),esperado);
        assertFalse(v1.toUpperCase(Locale.ROOT).contains("DROP TABLE"));
        assertTrue(ler("/db/checks/00_preflight_atualizacao.sql").contains("information_schema.columns"));
        assertTrue(ler("/db/checks/03_pos_verificacao_atualizacao.sql").contains("@tvscore_usuarios_antes"));
    }
    @Test void testeMysqlSoAceitaUrlDescartavelExplicitamenteIsolada() {
        String regex=MigracaoMySqlTest.class.getAnnotation(EnabledIfEnvironmentVariable.class).matches();
        assertTrue("jdbc:mysql://127.0.0.1:33079/tvscore_test_migracao".matches(regex));
        assertFalse("jdbc:mysql://exemplo.aivencloud.com:3306/tvscore_test_migracao".matches(regex));
        assertFalse("jdbc:mysql://127.0.0.1:3306/producao".matches(regex));
        assertFalse("jdbc:mysql://127.0.0.1:3306/tvscore_test_a?user=outro".matches(regex));
    }
    private String ler(String nome) throws Exception {
        try(var entrada=getClass().getResourceAsStream(nome)) {
            assertNotNull(entrada,nome);return new String(entrada.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
}
