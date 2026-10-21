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
            throw new IllegalArgumentException(
                    "Filme/Série já está nos favoritos."
            );
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
            throw new IllegalArgumentException("Favorito não encontrado.");
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
}