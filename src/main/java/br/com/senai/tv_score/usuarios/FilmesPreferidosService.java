package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.*;
import br.com.senai.tv_score.filme.TmdbClient;
import br.com.senai.tv_score.filme.TipoConteudo;
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
        return resposta(gravar(autenticado, dto.filmes().stream()
                .map(id -> new ConteudoPreferidoRequestDTO(id, TipoConteudo.MOVIE)).toList(), true));
    }

    @Transactional(readOnly = true)
    public ConteudosPreferidosResponseDTO consultarConteudos(Usuario autenticado) {
        return respostaConteudos(filmes.findByUsuarioIdOrderByFilmeIdAsc(exigirUsuario(autenticado)));
    }

    @Transactional
    public ConteudosPreferidosResponseDTO salvarConteudos(Usuario autenticado, ConteudosPreferidosRequestDTO dto) {
        return respostaConteudos(gravar(autenticado, dto.conteudos(), false));
    }

    private List<FilmePreferido> gravar(Usuario autenticado, List<ConteudoPreferidoRequestDTO> itens, boolean legado) {
        Long usuarioId = exigirUsuario(autenticado);
        if (itens == null || itens.size() > 5 || itens.stream().anyMatch(i -> i == null
                || i.id() == null || i.id() <= 0 || i.tipoConteudo() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleção inválida: máximo de cinco conteúdos.");
        }
        Map<ConteudoPreferidoRequestDTO, FilmePreferidoTmdbDTO> dados = new LinkedHashMap<>();
        itens.stream().distinct().forEach(item -> {
            var detalhe = tmdb.buscar(tmdb.consulta((item.tipoConteudo() == TipoConteudo.MOVIE
                    ? "/movie/" : "/tv/") + item.id()), FilmePreferidoTmdbDTO.class);
            validarDados(item.id(), detalhe);
            dados.put(item, detalhe);
        });

        // Serializa alterações da seleção do mesmo usuário e a exclusão da conta.
        Usuario usuario = usuarios.buscarPorIdComBloqueio(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado."
                ));
        List<FilmePreferido> anteriores = filmes.findByUsuarioIdOrderByFilmeIdAsc(usuarioId);
        Map<ConteudoPreferidoRequestDTO, FilmePreferido> porId = new HashMap<>();
        anteriores.forEach(filme -> porId.put(chave(filme), filme));
        long preservados = legado ? anteriores.stream().filter(f -> f.getTipoConteudo() == TipoConteudo.TV).count() : 0;
        if (dados.size() + preservados > 5) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Selecione no máximo cinco conteúdos no total.");
        filmes.deleteAll(anteriores.stream()
                .filter(filme -> (!legado || filme.getTipoConteudo() == TipoConteudo.MOVIE) && !dados.containsKey(chave(filme))).toList());

        List<FilmePreferido> selecionados = new ArrayList<>();
        dados.forEach((id, detalhes) -> {
            FilmePreferido filme = porId.get(id);
            if (filme == null) {
                filme = new FilmePreferido(usuario, id.id(), id.tipoConteudo());
            }
            filme.atualizarDados(detalhes);
            selecionados.add(filme);
        });
        List<FilmePreferido> salvos = new ArrayList<>(filmes.saveAllAndFlush(selecionados));
        if (legado) anteriores.stream().filter(f -> f.getTipoConteudo() == TipoConteudo.TV).forEach(salvos::add);
        return salvos;
    }

    private void validarDados(Long id, FilmePreferidoTmdbDTO dados) {
        if (dados == null || !id.equals(dados.id()) || dados.title() == null || dados.title().isBlank()
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
                    "O TMDB retornou dados inválidos para o conteúdo selecionado.");
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
                .filter(f -> f.getTipoConteudo() == TipoConteudo.MOVIE)
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
    private ConteudoPreferidoRequestDTO chave(FilmePreferido f) {
        return new ConteudoPreferidoRequestDTO(f.getFilmeId(), f.getTipoConteudo());
    }

    private ConteudosPreferidosResponseDTO respostaConteudos(List<FilmePreferido> itens) {
        return new ConteudosPreferidosResponseDTO(itens.stream()
                .sorted(Comparator.comparing(FilmePreferido::getFilmeId).thenComparing(FilmePreferido::getTipoConteudo))
                .map(f -> new ConteudoPreferidoDTO(f.getFilmeId(), f.getTipoConteudo(), f.getTitulo(), f.getSinopse(),
                        f.getGeneros().entrySet().stream().sorted(Map.Entry.comparingByKey())
                                .map(g -> new GeneroDTO(g.getKey(), g.getValue())).toList(),
                        f.getIdiomaOriginal(), f.getDataLancamento(), f.getNotaTmdb(),
                        f.getPopularidade(), f.getPosterPath(), f.getSelecionadoEm())).toList());
    }

}
