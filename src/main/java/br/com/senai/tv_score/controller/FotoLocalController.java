package br.com.senai.tv_score.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
/** Leitura pública somente no modo local de desenvolvimento. Produção usa URL do storage/CDN. */
@RestController @ConditionalOnProperty(name="foto.storage",havingValue="local")
public class FotoLocalController {
    private final Path raiz;
    public FotoLocalController(@Value("${foto.local.diretorio:./fotos-dev}") String diretorio){raiz=Path.of(diretorio).toAbsolutePath().normalize();}
    @GetMapping("/api/fotos/{chave}")
    public ResponseEntity<FileSystemResource> ler(@PathVariable String chave) {
        if(!chave.matches("[a-f0-9-]{36}\\.png"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Foto não encontrada.");
        Path arquivo=raiz.resolve(chave).normalize();
        if(!arquivo.startsWith(raiz) || !Files.isRegularFile(arquivo))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Foto não encontrada.");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                .header("X-Content-Type-Options","nosniff").body(new FileSystemResource(arquivo));
    }
}
