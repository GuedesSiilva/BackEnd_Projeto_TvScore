package br.com.senai.tv_score.filme;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.concurrent.*;
import java.util.function.Supplier;
@Component
public class CatalogoExecutor {
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32),new ThreadPoolExecutor.AbortPolicy());
    public <T> T executar(long prazo, Supplier<T> tarefa) {
        Future<T> futuro = null;
        try {
            long restante = prazo-System.nanoTime();
            if (restante <= 0) throw new TimeoutException();
            futuro=pool.submit(tarefa::get);
            return futuro.get(restante,TimeUnit.NANOSECONDS);
        } catch (RejectedExecutionException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Catálogo ocupado."); }
        catch (TimeoutException ex) { throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,"Prazo do catálogo excedido."); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Consulta cancelada."); }
        catch (ExecutionException ex) { if (ex.getCause() instanceof RuntimeException r) throw r; throw new IllegalStateException("Falha no catálogo."); }
        finally { if (futuro != null && !futuro.isDone()) futuro.cancel(true); }
    }
    @jakarta.annotation.PreDestroy void fechar() { pool.shutdownNow(); }
}
