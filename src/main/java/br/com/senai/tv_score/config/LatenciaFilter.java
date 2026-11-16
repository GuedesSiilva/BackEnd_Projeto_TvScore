package br.com.senai.tv_score.config;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
/** Somente padrão da rota; não registra parâmetros, IDs, e-mails ou autorização. */
@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 20)
public class LatenciaFilter extends OncePerRequestFilter {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(LatenciaFilter.class);
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        long inicio=System.nanoTime();
        if(LOG.isDebugEnabled())SqlDiagnostico.iniciar();
        try {chain.doFilter(request,response);}
        finally {
            Object rota=request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            var medicao=SqlDiagnostico.ATUAL.get();
            try {
                LOG.debug("http rota={} status={} duracaoMs={} sqlQuantidades={} sqlNanos={}",rota==null?"nao-mapeada":rota,response.getStatus(),(System.nanoTime()-inicio)/1000000d,
                    medicao==null?java.util.Map.of():medicao.quantidades,medicao==null?java.util.Map.of():medicao.nanos);
            }finally{SqlDiagnostico.encerrar();}
        }
    }
}
