package br.com.senai.tv_score.recomendacao;

import br.com.senai.tv_score.dto.RecomendacoesDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;

@Service
public class RecomendacaoService {

    private final RestTemplate cliente;
    private final URI endpoint;

    public RecomendacaoService(
            @Value("${recomendacao.api.url}") String apiUrl,
            @org.springframework.beans.factory.annotation.Qualifier("recomendadorHttp") RestTemplate cliente
    ) {

        URI base = URI.create(apiUrl);

        if (base.getHost() == null
                || !("http".equalsIgnoreCase(base.getScheme())
                || "https".equalsIgnoreCase(base.getScheme()))
                || base.getUserInfo() != null
                || base.getQuery() != null
                || base.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Configure uma URL HTTP ou HTTPS válida para o recomendador."
            );
        }

        this.endpoint = UriComponentsBuilder
                .fromUri(base)
                .pathSegment("recomendacoes", "filmes", "java")
                .build()
                .toUri();

        this.cliente = cliente;
    }

    public RecomendacoesDTO buscarFilmes(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(
                true, 0, "Bearer ", 0, 7
        )
                || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Envie um token Bearer válido."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<RecomendacoesDTO> resposta =
                    cliente.exchange(
                            endpoint,
                            HttpMethod.GET,
                            new HttpEntity<Void>(headers),
                            RecomendacoesDTO.class
                    );

            RecomendacoesDTO corpo = resposta.getBody();

            if (corpo == null || corpo.recomendacoes() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "O recomendador retornou uma resposta inválida."
                );
            }

            return corpo;

        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();

            if (status == 401 || status == 403) {
                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(status),
                        "Não foi possível autorizar o acesso às recomendações."
                );
            }

            if (status == 429 || status == 503) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "O recomendador está temporariamente indisponível."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O serviço de recomendações não conseguiu concluir a consulta."
            );

        } catch (ResourceAccessException ex) {
            Throwable causa = ex;

            while (causa != null) {
                if (causa instanceof SocketTimeoutException) {
                    throw new ResponseStatusException(
                            HttpStatus.GATEWAY_TIMEOUT,
                            "O recomendador demorou para responder."
                    );
                }

                causa = causa.getCause();
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível conectar ao recomendador."
            );

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Não foi possível interpretar a resposta do recomendador."
            );
        }
    }
}