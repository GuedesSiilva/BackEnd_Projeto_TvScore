package br.com.senai.tv_score.usuarios;

import br.com.senai.tv_score.auth.UsuarioLogadoResponse;
import br.com.senai.tv_score.dto.AtualizarUsuarioDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioLogadoResponse atualizar(
            Usuario autenticado,
            AtualizarUsuarioDTO dto
    ) {
        Usuario usuario = usuarioRepository.findById(autenticado.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));

        usuario.setNome(dto.nome().strip());

        return new UsuarioLogadoResponse(
                usuario.getNome(),
                usuario.getEmail()
        );
    }
}