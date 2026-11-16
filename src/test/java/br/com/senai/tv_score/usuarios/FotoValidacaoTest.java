package br.com.senai.tv_score.usuarios;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class FotoValidacaoTest {
    @Test void pngReencodadoComLimiteESemDistorcao() throws Exception {
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1000,500,BufferedImage.TYPE_INT_RGB),"png",bytes);
        byte[] seguro=FotoService.sanitizar(new MockMultipartFile("arquivo","../../foto.png","image/png",bytes.toByteArray()));
        var imagem=ImageIO.read(new ByteArrayInputStream(seguro));
        assertEquals(512,imagem.getWidth());assertEquals(256,imagem.getHeight());
    }
    @Test void rejeitaArquivoFalsoMimeETamanho() {
        assertEquals(400,assertThrows(ResponseStatusException.class,()->FotoService.sanitizar(new MockMultipartFile("arquivo","x.png","image/png",new byte[]{1,2,3}))).getStatusCode().value());
        assertEquals(415,assertThrows(ResponseStatusException.class,()->FotoService.sanitizar(new MockMultipartFile("arquivo","x.svg","image/svg+xml",new byte[]{1}))).getStatusCode().value());
        assertEquals(413,assertThrows(ResponseStatusException.class,()->FotoService.sanitizar(new MockMultipartFile("arquivo","x.png","image/png",new byte[5*1024*1024+1]))).getStatusCode().value());
    }
}
