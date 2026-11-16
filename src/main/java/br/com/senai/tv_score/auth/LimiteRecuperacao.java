package br.com.senai.tv_score.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

/** Contadores locais, TTL fixo e capacidade limitada. Nunca usa X-Forwarded-For. */
@Component
public class LimiteRecuperacao {
    private record Contador(long inicio, int quantidade) {}
    private final Map<String, Contador> contadores = new HashMap<>();
    private final int limite;
    private final long janela;
    public LimiteRecuperacao(@Value("${senha.reset.limite:5}") int limite,
                             @Value("${senha.reset.janela-segundos:900}") long janela) {
        if (limite < 1 || janela < 1) throw new IllegalArgumentException("Limites de recuperação inválidos.");
        this.limite = limite; this.janela = janela * 1000;
    }
    public synchronized void verificar(String chave) {
        long agora = System.currentTimeMillis();
        contadores.entrySet().removeIf(e -> agora - e.getValue().inicio >= janela);
        Contador c = contadores.get(chave);
        if ((c == null && contadores.size() >= 10000) || (c != null && c.quantidade >= limite)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Tente novamente mais tarde.") {
                @Override public org.springframework.http.HttpHeaders getHeaders() {
                    var headers = new org.springframework.http.HttpHeaders();
                    headers.set("Retry-After", Long.toString(Math.max(1, janela / 1000)));
                    return headers;
                }
            };
        }
        contadores.put(chave, c == null ? new Contador(agora, 1) : new Contador(c.inicio, c.quantidade + 1));
    }
}
