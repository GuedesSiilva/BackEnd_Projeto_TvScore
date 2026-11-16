package br.com.senai.tv_score.auth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class EmailRecuperacaoTest {
    @Test void provedorFalsoFalhaAssincronaNaoVazaParaPedido() throws Exception {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        var sender=mock(JavaMailSender.class);when(provider.getIfAvailable()).thenReturn(sender);
        var chamado=new CountDownLatch(1);
        doAnswer(i->{chamado.countDown();throw new IllegalStateException("falha simulada");}).when(sender).send(any(SimpleMailMessage.class));
        var email=new EmailRecuperacao(provider,"http://localhost:3000/reset","dev@localhost",true);
        try {assertDoesNotThrow(()->email.enviar("destino@example.test","token-falso"));assertTrue(chamado.await(2,TimeUnit.SECONDS));}
        finally{email.fechar();}
    }
}
