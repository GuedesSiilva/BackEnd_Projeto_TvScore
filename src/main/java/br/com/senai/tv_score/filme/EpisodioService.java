package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.EpisodioDTO;
import br.com.senai.tv_score.dto.PainelEpisodiosDTO;
import br.com.senai.tv_score.dto.TemporadaDTO;
import br.com.senai.tv_score.dto.TemporadasSerieDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class EpisodioService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String language;

    public EpisodioService(
            RestTemplate restTemplate,
            @Value("${tmdb.api.url}") String apiUrl,
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.api.language}") String language
    ) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.language = language;
    }

    public TemporadaDTO buscarTemporada(
            Long serieId,
            int temporada
    ) {
        validarSerie(serieId);
        validarTemporada(temporada);

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

        TemporadasSerieDTO serie = consultar(
                "/tv/" + serieId,
                TemporadasSerieDTO.class
        );

        if (serie.seasons() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O TMDB não retornou a lista de temporadas."
            );
        }

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
        URI uri = UriComponentsBuilder
                .fromUriString(apiUrl + caminho)
                .queryParam("api_key", apiKey)
                .queryParam("language", language)
                .build()
                .encode()
                .toUri();

        T resposta = restTemplate.getForObject(uri, tipo);

        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O TMDB retornou uma resposta vazia."
            );
        }

        return resposta;
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