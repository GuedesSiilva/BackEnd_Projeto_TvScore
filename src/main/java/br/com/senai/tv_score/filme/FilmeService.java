package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.util.Map;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

@Service
public class FilmeService {


    private record FiltroGenero(TipoConteudo tipo, Long id) {}

    private final TmdbClient tmdb;
    private final GeneroService generos;
    private final CacheTmdb<FiltroGenero, TmdbResponseDTO> cachePrimeiraPagina =
            new CacheTmdb<>(Duration.ofMinutes(5), 128);

    public FilmeService(TmdbClient tmdb, GeneroService generos) {
        this.tmdb = tmdb;
        this.generos = generos;
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
                            : nomesPorTipo.computeIfAbsent(tipo, generos::buscarNomesGeneros);
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
        return generos.listarGenerosDisponiveis().stream()
                .map(genero -> new GeneroConteudoDTO(
                        genero.codigo(),
                        genero.nome()
                ))
                .toList();
    }

    // Recebe um único gênero e retorna filmes e séries juntos.
    public TmdbResponseDTO buscarConteudosPorGenero(
            String codigoGenero,
            int pagina
    ) {
        validarPagina(pagina);

        GeneroService.GeneroDisponivel genero = generos.resolverGenero(codigoGenero);

        TmdbResponseDTO filmes = genero.generoFilmeId() == null
                ? paginaVazia(pagina)
                : buscarPorGenero(
                genero.generoFilmeId(),
                TipoConteudo.MOVIE,
                pagina
        );

        TmdbResponseDTO series = genero.generoSerieId() == null
                ? paginaVazia(pagina)
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
                        + (series.totalResults() == null
                        ? 0
                        : series.totalResults());

        return new TmdbResponseDTO(
                resultados,
                pagina,
                totalPaginas,
                totalResultados
        );
    }

    private TmdbResponseDTO paginaVazia(int pagina) {
        return new TmdbResponseDTO(
                List.of(),
                pagina,
                0,
                0
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
            String ids = generos.idsFilmes(codigos).stream().map(String::valueOf)
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
                .map(conteudo -> conteudo.comGeneros(tipo, generos.buscarNomesGeneros(tipo)))
                .toList();

        return new TmdbResponseDTO(
                resultados,
                resposta.page(),
                resposta.totalPages(),
                resposta.totalResults()
        );
    }

    public GeneroResponseDTO listarGeneros() { return generos.listarGeneros(); }
}
