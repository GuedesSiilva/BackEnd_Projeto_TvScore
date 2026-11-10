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
