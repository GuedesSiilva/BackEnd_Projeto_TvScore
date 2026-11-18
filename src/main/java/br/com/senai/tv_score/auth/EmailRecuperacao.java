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
import org.springframework.web.util.HtmlUtils;
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
    private record Remetente(String name, String email) {}
    private record Destinatario(String email) {}

    private record Mensagem(Remetente sender, List<Destinatario> to, String subject,
                            String textContent, String htmlContent) {
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
            @Value("${senha.reset.brevo-api-key:}") String apiKey) {
        this(base, remetente, habilitado, apiKey, clienteBrevo());
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
                || !remetente.matches("[^\\s<>@,;]+@[^\\s<>@,;]+")
                || "no-reply@localhost".equalsIgnoreCase(remetente))) {
            throw new IllegalArgumentException("Configure BREVO_API_KEY e RESET_REMETENTE com um endereço de e-mail simples.");
        }
        this.base = uri;
        this.remetente = remetente;
        this.apiKey = apiKey;
        this.habilitado = habilitado;
        this.http = http;
    }

    private static RestClient clienteBrevo() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl("https://api.brevo.com").requestFactory(factory).build();
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
        long minutos = Math.max(1, Math.ceilDiv(restante.toMillis(), 60_000));
        String validade = minutos == 1 ? "1 minuto" : minutos + " minutos";
        String texto = """
                Recebemos uma solicitação para redefinir a senha da sua conta no TV Score.

                Para continuar, acesse o link abaixo:
                %s

                O link é de uso único e expira em até %s.
                Se você não solicitou a redefinição, ignore esta mensagem. Sua senha permanecerá a mesma.

                Equipe TV Score
                """.formatted(link, validade);
        String logo = base.resolve("/redefinir-senha/logo-tvscore.png").toASCIIString();
        String html = """
                <div lang="pt-BR" style="background:#080f1e;color:#f4f5fb;padding:24px;font-family:Arial,sans-serif">
                  <div style="max-width:520px;margin:0 auto;padding:24px;background:#0d172b;border:1px solid #34415a;border-radius:16px">
                    <img src="%s" alt="TV Score" width="61" height="57" style="display:block;width:61px;max-width:100%%;height:auto;color:#eec15e">
                    <h1 style="font-size:26px;margin:14px 0">Redefinição de senha</h1>
                    <p>Recebemos uma solicitação para redefinir a senha da sua conta no TV Score.</p>
                    <p style="margin:24px 0">
                      <a href="%s" style="display:inline-block;padding:14px 20px;border-radius:8px;background:#4d8cff;color:#080f1e;font-weight:bold;text-decoration:none">Definir nova senha</a>
                    </p>
                    <p>O link é de uso único e expira em até %s.</p>
                    <p style="color:#c2cbe0">Se você não solicitou a redefinição, ignore esta mensagem. Sua senha permanecerá a mesma.</p>
                    <p style="color:#c2cbe0;margin-bottom:0">Equipe TV Score</p>
                  </div>
                </div>
                """.formatted(HtmlUtils.htmlEscape(logo), HtmlUtils.htmlEscape(link), validade);
        try {
            var resposta = http.post().uri("/v3/smtp/email")
                    .headers(headers -> headers.set("api-key", apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Mensagem(new Remetente("TV Score", remetente), List.of(new Destinatario(destinatario)),
                            "Solicitação de redefinição de senha | TV Score", texto, html))
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
