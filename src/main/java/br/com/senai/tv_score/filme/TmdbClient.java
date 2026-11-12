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
            @org.springframework.beans.factory.annotation.Qualifier("restTemplate") RestTemplate restTemplate,
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
        T resposta;
        try { resposta = restTemplate.getForObject(uri, tipo); }
        catch (org.springframework.web.client.RestClientResponseException ex) {
            int codigo = ex.getStatusCode().value();
            HttpStatus status = codigo == 404 ? HttpStatus.NOT_FOUND
                : codigo == 429 || codigo == 503 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Falha na consulta ao TMDB (HTTP " + codigo + ").");
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            Throwable causa = ex;
            while (causa != null) {
                if (causa instanceof java.net.SocketTimeoutException || causa instanceof java.net.http.HttpTimeoutException)
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "O TMDB demorou para responder.");
                causa = causa.getCause();
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB indisponivel.");
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta invalida do TMDB.");
        }
        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "O TMDB retornou uma resposta vazia."
            );
        }
        return resposta;
    }
}
