package br.com.senai.tv_score.favorito;

import br.com.senai.tv_score.dto.FavoritoDTO;
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

    public List<FavoritoDTO> listarFavoritos(Usuario usuario) {
        return favoritoRepository.findByUsuario(usuario)
                .stream()
                .map(f -> new FavoritoDTO(f.getId(), f.getFilmeSerieId(), f.getDataAdicionado()))
                .toList();
    }

    public FavoritoDTO adicionarFavorito(Usuario usuario, FavoritoDTO dto) {
        if (favoritoRepository.existsByUsuarioAndFilmeSerieId(usuario, dto.filmeSerieId())) {
            throw new IllegalArgumentException("Filme/Série já está nos favoritos.");
        }

        Favorito novoFavorito = new Favorito(usuario, dto.filmeSerieId());
        Favorito salvo = favoritoRepository.save(novoFavorito);

        return new FavoritoDTO(salvo.getId(), salvo.getFilmeSerieId(), salvo.getDataAdicionado());
    }

    @Transactional
    public void removerFavorito(Usuario usuario, Long filmeSerieId) {
        if (!favoritoRepository.existsByUsuarioAndFilmeSerieId(usuario, filmeSerieId)) {
            throw new IllegalArgumentException("Favorito não encontrado.");
        }
        favoritoRepository.deleteByUsuarioAndFilmeSerieId(usuario, filmeSerieId);
    }
}