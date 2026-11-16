package br.com.senai.tv_score.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/** Ativado somente pelo perfil diagnostico. Nunca registra texto SQL ou parâmetros. */
public class SqlDiagnostico implements StatementInspector {
    static final class Medicao {
        String categoria="outros";
        final Map<String,Long> quantidades=new LinkedHashMap<>(), nanos=new LinkedHashMap<>();
    }
    static final ThreadLocal<Medicao> ATUAL=new ThreadLocal<>();
    static void iniciar(){ATUAL.set(new Medicao());}
    static void encerrar(){ATUAL.remove();}
    static void registrar(String fase,long nanos){var m=ATUAL.get();if(m!=null){m.quantidades.merge(fase,1L,Long::sum);m.nanos.merge(fase,nanos,Long::sum);}}
    @Override public String inspect(String sql) {
        var m=ATUAL.get();
        if(m!=null) {
            String normal=sql.toLowerCase(Locale.ROOT);
            m.categoria=normal.contains("avg(")?"resumo":normal.contains("count(")?"count":
                    normal.contains(" from avaliacao ")?"avaliacoes":normal.contains(" from usuario ")?"usuario":"outros";
        }
        return sql;
    }
    public static final class Eventos implements org.hibernate.SessionEventListener {
        private long conexao,consulta;private String categoria;
        @Override public void jdbcConnectionAcquisitionStart(){conexao=System.nanoTime();}
        @Override public void jdbcConnectionAcquisitionEnd(){registrar("conexao",System.nanoTime()-conexao);}
        @Override public void jdbcExecuteStatementStart(){consulta=System.nanoTime();var m=ATUAL.get();categoria=m==null?"outros":m.categoria;}
        @Override public void jdbcExecuteStatementEnd(){registrar(categoria,System.nanoTime()-consulta);}
    }
}
