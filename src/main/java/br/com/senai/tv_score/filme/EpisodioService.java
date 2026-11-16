package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.EpisodioDTO;
import br.com.senai.tv_score.dto.PainelEpisodiosDTO;
import br.com.senai.tv_score.dto.TemporadaDTO;
import br.com.senai.tv_score.dto.TemporadasSerieDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class EpisodioService {
    private final java.util.concurrent.ExecutorService executor = new java.util.concurrent.ThreadPoolExecutor(4, 4, 0,
            java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(16),
            new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    private final java.util.concurrent.Semaphore operacoes = new java.util.concurrent.Semaphore(4);
    @org.springframework.beans.factory.annotation.Value("${episodios.prazo-ms:20000}")
    private long prazoMs = 20000;
    @org.springframework.beans.factory.annotation.Value("${episodios.max-temporadas:100}")
    private int maxTemporadas = 100;
    @jakarta.annotation.PreDestroy
    void encerrar() { executor.shutdownNow(); }

    private record ChaveTemporada(Long serieId, int temporada) {}

    private final TmdbClient tmdb;
    private final CacheTmdb<Long, TemporadasSerieDTO> cacheSeries =
            new CacheTmdb<>(Duration.ofMinutes(15), 128);
    private final CacheTmdb<ChaveTemporada, TemporadaDTO> cacheTemporadas =
            new CacheTmdb<>(Duration.ofMinutes(15), 128);

    public EpisodioService(TmdbClient tmdb) {
        this.tmdb = tmdb;
    }

    public TemporadaDTO buscarTemporada(
            Long serieId,
            int temporada
    ) {
        validarSerie(serieId);
        validarTemporada(temporada);

        return cacheTemporadas.buscar(
                new ChaveTemporada(serieId, temporada),
                () -> carregarTemporada(serieId, temporada)
        );
    }

    private TemporadaDTO carregarTemporada(Long serieId, int temporada) {
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

        var temporadaEmCache = cacheTemporadas.valorValido(new ChaveTemporada(serieId, temporada));
        if (temporadaEmCache != null) {
            var encontrado = temporadaEmCache.episodes().stream().filter(e -> java.util.Objects.equals(e.numero(), episodio)).findFirst();
            if (encontrado.isPresent()) return encontrado.get();
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

        List<TemporadaDTO> temporadas = carregarTemporadas(serieId, incluirEspeciais);
        List<EpisodioDTO> topRated = projetarRanking(temporadas, limite, minimoVotos);
        return new PainelEpisodiosDTO(
                serieId,
                "TMDB",
                List.copyOf(temporadas),
                topRated
        );
    }

    public List<TemporadaDTO> carregarTemporadas(Long serieId, boolean incluirEspeciais) {
        validarSerie(serieId);
        if (!operacoes.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Painel ocupado.");
        long prazo = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(prazoMs);
        try {
        TemporadasSerieDTO serie = aguardar(executor.submit(() -> cacheSeries.buscar(serieId, () -> {
            TemporadasSerieDTO resposta = consultar("/tv/" + serieId, TemporadasSerieDTO.class);
            if (resposta.seasons() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "O TMDB não retornou a lista de temporadas."
                );
            }
            return resposta;
        })), prazo);

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

        if (numeros.size() > maxTemporadas) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Orçamento de temporadas excedido.");
        List<java.util.concurrent.Future<TemporadaDTO>> pendentes = new ArrayList<>();
        try {
            for (int inicio = 0; inicio < numeros.size(); inicio += 4) {
                if (System.nanoTime() >= prazo) throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Prazo dos episódios excedido.");
                pendentes.clear();
                for (int numero : numeros.subList(inicio, Math.min(inicio + 4, numeros.size())))
                    pendentes.add(executor.submit(() -> buscarTemporada(serieId, numero)));
                for (var tarefa : pendentes) temporadas.add(aguardar(tarefa, prazo));
            }
            return List.copyOf(temporadas);
        } finally { pendentes.forEach(t -> t.cancel(true)); }
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Painel ocupado.");
        } finally { operacoes.release(); }
    }
    private <T> T aguardar(java.util.concurrent.Future<T> tarefa, long prazo) {
        try { return tarefa.get(Math.max(1, prazo - System.nanoTime()), java.util.concurrent.TimeUnit.NANOSECONDS); }
        catch (java.util.concurrent.TimeoutException ex) { throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Prazo dos episódios excedido."); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Consulta cancelada."); }
        catch (java.util.concurrent.ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException causa) throw causa;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao carregar episódios.");
        } finally { if (!tarefa.isDone()) tarefa.cancel(true); }
    }
    public List<EpisodioDTO> buscarRanking(Long serieId, int limite, int minimoVotos, boolean especiais) {
        if (limite < 1 || limite > 50 || minimoVotos < 1) throw new IllegalArgumentException("Limite ou mínimo de votos inválido.");
        return projetarRanking(carregarTemporadas(serieId, especiais), limite, minimoVotos);
    }
    private List<EpisodioDTO> projetarRanking(List<TemporadaDTO> temporadas, int limite, int minimoVotos) {
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

        return topRated;
    }

    private <T> T consultar(String caminho, Class<T> tipo) {
        return tmdb.buscar(tmdb.consulta(caminho), tipo);
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
