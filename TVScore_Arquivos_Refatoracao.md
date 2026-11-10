# Ajustes do backend TV Score

Arquivos completos preparados a partir da branch `main` no commit [`0c31ac1`](https://github.com/GuedesSiilva/BackEnd_Projeto_TvScore/commit/0c31ac15c5baa89e2b14060732dc84a321fa805e). Cada bloco corresponde ao conteúdo inteiro do arquivo indicado. Aplique os arquivos de cada grupo juntos, de preferência em uma branch separada.

- **Avaliações:** POST e PUT passam a aplicar a mesma regra de nota e comentário. O POST mantém o comportamento de criar ou atualizar e o status 201. O DELETE passa a responder 403 quando a avaliação pertence a outro usuário e 404 quando não existe. A listagem carrega o autor junto com as avaliações para evitar consultas adicionais por autor.
- **JWT:** o filtro passa a verificar a assinatura e ler a identidade uma única vez. Os tokens já emitidos mantêm o mesmo formato.
- **TMDB:** filmes e episódios passam a usar o mesmo cliente HTTP. A pesquisa reaproveita os nomes de gênero por tipo e mantém os títulos se apenas o enriquecimento falhar. Nesse caso usa os últimos nomes disponíveis em memória ou `genres: []` quando ainda não há cache.
- **Cache:** a primeira página por gênero fica em cache por cinco minutos; temporadas e o índice de temporadas da série, por quinze. Em cache frio, o painel continua consultando as temporadas em sequência. A lista de gêneros fica em cache por seis horas. Cada cache tem limite de entradas.
- **Flutter:** as URLs e os campos existentes foram preservados. Pesquisa e detalhes continuam com `genres`; as demais listagens continuam com `genre_ids`. Não há alteração de estrutura no banco.

## Ordem de aplicação

### Avaliações e validação de IDs

- Substituir: `src/main/java/br/com/senai/tv_score/dto/AvaliacaoDTO.java`
- Substituir: `src/main/java/br/com/senai/tv_score/dto/FavoritoDTO.java`
- Substituir: `src/main/java/br/com/senai/tv_score/avaliacao/AvaliacaoRepository.java`
- Substituir: `src/main/java/br/com/senai/tv_score/avaliacao/AvaliacaoService.java`

### Autenticação

- Substituir: `src/main/java/br/com/senai/tv_score/security/JwtService.java`
- Substituir: `src/main/java/br/com/senai/tv_score/security/JwtAuthenticationFilter.java`

### Catálogo e episódios

- Criar: `src/main/java/br/com/senai/tv_score/filme/CacheTmdb.java`
- Criar: `src/main/java/br/com/senai/tv_score/filme/TmdbClient.java`
- Substituir: `src/main/java/br/com/senai/tv_score/filme/FilmeService.java`
- Substituir: `src/main/java/br/com/senai/tv_score/filme/EpisodioService.java`

### Testes de regressão

- Criar: `src/test/java/br/com/senai/tv_score/avaliacao/AvaliacaoServiceTest.java`
- Criar: `src/test/java/br/com/senai/tv_score/filme/CacheTmdbTest.java`

## Avaliações e validação de IDs — código completo

### Substituir `src/main/java/br/com/senai/tv_score/dto/AvaliacaoDTO.java`

```java
package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AvaliacaoDTO(
        Long id,

        @NotNull(message = "O ID do filme/série é obrigatório")
        @Positive(message = "O ID do conteúdo deve ser positivo.")
        Long filmeSerieId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 0, message = "A nota mínima é 0")
        @Max(value = 10, message = "A nota máxima é 10")
        Double nota,

        @Size(max = 1000, message = "O comentário deve ter até 1000 caracteres.")
        String comentario,

        String nomeUsuario,

        LocalDateTime dataCriacao,

        @NotNull(message = "O tipo do conteúdo é obrigatório")
        TipoConteudo tipoConteudo
) {
}
```

### Substituir `src/main/java/br/com/senai/tv_score/dto/FavoritoDTO.java`

```java
package br.com.senai.tv_score.dto;

import br.com.senai.tv_score.filme.TipoConteudo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record FavoritoDTO(
        Long id,

        @NotNull(message = "O ID do filme/série é obrigatório")
        @Positive(message = "O ID do conteúdo deve ser positivo.")
        Long filmeSerieId,

        LocalDateTime dataAdicionado,

        @NotNull(message = "O tipo do conteúdo é obrigatório")
        TipoConteudo tipoConteudo
) {}
```

### Substituir `src/main/java/br/com/senai/tv_score/avaliacao/AvaliacaoRepository.java`

```java
package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    @EntityGraph(attributePaths = "usuario")
    List<Avaliacao> findByFilmeSerieIdAndTipoConteudo(
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    );

    @EntityGraph(attributePaths = "usuario")
    List<Avaliacao> findByUsuario(Usuario usuario);

    Optional<Avaliacao> findByUsuarioAndFilmeSerieIdAndTipoConteudo(
            Usuario usuario,
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    );

    interface ResumoNotas {
        Double getMedia();
        Long getTotalAvaliacoes();
    }

    @Query("""
        SELECT AVG(a.nota) AS media,
               COUNT(a) AS totalAvaliacoes
        FROM Avaliacao a
        WHERE a.filmeSerieId = :filmeSerieId
          AND a.tipoConteudo = :tipoConteudo
        """)
    ResumoNotas buscarResumo(
            @Param("filmeSerieId") Long filmeSerieId,
            @Param("tipoConteudo") TipoConteudo tipoConteudo
    );
}
```

### Substituir `src/main/java/br/com/senai/tv_score/avaliacao/AvaliacaoService.java`

```java
package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.dto.AtualizarAvaliacaoDTO;
import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.dto.ResumoAvaliacaoDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarPorFilme(
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    ) {
        return avaliacaoRepository
                .findByFilmeSerieIdAndTipoConteudo(
                        filmeSerieId,
                        tipoConteudo
                )
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoDTO> listarPorUsuario(Usuario usuario) {
        return avaliacaoRepository.findByUsuario(usuario)
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional
    public AvaliacaoDTO avaliar(Usuario usuario, AvaliacaoDTO dto) {
        validarConteudo(dto.filmeSerieId(), dto.tipoConteudo());
        String comentario = validarEdicao(dto.nota(), dto.comentario());
        Avaliacao avaliacao = avaliacaoRepository
                .findByUsuarioAndFilmeSerieIdAndTipoConteudo(
                        usuario,
                        dto.filmeSerieId(),
                        dto.tipoConteudo()
                )
                .orElseGet(() -> new Avaliacao(
                        usuario,
                        dto.filmeSerieId(),
                        dto.nota(),
                        comentario,
                        dto.tipoConteudo()
                ));

        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(comentario);

        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        return paraDTO(salva);
    }

    @Transactional
    public void removerAvaliacao(Usuario usuario, Long id) {
        avaliacaoRepository.delete(buscarDoUsuario(usuario, id));
    }

    private AvaliacaoDTO paraDTO(Avaliacao avaliacao) {
        return new AvaliacaoDTO(
                avaliacao.getId(),
                avaliacao.getFilmeSerieId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getUsuario().getNome(),
                avaliacao.getDataCriacao(),
                avaliacao.getTipoConteudo()
        );
    }

    @Transactional(readOnly = true)
    public ResumoAvaliacaoDTO buscarResumo(
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    ) {
        validarConteudo(filmeSerieId, tipoConteudo);

        AvaliacaoRepository.ResumoNotas resumo =
                avaliacaoRepository.buscarResumo(
                        filmeSerieId,
                        tipoConteudo
                );

        return new ResumoAvaliacaoDTO(
                filmeSerieId,
                tipoConteudo,
                resumo.getMedia(),
                resumo.getTotalAvaliacoes()
        );
    }
    @Transactional
    public AvaliacaoDTO atualizar(
            Usuario usuario,
            Long id,
            AtualizarAvaliacaoDTO dto
    ) {
        String comentario = validarEdicao(dto.nota(), dto.comentario());
        Avaliacao avaliacao = buscarDoUsuario(usuario, id);
        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(comentario);
        return paraDTO(avaliacao);
    }

    private Avaliacao buscarDoUsuario(Usuario usuario, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("O ID da avaliação deve ser positivo.");
        }
        Avaliacao avaliacao = avaliacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Avaliação não encontrada."
                ));
        if (!avaliacao.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não tem permissão para alterar esta avaliação."
            );
        }
        return avaliacao;
    }

    private static void validarConteudo(Long id, TipoConteudo tipo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("O ID do conteúdo deve ser positivo.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo do conteúdo é obrigatório.");
        }
    }

    private static String validarEdicao(Double nota, String comentario) {
        if (nota == null || !Double.isFinite(nota) || nota < 0 || nota > 10) {
            throw new IllegalArgumentException("Informe uma nota entre 0 e 10.");
        }
        if (comentario != null && comentario.length() > 1000) {
            throw new IllegalArgumentException("O comentário deve ter até 1000 caracteres.");
        }
        return comentario == null ? null : comentario.strip();
    }
}
```

## Autenticação — código completo

### Substituir `src/main/java/br/com/senai/tv_score/security/JwtService.java`

```java
package br.com.senai.tv_score.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final JwtParser parser;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET deve conter pelo menos 32 bytes "
                            + "aleatórios codificados em Base64."
            );
        }

        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "A validade do token deve ser positiva."
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String gerarToken(String email) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(agora))
                .expiration(
                        Date.from(agora.plusSeconds(expirationSeconds))
                )
                .signWith(signingKey)
                .compact();
    }

    public Optional<String> extrairEmailSeValido(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String email = claims.getSubject();
            if (claims.getExpiration() == null || email == null || email.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(email);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
```

### Substituir `src/main/java/br/com/senai/tv_score/security/JwtAuthenticationFilter.java`

```java
package br.com.senai.tv_score.security;

import br.com.senai.tv_score.usuarios.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.extrairEmailSeValido(authHeader.substring(7))
                    .flatMap(usuarioRepository::findByEmail)
                    .ifPresent(usuario -> {
                        var autenticacao = new UsernamePasswordAuthenticationToken(
                                usuario, null, Collections.emptyList()
                        );
                        autenticacao.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
```

## Catálogo e episódios — código completo

### Criar `src/main/java/br/com/senai/tv_score/filme/CacheTmdb.java`

```java
package br.com.senai.tv_score.filme;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Supplier;

final class CacheTmdb<K, V> {

    private record Entrada<V>(V valor, long carregadoEm) {}

    private final long ttlNanos;
    private final int capacidade;
    private final LinkedHashMap<K, Entrada<V>> valores = new LinkedHashMap<>(16, 0.75f, true);
    private final Object[] travas = new Object[32];

    CacheTmdb(Duration ttl, int capacidade) {
        if (ttl.isZero() || ttl.isNegative() || capacidade < 1) {
            throw new IllegalArgumentException("Configuração inválida do cache.");
        }
        this.ttlNanos = ttl.toNanos();
        this.capacidade = capacidade;
        for (int i = 0; i < travas.length; i++) {
            travas[i] = new Object();
        }
    }

    V buscar(K chave, Supplier<V> carregar) {
        V atual = valorValido(chave);
        if (atual != null) {
            return atual;
        }

        // A mesma chave não inicia dois carregamentos concorrentes.
        synchronized (travas[Math.floorMod(chave.hashCode(), travas.length)]) {
            atual = valorValido(chave);
            if (atual != null) {
                return atual;
            }

            V carregado = Objects.requireNonNull(carregar.get());
            synchronized (valores) {
                valores.put(chave, new Entrada<>(carregado, System.nanoTime()));
                while (valores.size() > capacidade) {
                    valores.pollFirstEntry();
                }
            }
            return carregado;
        }
    }

    V ultimoValor(K chave) {
        synchronized (valores) {
            Entrada<V> entrada = valores.get(chave);
            return entrada == null ? null : entrada.valor();
        }
    }

    private V valorValido(K chave) {
        synchronized (valores) {
            Entrada<V> entrada = valores.get(chave);
            return entrada != null && System.nanoTime() - entrada.carregadoEm() < ttlNanos
                    ? entrada.valor()
                    : null;
        }
    }
}
```

### Criar `src/main/java/br/com/senai/tv_score/filme/TmdbClient.java`

```java
package br.com.senai.tv_score.filme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class TmdbClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String idioma;

    public TmdbClient(
            RestTemplate restTemplate,
            @Value("${tmdb.api.url}") String apiUrl,
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.api.language}") String idioma
    ) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.idioma = idioma;
    }

    public UriComponentsBuilder consulta(String caminho) {
        return UriComponentsBuilder.fromUriString(apiUrl + caminho)
                .queryParam("api_key", apiKey)
                .queryParam("language", idioma);
    }

    public <T> T buscar(UriComponentsBuilder consulta, Class<T> tipo) {
        return buscar(consulta.build().encode().toUri(), tipo);
    }

    public <T> T buscar(URI uri, Class<T> tipo) {
        T resposta = restTemplate.getForObject(uri, tipo);
        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "O TMDB retornou uma resposta vazia."
            );
        }
        return resposta;
    }
}
```

### Substituir `src/main/java/br/com/senai/tv_score/filme/FilmeService.java`

```java
package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

@Service
public class FilmeService {

    private static final Logger LOG = LoggerFactory.getLogger(FilmeService.class);

    private record FiltroGenero(TipoConteudo tipo, Long id) {}
    private record CatalogoGeneros(GeneroResponseDTO resposta, Map<Integer, String> nomes) {}

    private final TmdbClient tmdb;
    private final CacheTmdb<TipoConteudo, CatalogoGeneros> cacheGeneros =
            new CacheTmdb<>(Duration.ofHours(6), 2);
    private final CacheTmdb<FiltroGenero, TmdbResponseDTO> cachePrimeiraPagina =
            new CacheTmdb<>(Duration.ofMinutes(5), 128);

    public FilmeService(TmdbClient tmdb) {
        this.tmdb = tmdb;
    }

    // Atende ao RF05 (Exibição de filmes em Destaque/Trending)
    public TmdbResponseDTO buscarFilmesEmAlta() {
        return consultarLista(
                tmdb.consulta("/trending/movie/week"),
                TipoConteudo.MOVIE
        );
    }

    public TmdbResponseDTO buscarSeriesEmAlta(int pagina) {
        validarPagina(pagina);

        return consultarLista(
                tmdb.consulta("/trending/tv/week")
                        .queryParam("page", pagina),
                TipoConteudo.TV
        );
    }

    // Atende ao RF04 (Pesquisa de filmes)
    public TmdbResponseDTO pesquisarFilmes(String query, int pagina) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe um texto para pesquisar."
            );
        }

        validarPagina(pagina);

        URI uri = tmdb.consulta("/search/multi")
                .queryParam("include_adult", false)
                .queryParam("page", pagina)
                .queryParam("query", "{termo}")
                .encode()
                .buildAndExpand(query.trim())
                .toUri();

        PesquisaTmdbResponseDTO resposta = tmdb.buscar(uri, PesquisaTmdbResponseDTO.class);
        Map<TipoConteudo, Map<Integer, String>> nomesPorTipo =
                new EnumMap<>(TipoConteudo.class);

        List<ResultadoPesquisaTmdbDTO> resultadosOriginais =
                resposta.results() == null
                        ? List.of()
                        : resposta.results();

        List<FilmeDTO> resultados = resultadosOriginais.stream()
                .filter(Objects::nonNull)
                .filter(ResultadoPesquisaTmdbDTO::ehFilmeOuSerie)
                .map(resultado -> {
                    TipoConteudo tipo = "tv".equals(resultado.mediaType())
                            ? TipoConteudo.TV
                            : TipoConteudo.MOVIE;

                    Map<Integer, String> nomes = resultado.genreIds() == null
                            || resultado.genreIds().isEmpty()
                            ? Map.of()
                            : nomesPorTipo.computeIfAbsent(tipo, this::buscarNomesGeneros);
                    return resultado.paraFilmeDTO(nomes);
                })
                .toList();

        return new TmdbResponseDTO(
                resultados,
                resposta.page(),
                resposta.totalPages(),
                resposta.totalResults()
        );
    }

    // Atende ao RF06 (Detalhes do conteúdo)
    public FilmeDTO buscarDetalhesFilme(Long id) {
        return buscarDetalhesConteudo(id, TipoConteudo.MOVIE);
    }

    public FilmeDTO buscarDetalhesSerie(Long id) {
        return buscarDetalhesConteudo(id, TipoConteudo.TV);
    }

    public FilmeDTO buscarDetalhesConteudo(
            Long id,
            TipoConteudo tipo
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "O ID do conteúdo deve ser positivo."
            );
        }

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Informe o tipo do conteúdo."
            );
        }

        String caminho = tipo == TipoConteudo.MOVIE
                ? "/movie/"
                : "/tv/";

        DetalhesTmdbDTO resposta = tmdb.buscar(
                tmdb.consulta(caminho + id)
                        .queryParam("append_to_response", "watch/providers"),
                DetalhesTmdbDTO.class
        );

        return resposta.paraFilmeDTO(tipo);
    }

    // Mantém a rota antiga de filmes funcionando.
    public TmdbResponseDTO buscarFilmesPorGenero(
            Long generoId,
            int pagina
    ) {
        return buscarPorGenero(generoId, TipoConteudo.MOVIE, pagina);
    }

    // Retorna a lista única de gêneros para o Flutter.
    public List<GeneroConteudoDTO> listarGenerosConteudo() {
        return Arrays.stream(GeneroConteudo.values())
                .map(genero -> new GeneroConteudoDTO(
                        genero.getCodigo(),
                        genero.getNome()
                ))
                .toList();
    }

    // Recebe um único gênero e retorna filmes e séries juntos.
    public TmdbResponseDTO buscarConteudosPorGenero(
            String codigoGenero,
            int pagina
    ) {
        validarPagina(pagina);

        GeneroConteudo genero = GeneroConteudo.porCodigo(codigoGenero);

        TmdbResponseDTO filmes = buscarPorGenero(
                genero.getGeneroFilmeId(),
                TipoConteudo.MOVIE,
                pagina
        );

        TmdbResponseDTO series = buscarPorGenero(
                genero.getGeneroSerieId(),
                TipoConteudo.TV,
                pagina
        );

        List<FilmeDTO> resultados = new ArrayList<>();

        int maiorLista = Math.max(
                filmes.resultados().size(),
                series.resultados().size()
        );

        // Uma única lista, alternando filmes e séries.
        for (int indice = 0; indice < maiorLista; indice++) {
            if (indice < filmes.resultados().size()) {
                resultados.add(filmes.resultados().get(indice));
            }

            if (indice < series.resultados().size()) {
                resultados.add(series.resultados().get(indice));
            }
        }

        int totalPaginas = Math.max(
                filmes.totalPages() == null ? 0 : filmes.totalPages(),
                series.totalPages() == null ? 0 : series.totalPages()
        );

        int totalResultados =
                (filmes.totalResults() == null ? 0 : filmes.totalResults())
                        + (series.totalResults() == null ? 0 : series.totalResults());

        return new TmdbResponseDTO(
                resultados,
                pagina,
                totalPaginas,
                totalResultados
        );
    }

    // Consulta um dos tipos internamente.
    private TmdbResponseDTO buscarPorGenero(
            Long generoId,
            TipoConteudo tipo,
            int pagina
    ) {
        validarPagina(pagina);

        if (generoId == null || generoId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do gênero deve ser positivo."
            );
        }

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Informe o tipo do conteúdo."
            );
        }

        FiltroGenero filtro = new FiltroGenero(tipo, generoId);
        TmdbResponseDTO primeiraPagina = cachePrimeiraPagina.buscar(
                filtro, () -> consultarPaginaGenero(filtro, 1)
        );
        if (pagina == 1) {
            return primeiraPagina;
        }

        if (primeiraPagina.totalPages() != null && pagina > primeiraPagina.totalPages()) {
            return new TmdbResponseDTO(
                    List.of(), pagina, primeiraPagina.totalPages(), primeiraPagina.totalResults()
            );
        }
        return consultarPaginaGenero(filtro, pagina);
    }

    private TmdbResponseDTO consultarPaginaGenero(FiltroGenero filtro, int pagina) {
        String caminho = filtro.tipo() == TipoConteudo.MOVIE ? "/discover/movie" : "/discover/tv";
        return consultarLista(
                tmdb.consulta(caminho)
                        .queryParam("with_genres", filtro.id())
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("include_adult", false)
                        .queryParam("page", pagina),
                filtro.tipo()
        );
    }

    private UriComponentsBuilder criarBuscaFilmes(int pagina) {
        validarPagina(pagina);
        return tmdb.consulta("/discover/movie")
                .queryParam("include_adult", false)
                .queryParam("page", pagina);
    }

    private TmdbResponseDTO executarBusca(
            UriComponentsBuilder builder
    ) {
        return consultarLista(builder, TipoConteudo.MOVIE);
    }

    // Lista os serviços disponíveis para filmes no Brasil.
    public StreamingResponseDTO listarStreamings() {
        return tmdb.buscar(
                tmdb.consulta("/watch/providers/movie").queryParam("watch_region", "BR"),
                StreamingResponseDTO.class
        );
    }

    // Busca filmes disponíveis por assinatura no serviço escolhido.
    public TmdbResponseDTO buscarFilmesPorStreaming(
            Long streamingId,
            int pagina
    ) {
        if (streamingId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do streaming deve ser positivo."
            );
        }

        UriComponentsBuilder builder = criarBuscaFilmes(pagina)
                .queryParam("watch_region", "BR")
                .queryParam("with_watch_providers", streamingId)
                .queryParam("with_watch_monetization_types", "flatrate")
                .queryParam("sort_by", "popularity.desc");

        return executarBusca(builder);
    }

    // Busca por faixa de notas, exigindo pelo menos 100 votos.
    public TmdbResponseDTO buscarFilmesPorNota(
            double minima,
            double maxima,
            int pagina
    ) {
        if (!Double.isFinite(minima) || !Double.isFinite(maxima)
                || minima < 0 || maxima > 10 || minima > maxima) {
            throw new IllegalArgumentException(
                    "Informe notas entre 0 e 10, com mínima menor "
                            + "ou igual à máxima."
            );
        }

        UriComponentsBuilder builder = criarBuscaFilmes(pagina)
                .queryParam("vote_average.gte", minima)
                .queryParam("vote_average.lte", maxima)
                .queryParam("vote_count.gte", 100)
                .queryParam("sort_by", "vote_average.desc");

        return executarBusca(builder);
    }

    // Busca pelo ano do lançamento principal do filme.
    public TmdbResponseDTO buscarFilmesPorAno(
            int ano,
            int pagina
    ) {
        if (ano < 1 || ano > 9999) {
            throw new IllegalArgumentException(
                    "Informe um ano entre 1 e 9999."
            );
        }

        UriComponentsBuilder builder = criarBuscaFilmes(pagina)
                .queryParam("primary_release_year", ano)
                .queryParam("sort_by", "popularity.desc");

        return executarBusca(builder);
    }

    public TmdbResponseDTO buscarCatalogo(int pagina) {
        UriComponentsBuilder builder = criarBuscaFilmes(pagina)
                .queryParam("sort_by", "popularity.desc");

        return executarBusca(builder);
    }

    private void validarPagina(int pagina) {
        if (pagina < 1 || pagina > 500) {
            throw new IllegalArgumentException(
                    "A página deve estar entre 1 e 500."
            );
        }
    }

    private TmdbResponseDTO consultarLista(
            UriComponentsBuilder builder,
            TipoConteudo tipo
    ) {
        TmdbResponseDTO resposta = tmdb.buscar(builder, TmdbResponseDTO.class);

        List<FilmeDTO> resultados = resposta.resultados() == null
                ? List.of()
                : resposta.resultados().stream()
                .filter(Objects::nonNull)
                .map(conteudo -> conteudo.comTipo(tipo))
                .toList();

        return new TmdbResponseDTO(
                resultados,
                resposta.page(),
                resposta.totalPages(),
                resposta.totalResults()
        );
    }

    public GeneroResponseDTO listarGeneros() {
        return catalogoGeneros(TipoConteudo.MOVIE).resposta();
    }

    private Map<Integer, String> buscarNomesGeneros(TipoConteudo tipo) {
        try {
            return catalogoGeneros(tipo).nomes();
        } catch (RestClientException | ResponseStatusException ex) {
            // A pesquisa já recebeu os títulos; preserva a resposta se o enriquecimento falhar.
            LOG.warn("Não foi possível atualizar os gêneros para {}.", tipo);
            CatalogoGeneros anterior = cacheGeneros.ultimoValor(tipo);
            return anterior == null ? Map.of() : anterior.nomes();
        }
    }

    private CatalogoGeneros catalogoGeneros(TipoConteudo tipo) {
        return cacheGeneros.buscar(tipo, () -> {
            String caminho = tipo == TipoConteudo.MOVIE ? "/genre/movie/list" : "/genre/tv/list";
            GeneroResponseDTO resposta = tmdb.buscar(tmdb.consulta(caminho), GeneroResponseDTO.class);
            if (resposta.genres() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "O TMDB não retornou a lista de gêneros."
                );
            }
            Map<Integer, String> nomes = new HashMap<>();
            for (GeneroDTO genero : resposta.genres()) {
                if (genero != null && genero.id() != null
                        && genero.name() != null && !genero.name().isBlank()) {
                    nomes.put(Math.toIntExact(genero.id()), genero.name());
                }
            }
            return new CatalogoGeneros(resposta, Map.copyOf(nomes));
        });
    }
}
```

### Substituir `src/main/java/br/com/senai/tv_score/filme/EpisodioService.java`

```java
package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.EpisodioDTO;
import br.com.senai.tv_score.dto.PainelEpisodiosDTO;
import br.com.senai.tv_score.dto.TemporadaDTO;
import br.com.senai.tv_score.dto.TemporadasSerieDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class EpisodioService {

    private record ChaveTemporada(Long serieId, int temporada) {}

    private final TmdbClient tmdb;
    private final CacheTmdb<Long, TemporadasSerieDTO> cacheSeries =
            new CacheTmdb<>(Duration.ofMinutes(15), 128);
    private final CacheTmdb<ChaveTemporada, TemporadaDTO> cacheTemporadas =
            new CacheTmdb<>(Duration.ofMinutes(15), 128);

    public EpisodioService(TmdbClient tmdb) {
        this.tmdb = tmdb;
    }

    public TemporadaDTO buscarTemporada(
            Long serieId,
            int temporada
    ) {
        validarSerie(serieId);
        validarTemporada(temporada);

        return cacheTemporadas.buscar(
                new ChaveTemporada(serieId, temporada),
                () -> carregarTemporada(serieId, temporada)
        );
    }

    private TemporadaDTO carregarTemporada(Long serieId, int temporada) {
        TemporadaDTO resposta = consultar(
                "/tv/" + serieId + "/season/" + temporada,
                TemporadaDTO.class
        );

        List<EpisodioDTO> episodios = resposta.episodes() == null
                ? List.of()
                : resposta.episodes().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        EpisodioDTO::numero,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ))
                .toList();

        return new TemporadaDTO(
                resposta.id(),
                resposta.name(),
                resposta.overview(),
                resposta.numero(),
                resposta.poster(),
                episodios
        );
    }

    public EpisodioDTO buscarDetalhes(
            Long serieId,
            int temporada,
            int episodio
    ) {
        validarSerie(serieId);
        validarTemporada(temporada);

        if (episodio < 1) {
            throw new IllegalArgumentException(
                    "O número do episódio deve ser positivo."
            );
        }

        return consultar(
                "/tv/" + serieId
                        + "/season/" + temporada
                        + "/episode/" + episodio,
                EpisodioDTO.class
        );
    }

    public PainelEpisodiosDTO buscarPainel(
            Long serieId,
            int limite,
            int minimoVotos,
            boolean incluirEspeciais
    ) {
        validarSerie(serieId);

        if (limite < 1 || limite > 50) {
            throw new IllegalArgumentException(
                    "O limite deve estar entre 1 e 50."
            );
        }

        if (minimoVotos < 1) {
            throw new IllegalArgumentException(
                    "O mínimo de votos deve ser maior que zero."
            );
        }

        TemporadasSerieDTO serie = cacheSeries.buscar(serieId, () -> {
            TemporadasSerieDTO resposta = consultar("/tv/" + serieId, TemporadasSerieDTO.class);
            if (resposta.seasons() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "O TMDB não retornou a lista de temporadas."
                );
            }
            return resposta;
        });

        List<Integer> numeros = serie.seasons().stream()
                .filter(Objects::nonNull)
                .map(TemporadasSerieDTO.ResumoTemporada::numero)
                .filter(Objects::nonNull)
                .filter(numero -> numero >= 0)
                .filter(numero -> incluirEspeciais || numero > 0)
                .distinct()
                .sorted()
                .toList();

        List<TemporadaDTO> temporadas = new ArrayList<>();

        for (Integer numero : numeros) {
            temporadas.add(buscarTemporada(serieId, numero));
        }

        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);

        List<EpisodioDTO> topRated = temporadas.stream()
                .flatMap(temporada -> temporada.episodes().stream())
                .filter(EpisodioDTO::possuiNota)
                .filter(episodio ->
                        episodio.quantidadeVotos() >= minimoVotos
                )
                .filter(episodio ->
                        episodio.dataExibicao() != null
                                && !episodio.dataExibicao().isAfter(hoje)
                )
                .sorted(
                        Comparator
                                .comparing(
                                        EpisodioDTO::nota,
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        EpisodioDTO::quantidadeVotos,
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        EpisodioDTO::temporada,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                                .thenComparing(
                                        EpisodioDTO::numero,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                )
                .limit(limite)
                .toList();

        return new PainelEpisodiosDTO(
                serieId,
                "TMDB",
                List.copyOf(temporadas),
                topRated
        );
    }

    private <T> T consultar(String caminho, Class<T> tipo) {
        return tmdb.buscar(tmdb.consulta(caminho), tipo);
    }

    private void validarSerie(Long serieId) {
        if (serieId == null || serieId <= 0) {
            throw new IllegalArgumentException(
                    "O ID da série deve ser positivo."
            );
        }
    }

    private void validarTemporada(int temporada) {
        if (temporada < 0) {
            throw new IllegalArgumentException(
                    "A temporada deve ser zero ou um número positivo."
            );
        }
    }
}
```

## Testes de regressão — código completo

### Criar `src/test/java/br/com/senai/tv_score/avaliacao/AvaliacaoServiceTest.java`

```java
package br.com.senai.tv_score.avaliacao;

import br.com.senai.tv_score.dto.AtualizarAvaliacaoDTO;
import br.com.senai.tv_score.dto.AvaliacaoDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvaliacaoServiceTest {

    private final AvaliacaoRepository repositorio = mock(AvaliacaoRepository.class);
    private final AvaliacaoService service = new AvaliacaoService(repositorio);
    private final Usuario usuario = usuario(1L);

    @Test
    void aplicaLimiteDeComentarioNaCriacaoENaEdicao() {
        String comentario = "x".repeat(1001);
        assertThrows(IllegalArgumentException.class,
                () -> service.avaliar(usuario, entrada(TipoConteudo.MOVIE, comentario)));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(usuario, 1L, new AtualizarAvaliacaoDTO(8.0, comentario)));
        verifyNoInteractions(repositorio);
    }

    @Test
    void impedeQueOutroUsuarioEditeOuRemova() {
        Avaliacao avaliacao = new Avaliacao(usuario(2L), 550L, 8.0, "Original", TipoConteudo.MOVIE);
        when(repositorio.findById(12L)).thenReturn(Optional.of(avaliacao));

        ResponseStatusException editar = assertThrows(ResponseStatusException.class,
                () -> service.atualizar(usuario, 12L, new AtualizarAvaliacaoDTO(9.0, "Alterado")));
        ResponseStatusException remover = assertThrows(ResponseStatusException.class,
                () -> service.removerAvaliacao(usuario, 12L));

        assertEquals(HttpStatus.FORBIDDEN, editar.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, remover.getStatusCode());
        assertEquals("Original", avaliacao.getComentario());
    }

    @Test
    void serieNaoSobrescreveFilmeComOMesmoIdTmdb() {
        Avaliacao filme = new Avaliacao(usuario, 550L, 8.0, "Original", TipoConteudo.MOVIE);
        when(repositorio.findByUsuarioAndFilmeSerieIdAndTipoConteudo(usuario, 550L, TipoConteudo.MOVIE))
                .thenReturn(Optional.of(filme));
        when(repositorio.findByUsuarioAndFilmeSerieIdAndTipoConteudo(usuario, 550L, TipoConteudo.TV))
                .thenReturn(Optional.empty());
        when(repositorio.save(any(Avaliacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        AvaliacaoDTO serie = service.avaliar(usuario, entrada(TipoConteudo.TV, "  Série  "));

        assertEquals(TipoConteudo.TV, serie.tipoConteudo());
        assertEquals("Série", serie.comentario());
        assertEquals("Original", filme.getComentario());
    }

    private static AvaliacaoDTO entrada(TipoConteudo tipo, String comentario) {
        return new AvaliacaoDTO(null, 550L, 8.0, comentario, null, null, tipo);
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário " + id);
        return usuario;
    }
}
```

### Criar `src/test/java/br/com/senai/tv_score/filme/CacheTmdbTest.java`

```java
package br.com.senai.tv_score.filme;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTmdbTest {

    @Test
    void compartilhaCarregamentoDaMesmaTemporada() throws Exception {
        var cache = new CacheTmdb<String, Integer>(Duration.ofMinutes(1), 2);
        var carregamentos = new AtomicInteger();
        var iniciou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var primeiro = executor.submit(() -> cache.buscar("temporada-1", () -> {
                    iniciou.countDown();
                    try {
                        if (!liberar.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Carregamento não liberado.");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    }
                    return carregamentos.incrementAndGet();
                }));
                assertTrue(iniciou.await(5, TimeUnit.SECONDS));
                var segundo = executor.submit(() -> cache.buscar(
                        "temporada-1", carregamentos::incrementAndGet
                ));
                liberar.countDown();
                assertEquals(1, primeiro.get(5, TimeUnit.SECONDS).intValue());
                assertEquals(1, segundo.get(5, TimeUnit.SECONDS).intValue());
                assertEquals(1, carregamentos.get());
            } finally {
                liberar.countDown();
            }
        }
    }

    @Test
    void removeEntradaMenosUsadaAoAtingirCapacidade() {
        var cache = new CacheTmdb<String, Integer>(Duration.ofMinutes(1), 2);
        cache.buscar("a", () -> 1);
        cache.buscar("b", () -> 2);
        assertEquals(1, cache.buscar("a", () -> 99).intValue());
        cache.buscar("c", () -> 3);
        assertNull(cache.ultimoValor("b"));
        assertEquals(1, cache.ultimoValor("a").intValue());
    }
}
```

## Como verificar depois de aplicar

No PowerShell, com JDK 21 e acesso ao Maven Central:

```powershell
.\mvnw.cmd "-Dtest=AvaliacaoServiceTest,CacheTmdbTest" test
```

Os dois testes novos não precisam abrir conexão com o MySQL. O `contextLoads` existente precisa de configuração e banco de teste para rodar com `clean test`.

No Swagger local, confira também:

1. Pesquise filmes e séries e verifique `results[].genres`. Em destaques, confira que `genre_ids` continua disponível.
2. Abra os detalhes de um título e confira `genres` e, quando houver dados para o Brasil, `ondeAssistir`.
3. Chame o painel de episódios duas vezes dentro da janela do cache. Confira o mesmo formato de resposta e, se medir as chamadas externas, verifique o reaproveitamento das temporadas.
4. Com dois usuários de teste, confira que o dono pode editar a avaliação e o outro recebe 403. Faça o caso de um filme e uma série com o mesmo ID TMDB no **banco de teste**.

Foi feita a verificação de sintaxe dos arquivos Java e um teste isolado do cache com Java 21. O Maven não conseguiu resolver as dependências neste ambiente, então o build completo e os testes JUnit não foram executados.

## Fora desta etapa

A migração de banco, a paginação de listas de usuários e a padronização dos gêneros em todas as rotas ficam para mudanças separadas. Antes de propor a migração, confira os índices do ambiente com `SHOW INDEX FROM avaliacao;` e `SHOW INDEX FROM favorito;`. Não remova índices com base apenas no código das entidades. A padronização do JSON exige combinar o contrato com o Flutter.
