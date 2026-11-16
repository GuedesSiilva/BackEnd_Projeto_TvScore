package br.com.senai.tv_score.config;

import br.com.senai.tv_score.usuarios.FotoStorage;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import java.net.URI;
import java.nio.file.*;

@Configuration
public class FotoStorageConfig {
    @Bean(destroyMethod = "close")
    public S3Client fotoS3(@Value("${foto.s3.endpoint:}") String endpoint,
            @Value("${foto.s3.regiao:us-east-1}") String regiao) {
        var builder = S3Client.builder().region(Region.of(regiao))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .overrideConfiguration(c -> c.apiCallTimeout(java.time.Duration.ofSeconds(20))
                        .apiCallAttemptTimeout(java.time.Duration.ofSeconds(10)));
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }
    @Bean public FotoStorage fotoStorage(S3Client s3, @Value("${foto.storage:desabilitado}") String modo,
            @Value("${foto.s3.bucket:}") String bucket, @Value("${foto.local.diretorio:./fotos-dev}") String diretorio,
            org.springframework.core.env.Environment ambiente) {
        if (ambiente.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")) && modo.equals("local"))
            throw new IllegalArgumentException("Produção exige storage durável S3.");
        return new FotoStorage() {
            private Path caminho(String chave) {
                if (!chave.matches("[a-f0-9-]{36}\\.png")) throw new IllegalArgumentException("Chave inválida.");
                return Path.of(diretorio).toAbsolutePath().normalize().resolve(chave);
            }
            public void gravar(String chave, byte[] png) {
                try {
                    if (modo.equals("s3") && !bucket.isBlank()) s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(chave).contentType("image/png").build(), RequestBody.fromBytes(png));
                    else if (modo.equals("local")) { Path p = caminho(chave); Files.createDirectories(p.getParent()); Files.write(p, png, StandardOpenOption.CREATE_NEW); }
                    else throw new IllegalStateException();
                } catch (Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento de fotos indisponível."); }
            }
            public void apagar(String chave) {
                try {
                    if (modo.equals("s3") && !bucket.isBlank()) s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build());
                    else if (modo.equals("local")) Files.deleteIfExists(caminho(chave));
                    else throw new IllegalStateException();
                } catch (Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento de fotos indisponível."); }
            }
        };
    }
}
