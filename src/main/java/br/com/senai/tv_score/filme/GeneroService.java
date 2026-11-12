package br.com.senai.tv_score.filme;

import br.com.senai.tv_score.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

@Service
public class GeneroService {
    private static final Logger LOG = LoggerFactory.getLogger(GeneroService.class);
    private record CatalogoGeneros(GeneroResponseDTO resposta, Map<Integer, String> nomes) {}
    private final TmdbClient tmdb;
    private final CacheTmdb<TipoConteudo, CatalogoGeneros> cacheGeneros = new CacheTmdb<>(Duration.ofHours(6), 2);
    public GeneroService(TmdbClient tmdb) { this.tmdb = tmdb; }
    public GeneroResponseDTO listarGeneros() {
        return catalogoGeneros(TipoConteudo.MOVIE).resposta();
    }

    public Map<Integer, String> buscarNomesGeneros(TipoConteudo tipo) {
        try {
            return catalogoGeneros(tipo).nomes();
        } catch (RestClientException | ResponseStatusException ex) {
            // A pesquisa já recebeu os títulos; preserva a resposta se o enriquecimento falhar.
            LOG.warn("Não foi possível atualizar os gêneros para {}.", tipo);
            CatalogoGeneros anterior = cacheGeneros.ultimoValor(tipo);
            return anterior == null ? Map.of() : anterior.nomes();
        }
    }

    private CatalogoGeneros catalogoGeneros(TipoConteudo tipo) {
        try { return cacheGeneros.buscar(tipo, () -> {
            String caminho = tipo == TipoConteudo.MOVIE ? "/genre/movie/list" : "/genre/tv/list";
            GeneroResponseDTO resposta = tmdb.buscar(tmdb.consulta(caminho), GeneroResponseDTO.class);
            if (resposta.genres() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "O TMDB não retornou a lista de gêneros."
                );
            }
            Map<Integer, String> nomes = new HashMap<>();
            for (GeneroDTO genero : resposta.genres()) {
                if (genero != null && genero.id() != null
                        && genero.name() != null && !genero.name().isBlank()) {
                    nomes.put(Math.toIntExact(genero.id()), genero.name());
                }
            }
            return new CatalogoGeneros(resposta, Map.copyOf(nomes));
        }); } catch (RestClientException | ResponseStatusException ex) {
            CatalogoGeneros anterior = cacheGeneros.ultimoValor(tipo);
            if (anterior != null) return anterior;
            throw ex;
        }
    }
    public record GeneroDisponivel(
            String codigo,
            String nome,
            Long generoFilmeId,
            Long generoSerieId
    ) {}

    public List<GeneroDisponivel> listarGenerosDisponiveis() {
        Map<Long, GeneroDisponivel> unificados = new LinkedHashMap<>();

        adicionarGeneros(
                unificados,
                catalogoGeneros(TipoConteudo.MOVIE).resposta(),
                TipoConteudo.MOVIE
        );

        adicionarGeneros(
                unificados,
                catalogoGeneros(TipoConteudo.TV).resposta(),
                TipoConteudo.TV
        );

        return unificados.values().stream()
                .sorted(
                        Comparator.comparing(
                                GeneroDisponivel::nome,
                                String.CASE_INSENSITIVE_ORDER
                        ).thenComparing(GeneroDisponivel::codigo)
                )
                .toList();
    }

    private void adicionarGeneros(
            Map<Long, GeneroDisponivel> unificados,
            GeneroResponseDTO resposta,
            TipoConteudo tipo
    ) {
        if (resposta == null || resposta.genres() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "O TMDB não retornou a lista de gêneros."
            );
        }

        for (GeneroDTO genero : resposta.genres()) {
            if (genero == null
                    || genero.id() == null
                    || genero.id() <= 0
                    || genero.name() == null
                    || genero.name().isBlank()) {
                continue;
            }

            GeneroDisponivel anterior = unificados.get(genero.id());

            Long filmeId = anterior == null
                    ? null
                    : anterior.generoFilmeId();

            Long serieId = anterior == null
                    ? null
                    : anterior.generoSerieId();

            if (tipo == TipoConteudo.MOVIE) {
                filmeId = genero.id();
            } else {
                serieId = genero.id();
            }

            unificados.put(
                    genero.id(),
                    new GeneroDisponivel(
                            codigoGenero(genero.id()),
                            anterior == null
                                    ? genero.name().strip()
                                    : anterior.nome(),
                            filmeId,
                            serieId
                    )
            );
        }
    }

    private String codigoGenero(Long id) {
        // Compatibilidade com as preferências já salvas.
        // A lista de gêneros continua vindo integralmente do TMDB.
        return Arrays.stream(GeneroConteudo.values())
                .filter(genero ->
                        Objects.equals(genero.getGeneroFilmeId(), id)
                                || Objects.equals(genero.getGeneroSerieId(), id)
                )
                .map(GeneroConteudo::getCodigo)
                .findFirst()
                .orElse("tmdb-" + id);
    }

    public GeneroDisponivel resolverGenero(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Informe um gênero.");
        }

        return listarGenerosDisponiveis().stream()
                .filter(genero ->
                        genero.codigo().equalsIgnoreCase(codigo.strip())
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gênero inválido. Consulte /api/conteudos/generos."
                ));
    }
}
