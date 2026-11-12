package br.com.senai.tv_score.favorito;

import br.com.senai.tv_score.dto.FavoritoDTO;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;

    public FavoritoService(FavoritoRepository favoritoRepository) {
        this.favoritoRepository = favoritoRepository;
    }

    @Transactional(readOnly = true)
    public List<FavoritoDTO> listarFavoritos(Usuario usuario) {
        return favoritoRepository.findByUsuario(usuario)
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional
    public FavoritoDTO adicionarFavorito(Usuario usuario, FavoritoDTO dto) {
        boolean existente = favoritoRepository
                .existsByUsuarioAndFilmeSerieIdAndTipoConteudo(
                        usuario,
                        dto.filmeSerieId(),
                        dto.tipoConteudo()
                );

        if (existente) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Favorito duplicado.");
        }

        Favorito favorito = new Favorito(
                usuario,
                dto.filmeSerieId(),
                dto.tipoConteudo()
        );

        return paraDTO(favoritoRepository.save(favorito));
    }

    @Transactional
    public void removerFavorito(
            Usuario usuario,
            Long filmeSerieId,
            TipoConteudo tipoConteudo
    ) {
        boolean existente = favoritoRepository
                .existsByUsuarioAndFilmeSerieIdAndTipoConteudo(
                        usuario,
                        filmeSerieId,
                        tipoConteudo
                );

        if (!existente) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Favorito inexistente.");
        }

        favoritoRepository.deleteByUsuarioAndFilmeSerieIdAndTipoConteudo(
                usuario,
                filmeSerieId,
                tipoConteudo
        );
    }

    private FavoritoDTO paraDTO(Favorito favorito) {
        return new FavoritoDTO(
                favorito.getId(),
                favorito.getFilmeSerieId(),
                favorito.getDataAdicionado(),
                favorito.getTipoConteudo()
        );
    }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<FavoritoDTO> listarFavoritos(Usuario usuario, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) throw new IllegalArgumentException("P?gina inv?lida; tamanho entre 1 e 100.");
        return favoritoRepository.findByUsuario(usuario, org.springframework.data.domain.PageRequest.of(
            pagina, tamanho, org.springframework.data.domain.Sort.by("id").ascending())).map(this::paraDTO);
    }
}