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
import java.util.LinkedHashMap;
import java.util.Comparator;
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
        return generosConteudo().values().stream()
                .sorted(Comparator.comparing(GeneroConteudo::codigo))
                .map(genero -> new GeneroConteudoDTO(genero.codigo(), genero.nome()))
                .toList();
    }

    private Map<String, GeneroConteudo> generosConteudo() {
        Map<String, GeneroConteudo> generos = new LinkedHashMap<>();
        for (TipoConteudo tipo : TipoConteudo.values()) {
            catalogoGeneros(tipo).nomes().forEach((id, nome) -> {
                String codigo = GeneroConteudo.codigoDoNome(nome);
                GeneroConteudo anterior = generos.get(codigo);
                Long filmeId = tipo == TipoConteudo.MOVIE ? Long.valueOf(id.longValue())
                        : anterior == null ? null : anterior.generoFilmeId();
                Long serieId = tipo == TipoConteudo.TV ? Long.valueOf(id.longValue())
                        : anterior == null ? null : anterior.generoSerieId();
                generos.put(codigo, new GeneroConteudo(codigo, nome, filmeId, serieId));
            });
        }
        return generos;
    }

    public GeneroConteudo buscarGeneroConteudo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Informe um gênero.");
        }
        GeneroConteudo genero = generosConteudo().get(
                codigo.trim().toLowerCase(java.util.Locale.ROOT)
        );
        if (genero == null) {
            throw new IllegalArgumentException(
                    "Gênero inválido. Consulte /api/conteudos/generos."
            );
        }
        return genero;
    }

    // Recebe um único gênero e retorna filmes e séries juntos.
    public TmdbResponseDTO buscarConteudosPorGenero(
            String codigoGenero,
            int pagina
    ) {
        validarPagina(pagina);

        GeneroConteudo genero = buscarGeneroConteudo(codigoGenero);

        TmdbResponseDTO filmes = genero.generoFilmeId() == null
                ? new TmdbResponseDTO(List.of(), pagina, 0, 0)
                : buscarPorGenero(
                genero.generoFilmeId(),
                TipoConteudo.MOVIE,
                pagina
        );

        TmdbResponseDTO series = genero.generoSerieId() == null
                ? new TmdbResponseDTO(List.of(), pagina, 0, 0)
                : buscarPorGenero(
                genero.generoSerieId(),
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

    public TmdbResponseDTO buscarOpcoesFilmesPreferidos(java.util.Set<String> codigos, int pagina) {
        UriComponentsBuilder consulta = criarBuscaFilmes(pagina)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("vote_count.gte", 100);
        if (!codigos.isEmpty()) {
            String ids = catalogoGeneros(TipoConteudo.MOVIE).nomes().entrySet().stream()
                    .filter(genero -> codigos.contains(GeneroConteudo.codigoDoNome(genero.getValue())))
                    .map(Map.Entry::getKey).sorted().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining("|"));
            // Um gênero pode existir somente em séries. Nesse caso, oferece filmes populares.
            if (!ids.isEmpty()) {
                consulta.queryParam("with_genres", ids);
            }
        }
        return consultarLista(consulta, TipoConteudo.MOVIE);
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
                if (genero != null && genero.id() != null && genero.id() > 0
                        && genero.name() != null && !genero.name().isBlank()) {
                    nomes.put(Math.toIntExact(genero.id()), genero.name());
                }
            }
            return new CatalogoGeneros(resposta, Map.copyOf(nomes));
        });
    }
}
