package br.com.senai.tv_score.filme;
import br.com.senai.tv_score.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;
@Service
public class ResumoConteudoService {
    public record Chave(Long id,TipoConteudo tipo,String idioma) {}
    private final TmdbClient tmdb;
    private final String idioma;
    private final CacheTmdb<Chave,ConteudoResumoDTO> cache;
    private final ThreadPoolExecutor pool=new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),new ThreadPoolExecutor.AbortPolicy());
    public ResumoConteudoService(TmdbClient tmdb,@Value("${tmdb.api.language}") String idioma,
            @Value("${catalogo.resumo.ttl-segundos:900}") long ttl,@Value("${catalogo.resumo.capacidade:512}") int capacidade) {
        this.tmdb=tmdb;this.idioma=idioma;cache=new CacheTmdb<>(Duration.ofSeconds(ttl),capacidade);
    }
    private ConteudoResumoDTO carregar(Chave chave) {
        try {
            return cache.buscar(chave,()->{
                var d=tmdb.buscar(tmdb.consulta((chave.tipo()==TipoConteudo.MOVIE?"/movie/":"/tv/")+chave.id()),DetalhesTmdbDTO.class);
                boolean filme=chave.tipo()==TipoConteudo.MOVIE;
                return new ConteudoResumoDTO(chave.id(),chave.tipo(),d.title(),d.posterPath(),d.releaseDate(),d.voteAverage());
            });
        } catch(RuntimeException ex) { return cache.ultimoValor(chave); }
    }
    private Map<Chave,ConteudoResumoDTO> lote(List<Chave> chaves) {
        long prazo=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        Map<Chave,Future<ConteudoResumoDTO>> tarefas=new LinkedHashMap<>();
        for(var chave:new LinkedHashSet<>(chaves)) {
            if(tarefas.size()>=20)break;
            try { tarefas.put(chave,pool.submit(()->carregar(chave))); }
            catch(RejectedExecutionException ex) { break; }
        }
        Map<Chave,ConteudoResumoDTO> resultados=new HashMap<>();
        try {
            for(var e:tarefas.entrySet()) {
                try { resultados.put(e.getKey(),e.getValue().get(Math.max(1,prazo-System.nanoTime()),TimeUnit.NANOSECONDS)); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt();break; }
                catch(ExecutionException | TimeoutException ex) { resultados.put(e.getKey(),cache.ultimoValor(e.getKey())); }
            }
        } finally { tarefas.values().forEach(t->t.cancel(true)); }
        return resultados;
    }
    public org.springframework.data.domain.Page<AvaliacaoDTO> avaliacoes(org.springframework.data.domain.Page<AvaliacaoDTO> pagina) {
        var res=lote(pagina.stream().map(a->new Chave(a.filmeSerieId(),a.tipoConteudo(),idioma)).toList());
        return pagina.map(a->a.comConteudo(res.get(new Chave(a.filmeSerieId(),a.tipoConteudo(),idioma))));
    }
    public org.springframework.data.domain.Page<FavoritoDTO> favoritos(org.springframework.data.domain.Page<FavoritoDTO> pagina) {
        var res=lote(pagina.stream().map(a->new Chave(a.filmeSerieId(),a.tipoConteudo(),idioma)).toList());
        return pagina.map(a->a.comConteudo(res.get(new Chave(a.filmeSerieId(),a.tipoConteudo(),idioma))));
    }
    @jakarta.annotation.PreDestroy void fechar(){pool.shutdownNow();}
}
