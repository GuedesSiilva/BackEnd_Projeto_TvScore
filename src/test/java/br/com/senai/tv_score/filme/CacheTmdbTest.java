package br.com.senai.tv_score.filme;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTmdbTest {

    @Test
    void compartilhaCarregamentoDaMesmaTemporada() throws Exception {
        var cache = new CacheTmdb<String, Integer>(Duration.ofMinutes(1), 2);
        var carregamentos = new AtomicInteger();
        var iniciou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var primeiro = executor.submit(() -> cache.buscar("temporada-1", () -> {
                    iniciou.countDown();
                    try {
                        if (!liberar.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Carregamento não liberado.");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    }
                    return carregamentos.incrementAndGet();
                }));
                assertTrue(iniciou.await(5, TimeUnit.SECONDS));
                var segundo = executor.submit(() -> cache.buscar(
                        "temporada-1", carregamentos::incrementAndGet
                ));
                liberar.countDown();
                assertEquals(1, primeiro.get(5, TimeUnit.SECONDS).intValue());
                assertEquals(1, segundo.get(5, TimeUnit.SECONDS).intValue());
                assertEquals(1, carregamentos.get());
            } finally {
                liberar.countDown();
            }
        }
    }

    @Test
    void removeEntradaMenosUsadaAoAtingirCapacidade() {
        var cache = new CacheTmdb<String, Integer>(Duration.ofMinutes(1), 2);
        cache.buscar("a", () -> 1);
        cache.buscar("b", () -> 2);
        assertEquals(1, cache.buscar("a", () -> 99).intValue());
        cache.buscar("c", () -> 3);
        assertNull(cache.ultimoValor("b"));
        assertEquals(1, cache.ultimoValor("a").intValue());
    }
    @Test void falhaNaoEnvenenaCache() {
        var cache = new CacheTmdb<String,Integer>(Duration.ofMinutes(1),2);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> cache.buscar("a", () -> { throw new IllegalStateException(); }));
        assertEquals(2, cache.buscar("a", () -> 2));
    }
    @Test void expiraERetemUltimoValor() throws Exception {
        var cache = new CacheTmdb<String,Integer>(Duration.ofMillis(10),2);
        cache.buscar("a", () -> 1);
        Thread.sleep(30);
        assertEquals(1,cache.ultimoValor("a"));
        assertEquals(2,cache.buscar("a", () -> 2));
    }
}
