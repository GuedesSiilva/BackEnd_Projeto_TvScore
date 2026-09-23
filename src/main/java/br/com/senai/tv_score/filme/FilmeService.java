package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.FilmeDTO;
import br.com.senai.tv_score.dto.TmdbResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FilmeService {

    @Value("${tmdb.api.url}")
    private String apiUrl;

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.language}")
    private String language;

    private final RestTemplate restTemplate;

    public FilmeService() {
        this.restTemplate = new RestTemplate();
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
}