package br.com.senai.tv_score.recomendacao;

import br.com.senai.tv_score.dto.RecomendacoesDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class RecomendacaoService {
    private final RestTemplate cliente;
    private final URI filmes;
    private final URI series;
    private final String tokenInterno;

    public RecomendacaoService(
            @Value("${recomendacao.api.url}") String apiUrl,
            @Qualifier("recomendadorHttp") RestTemplate cliente,
            @Value("${recomendacao.api.token-interno:}") String tokenInterno
    ) {
        URI base = URI.create(apiUrl);
        if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null
                || base.getFragment() != null || !("http".equalsIgnoreCase(base.getScheme())
                || "https".equalsIgnoreCase(base.getScheme()))) {
            throw new IllegalArgumentException("Configure uma URL HTTP ou HTTPS válida para o recomendador.");
        }
        if (tokenInterno == null || tokenInterno.length() < 32
                || tokenInterno.chars().anyMatch(c -> c < 33 || c > 126)) {
            throw new IllegalArgumentException("Configure IA_INTERNAL_TOKEN com ao menos 32 caracteres aleatórios.");
        }
        this.cliente = cliente;
        this.tokenInterno = tokenInterno;
        filmes = UriComponentsBuilder.fromUri(base).pathSegment("recomendacoes", "filmes", "java").build().toUri();
        series = UriComponentsBuilder.fromUri(base).pathSegment("recomendacoes", "series", "java").build().toUri();
    }

    public RecomendacoesDTO buscarFilmes(String authorization) {
        return consultar(authorization, filmes, TipoConteudo.MOVIE);
    }

    public RecomendacoesDTO buscarSeries(String authorization) {
        return consultar(authorization, series, TipoConteudo.TV);
    }

    private RecomendacoesDTO consultar(String authorization, URI destino, TipoConteudo tipo) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Envie um token Bearer válido.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.set("X-TVScore-Internal-Token", tokenInterno);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            var resposta = cliente.exchange(destino, HttpMethod.GET, new HttpEntity<Void>(headers), RecomendacoesDTO.class);
            if (!resposta.getStatusCode().is2xxSuccessful()) throw invalida();
            return validar(resposta.getBody(), tipo);
        } catch (RestClientResponseException ex) {
            int codigo = ex.getStatusCode().value();
            if (codigo == 401 || codigo == 403) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(codigo), "Não foi possível autorizar o acesso às recomendações.");
            }
            if (codigo == 429 || codigo == 503) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O recomendador está temporariamente indisponível.");
            }
            if (codigo == 504) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "O recomendador demorou para responder.");
            }
            throw invalida();
        } catch (ResourceAccessException ex) {
            for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
                if (causa instanceof SocketTimeoutException || causa instanceof java.net.http.HttpTimeoutException) {
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "O recomendador demorou para responder.");
                }
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar ao recomendador.");
        } catch (RestClientException ex) {
            throw invalida();
        }
    }

    private RecomendacoesDTO validar(RecomendacoesDTO corpo, TipoConteudo tipo) {
        if (corpo == null || corpo.recomendacoes() == null || corpo.total() == null
                || corpo.total() != corpo.recomendacoes().size()) throw invalida();
        Map<String, Double> preferencias = corpo.preferencias() == null ? Map.of() : corpo.preferencias();
        if (preferencias.entrySet().stream().anyMatch(e -> e.getKey() == null
                || e.getValue() == null || !Double.isFinite(e.getValue()))) throw invalida();
        var vistos = new HashSet<Long>();
        List<RecomendacoesDTO.Item> itens = corpo.recomendacoes().stream().map(item -> {
            if (item == null || item.id() == null || item.id() <= 0 || !vistos.add(item.id())
                    || item.titulo() == null || item.titulo().isBlank()
                    || !numero(item.nota(), 0, 10) || !numero(item.scoreRecomendacao(), 0, 100)
                    || item.tipoConteudo() != null && item.tipoConteudo() != tipo
                    || item.similaridadeSinopse() != null && !numero(item.similaridadeSinopse(), 0, 1)
                    || item.generos() == null || item.generos().stream().anyMatch(g -> g == null || g <= 0)) {
                throw invalida();
            }
            return new RecomendacoesDTO.Item(item.id(), item.titulo(), item.sinopse(), item.nota(),
                    item.generos(), item.poster(), item.scoreRecomendacao(), item.motivoRecomendacao(),
                    item.similaridadeSinopse(), tipo);
        }).toList();
        List<String> avisos = corpo.avisos() == null ? List.of() : corpo.avisos();
        if (avisos.stream().anyMatch(a -> a == null || a.isBlank())) throw invalida();
        return new RecomendacoesDTO(preferencias, itens.size(), itens,
                Boolean.TRUE.equals(corpo.parcial()) || !avisos.isEmpty(), avisos);
    }

    private static boolean numero(Double valor, double minimo, double maximo) {
        return valor != null && Double.isFinite(valor) && valor >= minimo && valor <= maximo;
    }

    private static ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O recomendador retornou uma resposta inválida.");
    }
}
