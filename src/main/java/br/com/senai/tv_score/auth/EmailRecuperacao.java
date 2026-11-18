package br.com.senai.tv_score.auth;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Fila volátil: token bruto somente em memória, sem outbox em texto claro. */
@Component
public class EmailRecuperacao {
    private static final Logger LOG = LoggerFactory.getLogger(EmailRecuperacao.class);
    private record Mensagem(String from, List<String> to, String subject, String text) {
        // RestClient inclui o corpo em logs de depuração via toString().
        @Override public String toString() { return "Mensagem de recuperação"; }
    }

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());
    private final RestClient http;
    private final URI base;
    private final String remetente;
    private final String apiKey;
    private final boolean habilitado;

    @Autowired
    public EmailRecuperacao(
            @Value("${senha.reset.url:http://localhost:3000/redefinir-senha}") String base,
            @Value("${senha.reset.remetente:no-reply@localhost}") String remetente,
            @Value("${senha.reset.email-habilitado:false}") boolean habilitado,
            @Value("${senha.reset.resend-api-key:}") String apiKey) {
        this(base, remetente, habilitado, apiKey, clienteResend());
    }

    EmailRecuperacao(String base, String remetente, boolean habilitado, String apiKey, RestClient http) {
        URI uri;
        try {
            uri = URI.create(base);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("RESET_URL inválida.");
        }
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean localhost = "http".equalsIgnoreCase(uri.getScheme()) && "localhost".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !(https || !habilitado && localhost)) {
            throw new IllegalArgumentException("Configure RESET_URL com HTTPS para habilitar a recuperação.");
        }
        if (habilitado && (apiKey.isBlank() || apiKey.chars().anyMatch(c -> c < 33 || c > 126)
                || remetente.isBlank() || "no-reply@localhost".equalsIgnoreCase(remetente.strip()))) {
            throw new IllegalArgumentException("Configure RESEND_API_KEY e RESET_REMETENTE para habilitar a recuperação.");
        }
        this.base = uri;
        this.remetente = remetente;
        this.apiKey = apiKey;
        this.habilitado = habilitado;
        this.http = http;
    }

    private static RestClient clienteResend() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl("https://api.resend.com").requestFactory(factory).build();
    }

    public void enviar(String destinatario, String token, Instant expiraEm) {
        if (!habilitado) return;
        try {
            executor.execute(() -> entregar(destinatario, token, expiraEm));
        } catch (RejectedExecutionException ex) {
            LOG.warn("Fila de recuperação indisponível; envio descartado.");
        }
    }

    private void entregar(String destinatario, String token, Instant expiraEm) {
        Duration restante = Duration.between(Instant.now(), expiraEm);
        if (restante.isNegative() || restante.isZero()) {
            LOG.warn("Envio de recuperação expirou na fila.");
            return;
        }
        // O token gerado já é Base64 URL-safe. O builder preserva o caminho codificado de RESET_URL.
        String link = UriComponentsBuilder.fromUri(base).queryParam("token", token).build(true).toUriString();
        String texto = """
                Recebemos um pedido para redefinir sua senha no TV Score.

                Abra este link para escolher uma nova senha:
                %s

                O link é de uso único e expira em cerca de %d minuto(s) ou menos.
                Se você não fez esse pedido, ignore este e-mail.
                """.formatted(link, Math.max(1, Math.ceilDiv(restante.toSeconds(), 60)));
        try {
            var resposta = http.post().uri("/emails")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Mensagem(remetente, List.of(destinatario), "Redefina sua senha do TV Score", texto))
                    .retrieve()
                    .toBodilessEntity();
            if (resposta.getStatusCode().is2xxSuccessful()) return;
        } catch (RestClientException ex) {
            // O erro do provedor pode conter dados da requisição.
        }
        LOG.warn("Falha no provedor de e-mail de recuperação.");
    }

    @PreDestroy void fechar() {
        executor.shutdownNow();
    }
}
