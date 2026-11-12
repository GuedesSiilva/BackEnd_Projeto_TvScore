package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.dto.TmdbResponseDTO;
import br.com.senai.tv_score.filme.FilmeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpcoesFilmesPreferidosService {
    private final UsuarioRepository usuarios;
    private final FilmeService filmes;

    public OpcoesFilmesPreferidosService(UsuarioRepository usuarios, FilmeService filmes) {
        this.usuarios = usuarios;
        this.filmes = filmes;
    }

    @Transactional(readOnly = true)
    public TmdbResponseDTO consultar(Usuario autenticado, int pagina) {
        if (autenticado == null || autenticado.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        Usuario usuario = usuarios.findById(autenticado.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return filmes.buscarOpcoesFilmesPreferidos(usuario.getGenerosPreferidos(), pagina);
    }
}
