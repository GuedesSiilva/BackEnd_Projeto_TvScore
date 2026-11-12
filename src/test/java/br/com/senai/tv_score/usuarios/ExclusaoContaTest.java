package br.com.senai.tv_score.usuarios;
import br.com.senai.tv_score.avaliacao.*;
import br.com.senai.tv_score.favorito.*;
import br.com.senai.tv_score.filme.TipoConteudo;
import br.com.senai.tv_score.dto.ExcluirContaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ExclusaoContaTest {
 @Autowired UsuarioRepository usuarios;
 @Autowired AvaliacaoRepository avaliacoes;
 @Autowired FavoritoRepository favoritos;
 @Autowired UsuarioService servico;
 @Autowired PasswordEncoder encoder;
 @Autowired PlatformTransactionManager manager;
 @Test void exclusaoComDependentesERollback() {
    var tx = new TransactionTemplate(manager);
    Usuario u = tx.execute(s -> {
        var novo = new Usuario("Teste", "rollback@example.com",encoder.encode("1234"));
        novo.getGenerosPreferidos().add("drama");
        usuarios.saveAndFlush(novo);
        avaliacoes.saveAndFlush(new Avaliacao(novo, 7L, 8.0, "Texto", TipoConteudo.MOVIE));
        favoritos.saveAndFlush(new Favorito(novo,7L,TipoConteudo.MOVIE));
        favoritos.saveAndFlush(new Favorito(novo,7L,TipoConteudo.TV));
        return novo;
    });
    assertThrows(IllegalStateException.class, () -> tx.execute(s -> {
        servico.excluir(u,new ExcluirContaDTO("1234"));
        throw new IllegalStateException("rollback forcado");
    }));
    assertTrue(usuarios.existsById(u.getId()));
    assertEquals(2,favoritos.findByUsuario(u).size());
    assertEquals(1,avaliacoes.findByUsuario(u).size());
    servico.excluir(u,new ExcluirContaDTO("1234"));
    assertFalse(usuarios.existsById(u.getId()));
    assertTrue(favoritos.findByUsuario(u).isEmpty());
    assertTrue(avaliacoes.findByUsuario(u).isEmpty());
 }
}
