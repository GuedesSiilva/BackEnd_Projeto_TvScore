package br.com.senai.tv_score.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.concurrent.*;

/** Fila volátil: token bruto somente em memória, sem outbox em texto claro. */
@Component
public class EmailRecuperacao {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EmailRecuperacao.class);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());
    private final ObjectProvider<JavaMailSender> sender;
    private final String base, remetente;
    private final boolean habilitado;
    @Value("${senha.reset.validade-segundos:1200}") private long validadeSegundos=1200;
    public EmailRecuperacao(ObjectProvider<JavaMailSender> sender,
            @Value("${senha.reset.url:http://localhost:3000/redefinir-senha}") String base,
            @Value("${senha.reset.remetente:no-reply@localhost}") String remetente,
            @Value("${senha.reset.email-habilitado:false}") boolean habilitado) {
        URI uri = URI.create(base);
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost()))))
            throw new IllegalArgumentException("URL de recuperação inválida; use HTTPS ou localhost em desenvolvimento.");
        this.sender = sender; this.base = base; this.remetente = remetente; this.habilitado = habilitado;
    }
    public void enviar(String email, String token) {
        long expira=System.nanoTime()+TimeUnit.SECONDS.toNanos(validadeSegundos);
        try {
            executor.execute(() -> {
                try {
                    if (System.nanoTime()>=expira) { LOG.warn("Envio de recuperação expirou na fila."); return; }
                    if (!habilitado) { LOG.warn("Envio de recuperação desabilitado."); return; }
                    JavaMailSender cliente = sender.getIfAvailable();
                    if (cliente == null) throw new IllegalStateException();
                    var mensagem = new SimpleMailMessage();
                    mensagem.setFrom(remetente); mensagem.setTo(email);
                    mensagem.setSubject("Recuperação de senha TV Score");
                    mensagem.setText("Abra a tela para definir uma nova senha: " + base + "?token=" + token);
                    cliente.send(mensagem);
                } catch (Exception ex) { LOG.warn("Falha no provedor de recuperação; tipo={}", ex.getClass().getSimpleName()); }
            });
        } catch (RejectedExecutionException ex) { LOG.warn("Fila de recuperação saturada; envio descartado."); }
    }
    @jakarta.annotation.PreDestroy void fechar() { executor.shutdownNow(); }
}
