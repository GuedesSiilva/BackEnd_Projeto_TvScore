package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.*;
import com.fasterxml.jackson.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.Duration;
import java.security.SecureRandom;

/** Cursores aleatórios de 256 bits referenciam estado limitado no servidor; não expõem parâmetros upstream. */
@Service
public class NavegacaoCatalogoService {
    public record Pagina(List<FilmeDTO> resultados, String proximoCursor, Boolean temMais,
            boolean podeContinuar, Long totalResultados, boolean totalExato, boolean truncado) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Item(Long id, String title, String name, String overview,
            @JsonProperty("vote_average") Double nota, @JsonProperty("release_date") String data,
            @JsonProperty("first_air_date") String estreia, @JsonProperty("poster_path") String poster,
            @JsonProperty("backdrop_path") String fundo, @JsonProperty("genre_ids") List<Integer> generos,
            @JsonProperty("media_type") String tipo, Double popularity) {
        FilmeDTO dto(TipoConteudo tipo) {
            return new FilmeDTO(id, tipo==TipoConteudo.MOVIE?title:name,overview,nota,
                    tipo==TipoConteudo.MOVIE?data:estreia,poster,fundo,generos,tipo,null,null);
        }
    }
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Fonte(List<Item> results, @JsonProperty("total_pages") Integer totalPages) {}
    private static final class Posicao {
        int pagina=1, offset=0; boolean fim=false;
        Posicao copiar() { var p=new Posicao(); p.pagina=pagina;p.offset=offset;p.fim=fim;return p; }
    }
    private static final class Estado {
        String vinculo; long expira; Posicao movie=new Posicao(),tv=new Posicao();
        LinkedHashSet<String> vistos=new LinkedHashSet<>(); boolean truncado;
        Estado copiar() { var e=new Estado();e.vinculo=vinculo;e.expira=expira;e.movie=movie.copiar();e.tv=tv.copiar();e.vistos=new LinkedHashSet<>(vistos);e.truncado=truncado;return e; }
    }
    private static final class Orcamento { int chamadas; long prazo; Orcamento(int n,long prazo){this.chamadas=n;this.prazo=prazo;} }
    private static final class Pausa extends RuntimeException {}
    private final TmdbClient tmdb; private final GeneroService generos; private final CatalogoExecutor executor;
    private final String idioma; private final int maxChamadas; private final long prazoMs;
    private final Map<String,Estado> cursores=new LinkedHashMap<>();
    private final SecureRandom random=new SecureRandom();
    @Value("${catalogo.cursor.ttl-ms:900000}") private long cursorTtlMs=900000;
    private final CacheTmdb<String,Fonte> paginas=new CacheTmdb<>(Duration.ofMinutes(2),512);
    public NavegacaoCatalogoService(TmdbClient tmdb, GeneroService generos, CatalogoExecutor executor,
            @Value("${tmdb.api.language}") String idioma,
            @Value("${catalogo.max-chamadas:6}") int maxChamadas,
            @Value("${catalogo.prazo-ms:10000}") long prazoMs) {
        this.tmdb=tmdb;this.generos=generos;this.executor=executor;this.idioma=idioma;
        if(maxChamadas<1 || prazoMs<1)throw new IllegalArgumentException("Orçamento inválido.");
        this.maxChamadas=maxChamadas;this.prazoMs=prazoMs;
    }
    private Estado estado(String cursor,String vinculo) {
        if(cursor==null) { var e=new Estado();e.vinculo=vinculo;e.expira=System.currentTimeMillis()+cursorTtlMs;return e; }
        if(!cursor.matches("[A-Za-z0-9_-]{43}"))throw invalido();
        synchronized(cursores) {
            var e=cursores.get(cursor);
            if(e==null || e.expira<System.currentTimeMillis() || !e.vinculo.equals(vinculo))throw invalido();
            return e.copiar();
        }
    }
    private String guardar(Estado e) {
        byte[] b=new byte[32];random.nextBytes(b);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        synchronized(cursores) {
            cursores.entrySet().removeIf(x->x.getValue().expira<System.currentTimeMillis());
            while(cursores.size()>=1000)cursores.remove(cursores.keySet().iterator().next());
            cursores.put(token,e.copiar());
        }
        return token;
    }
    private ResponseStatusException invalido(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cursor inválido ou expirado; reinicie a navegação.");}
    private String normalizar(String q,int tamanho) {
        if(q==null || q.isBlank() || q.length()>200 || tamanho<1 || tamanho>100)throw new IllegalArgumentException("Consulta ou tamanho inválido.");
        return q.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
    }
    public Pagina pesquisar(String q,int tamanho,String cursor) {
        String consulta=normalizar(q,tamanho); var e=estado(cursor,"v1|pesquisa|"+idioma+"|"+tamanho+"|"+consulta);
        return navegar(e,tamanho,new Orcamento(maxChamadas,System.nanoTime()+prazoMs*1000000),consulta,null);
    }
    public Pagina genero(String codigo,int tamanho,String cursor) {
        String consulta=normalizar(codigo,tamanho);var e=estado(cursor,"v1|genero|"+idioma+"|"+tamanho+"|"+consulta);
        long prazo=System.nanoTime()+prazoMs*1000000;
        var genero=executor.executar(prazo,()->generos.resolverGenero(consulta));
        if(genero.generoFilmeId()==null)e.movie.fim=true;
        if(genero.generoSerieId()==null)e.tv.fim=true;
        return navegar(e,tamanho,new Orcamento(maxChamadas,prazo),consulta,genero);
    }
    private Pagina navegar(Estado e,int tamanho,Orcamento o,String consulta,GeneroService.GeneroDisponivel genero) {
        List<FilmeDTO> saida=new ArrayList<>();Boolean mais=false;boolean continuar=false;
        Estado lookahead=null;
        try {
            while(saida.size()<tamanho) { FilmeDTO item=proximo(e,o,consulta,genero);if(item==null)break;saida.add(item); }
            lookahead=e.copiar();
            mais=proximo(lookahead,o,consulta,genero)!=null;
            continuar=mais;
        } catch(Pausa ex){mais=null;continuar=true;}
        finally { if(lookahead!=null)e.truncado |= lookahead.truncado; }
        return new Pagina(List.copyOf(saida),continuar?guardar(e):null,mais,continuar,null,false,e.truncado);
    }
    private FilmeDTO proximo(Estado e,Orcamento o,String consulta,GeneroService.GeneroDisponivel genero) {
        while(true) {
            Item escolhido;TipoConteudo tipo;Posicao pos;
            if(genero==null) {
                pos=e.movie;escolhido=cabeca(e,pos,o,"/search/multi",consulta,null);
                if(escolhido==null)return null;
                pos.offset++;
                if(!"movie".equals(escolhido.tipo()) && !"tv".equals(escolhido.tipo()))continue;
                tipo="movie".equals(escolhido.tipo())?TipoConteudo.MOVIE:TipoConteudo.TV;
            } else {
                Item m=cabeca(e,e.movie,o,"/discover/movie",null,genero.generoFilmeId());
                Item t=cabeca(e,e.tv,o,"/discover/tv",null,genero.generoSerieId());
                if(m==null && t==null)return null;
                boolean movie=t==null || (m!=null && comparar(m,t)<=0);
                escolhido=movie?m:t;tipo=movie?TipoConteudo.MOVIE:TipoConteudo.TV;pos=movie?e.movie:e.tv;pos.offset++;
            }
            if(escolhido.id()==null || escolhido.id()<=0)continue;
            String chave=tipo+":"+escolhido.id();
            if(!e.vistos.add(chave))continue;
            if(e.vistos.size()>2000)e.vistos.remove(e.vistos.iterator().next());
            return escolhido.dto(tipo);
        }
    }
    private int comparar(Item m,Item t) {
        double a=m.popularity()==null || !Double.isFinite(m.popularity())?Double.NEGATIVE_INFINITY:m.popularity();
        double b=t.popularity()==null || !Double.isFinite(t.popularity())?Double.NEGATIVE_INFINITY:t.popularity();
        int cmp=Double.compare(b,a);return cmp!=0?cmp:-1; // MOVIE precede TV em empate; ordem interna vem do upstream.
    }
    private Item cabeca(Estado e,Posicao p,Orcamento o,String caminho,String q,Long genero) {
        while(!p.fim) {
            if(p.pagina>500){e.truncado=true;p.fim=true;break;}
            if(System.nanoTime()>=o.prazo)throw new Pausa();
            String chave=idioma+"|"+caminho+"|"+q+"|"+genero+"|"+p.pagina;
            int pagina=p.pagina;
            Fonte fonte;
            try {
                fonte=executor.executar(o.prazo,()->paginas.buscar(chave,()->{
                    if(o.chamadas<=0)throw new Pausa();
                    o.chamadas--;
                    var uri=tmdb.consulta(caminho).queryParam("page",pagina);
                    if(q!=null)uri.queryParam("query",q);
                    else uri.queryParam("with_genres",genero).queryParam("sort_by","popularity.desc");
                    Fonte resposta=tmdb.buscar(uri,Fonte.class);
                    if(q!=null || resposta.results()==null)return resposta;
                    var ordenados=resposta.results().stream().filter(Objects::nonNull).sorted(
                        Comparator.comparing((Item i)->i.popularity()==null || !Double.isFinite(i.popularity())?Double.NEGATIVE_INFINITY:i.popularity(),Comparator.reverseOrder())
                        .thenComparing(Item::id,Comparator.nullsLast(Comparator.naturalOrder()))).toList();
                    return new Fonte(ordenados,resposta.totalPages());
                }));
            } catch(ResponseStatusException ex) { if(ex.getStatusCode().value()==504)throw new Pausa();throw ex; }
            List<Item> itens=fonte.results()==null?List.of():fonte.results();
            if(p.offset<itens.size()) { if(itens.get(p.offset)==null){p.offset++;continue;}return itens.get(p.offset); }
            if(fonte.totalPages()==null || p.pagina>=fonte.totalPages()){p.fim=true;break;}
            if(p.pagina==500){e.truncado=true;p.fim=true;break;}
            p.pagina++;p.offset=0;
        }
        return null;
    }
}
