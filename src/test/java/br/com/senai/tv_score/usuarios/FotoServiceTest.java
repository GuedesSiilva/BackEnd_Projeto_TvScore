package br.com.senai.tv_score.usuarios;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
class FotoServiceTest {
    @Autowired FotoService fotos; @Autowired UsuarioRepository usuarios; @Autowired FotoLimpezaRepository limpeza;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean FotoStorage storage;
    @Autowired br.com.senai.tv_score.avaliacao.AvaliacaoService avaliacoes;
    MockMultipartFile imagem() throws Exception {var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",b);return new MockMultipartFile("arquivo","x.png","image/png",b.toByteArray());}
    Usuario criar(){return usuarios.saveAndFlush(new Usuario("Foto",java.util.UUID.randomUUID()+"@example.test","hash"));}
    @Test void trocaRemocaoEObjetoAntigoApenasAposConfirmacao() throws Exception {
        var u=criar();String a=fotos.atualizar(u,imagem());String chaveA=usuarios.findById(u.getId()).orElseThrow().getFotoChave();
        assertTrue(a.endsWith(chaveA));verify(storage,never()).apagar(anyString());
        fotos.atualizar(u,imagem());String chaveB=usuarios.findById(u.getId()).orElseThrow().getFotoChave();assertNotEquals(chaveA,chaveB);
        assertTrue(limpeza.existsById(chaveA));
        fotos.remover(u);fotos.remover(u);assertNull(usuarios.findById(u.getId()).orElseThrow().getFotoChave());assertTrue(limpeza.existsById(chaveB));
    }
    @Test void falhaStoragePreservaReferenciaELimpezaPersiste() throws Exception {
        var u=criar();u.setFotoChave("anterior.png");usuarios.saveAndFlush(u);
        doThrow(new IllegalStateException("falha simulada")).when(storage).gravar(anyString(),any());
        assertThrows(IllegalStateException.class,()->fotos.atualizar(u,imagem()));
        assertEquals("anterior.png",usuarios.findById(u.getId()).orElseThrow().getFotoChave());
    }
    @Test void duasTrocasConcorrentesMantemFotoAtivaForaDaLimpeza() throws Exception {
        var u=criar();var img=imagem();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->fotos.atualizar(u,img));var b=pool.submit(()->fotos.atualizar(u,img));
            a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
        String ativa=usuarios.findById(u.getId()).orElseThrow().getFotoChave();assertNotNull(ativa);assertFalse(limpeza.existsById(ativa));
    }
    @Test void limpezaNuncaApagaFotoAindaReferenciada() {
        var u=criar();u.setFotoChave("ativa.png");usuarios.saveAndFlush(u);
        var tarefa=new FotoLimpeza("ativa.png");tarefa.setDepoisDe(Instant.now().minusSeconds(10));limpeza.saveAndFlush(tarefa);
        fotos.limpar();verify(storage,never()).apagar("ativa.png");assertTrue(usuarios.existsByFotoChave("ativa.png"));
    }
    @Test void avatarDeAvaliacaoVemDoAutorPersistido() {
        var u=criar();u.setFotoChave("autor.png");usuarios.saveAndFlush(u);
        var pedido=new br.com.senai.tv_score.dto.AvaliacaoDTO(null,55L,9d,"Texto","Nome enviado pelo cliente",null,
                br.com.senai.tv_score.filme.TipoConteudo.TV,"https://cliente.invalid/foto",null);
        var res=avaliacoes.avaliar(u,pedido);
        assertEquals("Foto",res.nomeUsuario());assertEquals(fotos.url("autor.png"),res.fotoUsuarioUrl());assertNull(res.conteudo());
    }
}
