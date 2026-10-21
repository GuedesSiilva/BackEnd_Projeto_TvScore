package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;


import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class FilmeService {

    @Value("${tmdb.api.url}")
    private String apiUrl;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.language}")
    private String language;

    private final RestTemplate restTemplate;

    public FilmeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Atende ao RF05 (Exibição de filmes em Destaque/Trending)
    public TmdbResponseDTO buscarFilmesEmAlta() {
        return consultarLista(
                criarConsultaTmdb("/trending/movie/week"),
                TipoConteudo.MOVIE
        );
    }

    public TmdbResponseDTO buscarSeriesEmAlta(int pagina) {
        validarPagina(pagina);

        return consultarLista(
                criarConsultaTmdb("/trending/tv/week")
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

        if (pagina < 1 || pagina > 500) {
            throw new IllegalArgumentException(
                    "A página deve estar entre 1 e 500."
            );
        }

        URI uri = UriComponentsBuilder
                .fromUriString(apiUrl + "/search/multi")
                .queryParam("api_key", apiKey)
                .queryParam("language", language)
                .queryParam("include_adult", false)
                .queryParam("page", pagina)
                .queryParam("query", "{termo}")
                .encode()
                .buildAndExpand(query.trim())
                .toUri();

        PesquisaTmdbResponseDTO resposta = restTemplate.getForObject(
                uri,
                PesquisaTmdbResponseDTO.class
        );

        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O TMDB retornou uma resposta vazia."
            );
        }

        List<ResultadoPesquisaTmdbDTO> resultadosOriginais =
                resposta.results() == null
                        ? List.of()
                        : resposta.results();

        List<FilmeDTO> resultados = resultadosOriginais.stream()
                .filter(Objects::nonNull)
                .filter(ResultadoPesquisaTmdbDTO::ehFilmeOuSerie)
                .map(ResultadoPesquisaTmdbDTO::paraFilmeDTO)
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

        URI uri = criarConsultaTmdb(caminho + id)
                .build()
                .encode()
                .toUri();

        FilmeDTO resposta = restTemplate.getForObject(
                uri,
                FilmeDTO.class
        );

        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O TMDB retornou uma resposta vazia."
            );
        }

        return resposta.comTipo(tipo);
    }

    public GeneroResponseDTO listarGeneros() {
        URI uri = UriComponentsBuilder
                .fromUriString(apiUrl + "/genre/movie/list")
                .queryParam("api_key", apiKey)
                .queryParam("language", language)
                .build()
                .encode()
                .toUri();

        return restTemplate.getForObject(uri, GeneroResponseDTO.class);
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

        String caminho = tipo == TipoConteudo.MOVIE
                ? "/discover/movie"
                : "/discover/tv";

        // Filmes e séries podem ter quantidades diferentes de páginas.
        if (pagina > 1) {
            TmdbResponseDTO primeiraPagina = buscarPorGenero(
                    generoId,
                    tipo,
                    1
            );

            if (primeiraPagina.totalPages() != null
                    && pagina > primeiraPagina.totalPages()) {
                return new TmdbResponseDTO(
                        List.of(),
                        pagina,
                        primeiraPagina.totalPages(),
                        primeiraPagina.totalResults()
                );
            }
        }

        return consultarLista(
                criarConsultaTmdb(caminho)
                        .queryParam("with_genres", generoId)
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("include_adult", false)
                        .queryParam("page", pagina),
                tipo
        );
    }

    // Configuração compartilhada pelas novas buscas.
    private UriComponentsBuilder criarBuscaFilmes(int pagina) {
        if (pagina < 1) {
            throw new IllegalArgumentException(
                    "A página deve ser maior que zero."
            );
        }

        return UriComponentsBuilder
                .fromUriString(apiUrl + "/discover/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", language)
                .queryParam("include_adult", false)
                .queryParam("page", pagina);
    }

    private TmdbResponseDTO executarBusca(UriComponentsBuilder builder) {
        URI uri = builder.build().encode().toUri();

        return restTemplate.getForObject(uri, TmdbResponseDTO.class);
    }

    // Lista os serviços disponíveis para filmes no Brasil.
    public StreamingResponseDTO listarStreamings() {
        URI uri = UriComponentsBuilder
                .fromUriString(apiUrl + "/watch/providers/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", language)
                .queryParam("watch_region", "BR")
                .build()
                .encode()
                .toUri();

        return restTemplate.getForObject(
                uri,
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
    private UriComponentsBuilder criarConsultaTmdb(String caminho) {
        return UriComponentsBuilder
                .fromUriString(apiUrl + caminho)
                .queryParam("api_key", apiKey)
                .queryParam("language", language);
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
        URI uri = builder.build().encode().toUri();

        TmdbResponseDTO resposta = restTemplate.getForObject(
                uri,
                TmdbResponseDTO.class
        );

        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O TMDB retornou uma resposta vazia."
            );
        }

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
}