package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.FilmeDTO;
import br.com.senai.tv_score.dto.TmdbResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import br.com.senai.tv_score.dto.GeneroResponseDTO;
import org.springframework.web.util.UriComponentsBuilder;
import br.com.senai.tv_score.dto.StreamingResponseDTO;


import java.net.URI;

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
        String url = apiUrl + "/trending/movie/week?api_key=" + apiKey + "&language=" + language;
        return restTemplate.getForObject(url, TmdbResponseDTO.class);
    }

    // Atende ao RF04 (Pesquisa de filmes)
    public TmdbResponseDTO pesquisarFilmes(String query) {
        String url = apiUrl + "/search/movie?api_key=" + apiKey + "&language=" + language + "&query=" + query;
        return restTemplate.getForObject(url, TmdbResponseDTO.class);
    }

    // Atende ao RF06 (Detalhes do conteúdo)
    public FilmeDTO buscarDetalhesFilme(Long id) {
        String url = apiUrl + "/movie/" + id + "?api_key=" + apiKey + "&language=" + language;
        return restTemplate.getForObject(url, FilmeDTO.class);
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

    public TmdbResponseDTO buscarFilmesPorGenero(Long generoId, int pagina) {
        if (generoId <= 0) {
            throw new IllegalArgumentException("O ID do gênero deve ser positivo.");
        }

        if (pagina < 1) {
            throw new IllegalArgumentException("A página deve ser maior que zero.");
        }

        URI uri = UriComponentsBuilder
                .fromUriString(apiUrl + "/discover/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", language)
                .queryParam("with_genres", generoId)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("include_adult", false)
                .queryParam("page", pagina)
                .build()
                .encode()
                .toUri();

        return restTemplate.getForObject(uri, TmdbResponseDTO.class);
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
}