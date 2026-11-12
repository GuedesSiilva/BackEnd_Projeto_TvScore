package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.*;
import br.com.senai.tv_score.filme.TmdbClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FilmesPreferidosService {
    private final FilmePreferidoRepository filmes;
    private final UsuarioRepository usuarios;
    private final TmdbClient tmdb;

    public FilmesPreferidosService(FilmePreferidoRepository filmes,
                                   UsuarioRepository usuarios, TmdbClient tmdb) {
        this.filmes = filmes;
        this.usuarios = usuarios;
        this.tmdb = tmdb;
    }

    @Transactional(readOnly = true)
    public FilmesPreferidosResponseDTO consultar(Usuario autenticado) {
        Long usuarioId = exigirUsuario(autenticado);
        return resposta(filmes.findByUsuarioIdOrderByFilmeIdAsc(usuarioId));
    }

    @Transactional
    public FilmesPreferidosResponseDTO salvar(Usuario autenticado, FilmesPreferidosRequestDTO dto) {
        Long usuarioId = exigirUsuario(autenticado);
        // Todos os títulos são consultados antes de alterar qualquer registro.
        Map<Long, FilmePreferidoTmdbDTO> dados = new LinkedHashMap<>();
        dto.filmes().stream().distinct().forEach(id -> {
            FilmePreferidoTmdbDTO filme = tmdb.buscar(
                    tmdb.consulta("/movie/" + id), FilmePreferidoTmdbDTO.class
            );
            validarDados(id, filme);
            dados.put(id, filme);
        });

        // Serializa alterações da seleção do mesmo usuário e a exclusão da conta.
        Usuario usuario = usuarios.buscarPorIdComBloqueio(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado."
                ));
        List<FilmePreferido> anteriores = filmes.findByUsuarioIdOrderByFilmeIdAsc(usuarioId);
        Map<Long, FilmePreferido> porId = new HashMap<>();
        anteriores.forEach(filme -> porId.put(filme.getFilmeId(), filme));
        filmes.deleteAll(anteriores.stream()
                .filter(filme -> !dados.containsKey(filme.getFilmeId())).toList());

        List<FilmePreferido> selecionados = new ArrayList<>();
        dados.forEach((id, detalhes) -> {
            FilmePreferido filme = porId.get(id);
            if (filme == null) {
                filme = new FilmePreferido(usuario, id);
            }
            filme.atualizarDados(detalhes);
            selecionados.add(filme);
        });
        return resposta(filmes.saveAllAndFlush(selecionados));
    }

    private void validarDados(Long id, FilmePreferidoTmdbDTO dados) {
        if (!id.equals(dados.id()) || dados.title() == null || dados.title().isBlank()
                || dados.title().length() > 1000 || dados.genres() == null
                || dados.genres().stream().anyMatch(genero -> genero == null
                    || genero.id() == null || genero.id() <= 0
                    || genero.name() == null || genero.name().isBlank()
                    || genero.name().length() > 255)
                || dados.originalLanguage() != null && dados.originalLanguage().length() > 20
                || dados.releaseDate() != null && !dados.releaseDate().isEmpty()
                    && !dataValida(dados.releaseDate())
                || dados.posterPath() != null && dados.posterPath().length() > 1000
                || dados.voteAverage() != null && (!Double.isFinite(dados.voteAverage())
                    || dados.voteAverage() < 0 || dados.voteAverage() > 10)
                || dados.popularity() != null && (!Double.isFinite(dados.popularity())
                    || dados.popularity() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O TMDB retornou dados inválidos para o filme selecionado.");
        }
    }

    private boolean dataValida(String data) {
        try {
            if (data.length() != 10) {
                return false;
            }
            java.time.LocalDate.parse(data);
            return true;
        } catch (java.time.format.DateTimeParseException ex) {
            return false;
        }
    }

    private Long exigirUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        return usuario.getId();
    }

    private FilmesPreferidosResponseDTO resposta(List<FilmePreferido> selecionados) {
        return new FilmesPreferidosResponseDTO(selecionados.stream()
                .sorted(Comparator.comparing(FilmePreferido::getFilmeId))
                .map(filme -> new FilmePreferidoDTO(
                        filme.getFilmeId(), filme.getTitulo(), filme.getSinopse(),
                        filme.getGeneros().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(genero -> new GeneroDTO(genero.getKey(), genero.getValue())).toList(),
                        filme.getIdiomaOriginal(), filme.getDataLancamento(), filme.getNotaTmdb(),
                        filme.getPopularidade(), filme.getPosterPath(), filme.getSelecionadoEm()
                )).toList());
    }
}
