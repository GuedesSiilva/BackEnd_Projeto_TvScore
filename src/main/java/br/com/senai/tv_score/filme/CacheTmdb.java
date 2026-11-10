package br.com.senai.tv_score.filme;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Supplier;

final class CacheTmdb<K, V> {

    private record Entrada<V>(V valor, long carregadoEm) {}

    private final long ttlNanos;
    private final int capacidade;
    private final LinkedHashMap<K, Entrada<V>> valores = new LinkedHashMap<>(16, 0.75f, true);
    private final Object[] travas = new Object[32];

    CacheTmdb(Duration ttl, int capacidade) {
        if (ttl.isZero() || ttl.isNegative() || capacidade < 1) {
            throw new IllegalArgumentException("Configuração inválida do cache.");
        }
        this.ttlNanos = ttl.toNanos();
        this.capacidade = capacidade;
        for (int i = 0; i < travas.length; i++) {
            travas[i] = new Object();
        }
    }

    V buscar(K chave, Supplier<V> carregar) {
        V atual = valorValido(chave);
        if (atual != null) {
            return atual;
        }

        // A mesma chave não inicia dois carregamentos concorrentes.
        synchronized (travas[Math.floorMod(chave.hashCode(), travas.length)]) {
            atual = valorValido(chave);
            if (atual != null) {
                return atual;
            }

            V carregado = Objects.requireNonNull(carregar.get());
            synchronized (valores) {
                valores.put(chave, new Entrada<>(carregado, System.nanoTime()));
                while (valores.size() > capacidade) {
                    valores.pollFirstEntry();
                }
            }
            return carregado;
        }
    }

    V ultimoValor(K chave) {
        synchronized (valores) {
            Entrada<V> entrada = valores.get(chave);
            return entrada == null ? null : entrada.valor();
        }
    }

    private V valorValido(K chave) {
        synchronized (valores) {
            Entrada<V> entrada = valores.get(chave);
            return entrada != null && System.nanoTime() - entrada.carregadoEm() < ttlNanos
                    ? entrada.valor()
                    : null;
        }
    }
}
