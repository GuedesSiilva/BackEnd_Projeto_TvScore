package br.com.senai.tv_score.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmailRecuperacaoTest {
    private static final String RESEND = "https://api.resend.com";
    private static final String RESET = "https://conta.example.test/redefinir%20senha";
    private static final String DE = "TV Score <reset@example.test>";
    private static final String PARA = "destino@example.test";
    private static final String CHAVE_FICTICIA = "chave-de-teste";

    @Test void enviaJsonAoResendComLinkDeUsoUnico() throws Exception {
        var builder = RestClient.builder().baseUrl(RESEND);
        var servidor = MockRestServiceServer.bindTo(builder).build();
        String logo = URI.create(RESET).resolve("/redefinir-senha/logo-tvscore.png").toString();
        String introducao = "Recebemos uma solicitação para redefinir a senha da sua conta no TV Score.";
        String link = RESET + "?token=token-falso";
        String validade = "O link é de uso único e expira em até 20 minutos.";
        servidor.expect(requestTo(RESEND + "/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + CHAVE_FICTICIA))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from").value(DE))
                .andExpect(jsonPath("$.to[0]").value(PARA))
                .andExpect(jsonPath("$.subject").value("Solicitação de redefinição de senha | TV Score"))
                .andExpect(jsonPath("$.text", stringContainsInOrder(introducao,
                        "Para continuar, acesse o link abaixo:", link, validade)))
                .andExpect(jsonPath("$.html", stringContainsInOrder("<img src=\"" + logo + "\"",
                        ">Redefinição de senha</h1>", introducao, "href=\"" + link + "\"",
                        ">Definir nova senha</a>", validade)))
                .andRespond(withSuccess());
        var email = new EmailRecuperacao(RESET, DE, true, CHAVE_FICTICIA, builder.build());
        try {
            email.enviar(PARA, "token-falso", Instant.now().plusSeconds(1200));
            servidor.verify(Duration.ofSeconds(5));
        } finally {
            email.fechar();
        }
    }

    @Test void rejeicaoETimeoutNaoBloqueiamSolicitanteNemParalisamFila() throws Exception {
        var builder = RestClient.builder().baseUrl(RESEND);
        var servidor = MockRestServiceServer.bindTo(builder).build();
        var iniciou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        servidor.expect(requestTo(RESEND + "/emails")).andRespond(request -> {
            iniciou.countDown();
            try {
                if (!liberar.await(5, TimeUnit.SECONDS)) throw new AssertionError("Worker não liberado");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
            return withStatus(HttpStatus.TOO_MANY_REQUESTS).createResponse(request);
        });
        servidor.expect(requestTo(RESEND + "/emails"))
                .andRespond(withException(new SocketTimeoutException("falha simulada")));
        servidor.expect(requestTo(RESEND + "/emails")).andRespond(withSuccess());
        var email = new EmailRecuperacao(RESET, DE, true, CHAVE_FICTICIA, builder.build());
        try {
            var expira = Instant.now().plusSeconds(1200);
            email.enviar(PARA, "primeiro", expira);
            assertTrue(iniciou.await(5, TimeUnit.SECONDS));
            email.enviar(PARA, "segundo", expira);
            email.enviar(PARA, "terceiro", expira);
            liberar.countDown();
            servidor.verify(Duration.ofSeconds(5));
        } finally {
            liberar.countDown();
            email.fechar();
        }
    }

    @Test void ignoraEnvioDesabilitadoETokenExpirado() throws Exception {
        var builder = RestClient.builder().baseUrl(RESEND);
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(RESEND + "/emails"))
                .andExpect(jsonPath("$.text", containsString("?token=valido")))
                .andExpect(jsonPath("$.text", containsString("expira em até 1 minuto.")))
                .andExpect(jsonPath("$.html", containsString("expira em até 1 minuto.")))
                .andRespond(withSuccess());
        var http = builder.build();
        var ativo = new EmailRecuperacao(RESET, DE, true, CHAVE_FICTICIA, http);
        var inativo = new EmailRecuperacao("http://localhost:3000/reset", DE, false, "", http);
        try {
            inativo.enviar(PARA, "desabilitado", Instant.now().plusSeconds(30));
            ativo.enviar(PARA, "expirado", Instant.now().minusSeconds(10));
            ativo.enviar(PARA, "valido", Instant.now().plusSeconds(30));
            servidor.verify(Duration.ofSeconds(5));
        } finally {
            ativo.fechar();
            inativo.fechar();
        }
    }

    @Test void exigeHttpsChaveERemetenteQuandoHabilitado() {
        var http = RestClient.create();
        assertThrows(IllegalArgumentException.class,
                () -> new EmailRecuperacao("http://localhost:3000/reset", DE, true, CHAVE_FICTICIA, http));
        assertThrows(IllegalArgumentException.class, () -> new EmailRecuperacao(RESET, DE, true, "", http));
        assertThrows(IllegalArgumentException.class,
                () -> new EmailRecuperacao(RESET, "no-reply@localhost", true, CHAVE_FICTICIA, http));
    }
}
