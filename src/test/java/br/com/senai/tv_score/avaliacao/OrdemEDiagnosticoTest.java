package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.usuarios.*;
import br.com.senai.tv_score.filme.TipoConteudo;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")
@org.springframework.test.context.ActiveProfiles("diagnostico")
class OrdemEDiagnosticoTest {
    @Autowired UsuarioRepository usuarios; @Autowired AvaliacaoRepository avaliacoes;
    @Autowired AvaliacaoService service; @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManagerFactory emf;
    @Test void ordemCriacaoDesempateCountEEntityGraph() throws Exception {
        long conteudo=987654321L;List<Long> ids=new ArrayList<>();
        for(int i=0;i<25;i++) {
            var u=usuarios.saveAndFlush(new Usuario("Autor",UUID.randomUUID()+"@example.test","hash"));
            var a=avaliacoes.saveAndFlush(new Avaliacao(u,conteudo,8d,"Texto",TipoConteudo.MOVIE));ids.add(a.getId());
            jdbc.update("update avaliacao set data_criacao=? where id=?",java.sql.Timestamp.valueOf(i<10?"2026-01-01 10:00:00":"2026-02-01 10:00:00"),a.getId());
        }
        var pagina=service.listarPorFilme(conteudo,TipoConteudo.MOVIE,0,5);
        assertEquals(ids.reversed().subList(0,5),pagina.stream().map(a->a.id()).toList());
        assertEquals(25,pagina.getTotalElements());
        assertTrue(service.listarPorFilme(conteudo,TipoConteudo.TV,0,5).isEmpty());
        assertTrue(service.listarPorFilme(conteudo,TipoConteudo.MOVIE,5,5).isEmpty());
        assertEquals(ids.reversed().subList(5,10),service.listarPorFilme(conteudo,TipoConteudo.MOVIE,1,5).stream().map(a->a.id()).toList());
        var stats=emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        service.listarPorFilme(conteudo,TipoConteudo.MOVIE,0,5);
        assertEquals(2,stats.getPrepareStatementCount(),"Uma seleção com autor e um count, sem N+1.");
        List<Double> legado=new ArrayList<>(),recente=new ArrayList<>();
        for(int i=0;i<30;i++) {
            long t=System.nanoTime();avaliacoes.findByFilmeSerieIdAndTipoConteudo(conteudo,TipoConteudo.MOVIE,PageRequest.of(0,5,Sort.by("id").ascending()));legado.add((System.nanoTime()-t)/1000000d);
            t=System.nanoTime();service.listarPorFilme(conteudo,TipoConteudo.MOVIE,0,5);recente.add((System.nanoTime()-t)/1000000d);
        }
        Collections.sort(legado);Collections.sort(recente);
        String relatorio="Ambiente: H2 em memória; JVM "+System.getProperty("java.version")+"; 25 avaliações, 25 autores, página 5; 30 amostras aquecidas.\n"
            +"Comparação restrita à consulta local: id ASC (repositório) versus dataCriacao DESC/id DESC (serviço). Não mede autenticação, HTTP, MySQL, rede ou TMDB.\n"
            +"id ASC p50="+legado.get(14)+"ms p95="+legado.get(28)+"ms\n"
            +"criação DESC/id DESC p50="+recente.get(14)+"ms p95="+recente.get(28)+"ms\n"
            +"SQL para uma página com total: 2 statements. Não prova ganho em produção.\n";
        Files.writeString(Path.of("target/latencia-avaliacoes-h2.txt"),relatorio);
    }
}
