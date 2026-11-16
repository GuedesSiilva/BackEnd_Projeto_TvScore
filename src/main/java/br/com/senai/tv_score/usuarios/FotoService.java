package br.com.senai.tv_score.usuarios;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import java.util.UUID;

@Service
public class FotoService {
    private final UsuarioRepository usuarios;
    private final FotoLimpezaRepository limpeza;
    private final FotoStorage storage;
    private final TransactionTemplate tx;
    private final String base;
    public FotoService(UsuarioRepository usuarios, FotoLimpezaRepository limpeza, FotoStorage storage,
            PlatformTransactionManager manager, @Value("${foto.url-base:http://localhost:8080/api/fotos}") String base) {
        var uri = java.net.URI.create(base);
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) throw new IllegalArgumentException("Base de fotos inválida.");
        this.usuarios=usuarios; this.limpeza=limpeza; this.storage=storage; this.tx=new TransactionTemplate(manager);
        this.base=base.replaceAll("/+$", "");
    }
    public String url(String chave) { return chave == null ? null : base + "/" + chave; }
    public String atualizar(Usuario autenticado, MultipartFile arquivo) {
        byte[] png = sanitizar(arquivo);
        String chave = UUID.randomUUID() + ".png";
        tx.executeWithoutResult(s -> limpeza.saveAndFlush(new FotoLimpeza(chave)));
        storage.gravar(chave,png); // sem conexão SQL durante I/O externo
        tx.executeWithoutResult(s -> {
            Usuario u = usuarios.buscarPorIdComBloqueio(autenticado.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            limpeza.buscarComBloqueio(chave).orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Upload expirado; tente novamente."));
            agendarExclusao(u.getFotoChave());
            u.setFotoChave(chave);
            limpeza.deleteById(chave);
        });
        return url(chave);
    }
    public void remover(Usuario autenticado) {
        tx.executeWithoutResult(s -> {
            Usuario u = usuarios.buscarPorIdComBloqueio(autenticado.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            agendarExclusao(u.getFotoChave()); u.setFotoChave(null);
        });
    }
    // Chamado dentro da transação de troca/exclusão da conta; tarefa sobrevive a falhas externas.
    public void agendarExclusao(String chave) { if (chave != null) limpeza.save(new FotoLimpeza(chave)); }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString="${foto.limpeza-intervalo-ms:60000}")
    public void limpar() {
        for (var item : limpeza.findTop50ByDepoisDeBeforeOrderByDepoisDeAsc(java.time.Instant.now())) {
            try {
                tx.executeWithoutResult(s -> {
                    var atual = limpeza.buscarComBloqueio(item.getChave());
                    if (atual.isPresent() && !atual.get().getDepoisDe().isAfter(java.time.Instant.now())) {
                        if (!usuarios.existsByFotoChave(item.getChave())) storage.apagar(item.getChave());
                        limpeza.deleteById(item.getChave());
                    }
                });
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Falha na limpeza de foto; nova tentativa no próximo ciclo.");
            }
        }
    }
    public static byte[] sanitizar(MultipartFile arquivo) {
        if (arquivo.getSize() > 5L*1024*1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Foto excede 5 MiB.");
        if (!"image/png".equals(arquivo.getContentType()) && !"image/jpeg".equals(arquivo.getContentType()))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Envie JPEG ou PNG.");
        try (var entrada=ImageIO.createImageInputStream(arquivo.getInputStream())) {
            if (entrada == null) throw new IOException();
            var readers=ImageIO.getImageReaders(entrada);
            if (!readers.hasNext()) throw new IOException();
            var reader=readers.next();
            try {
                String formato=reader.getFormatName();
                if (!formato.equalsIgnoreCase("png") && !formato.equalsIgnoreCase("jpeg")) throw new IOException();
                reader.setInput(entrada,true,true);
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if (w<1 || h<1 || w>10000 || h>10000 || (long)w*h>20000000) throw new IOException();
                var original=reader.read(0);
                double escala=Math.min(1,512d/Math.max(w,h));
                var imagem=new BufferedImage(Math.max(1,(int)(w*escala)),Math.max(1,(int)(h*escala)),BufferedImage.TYPE_INT_ARGB);
                var g=imagem.createGraphics();
                try { g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(original,0,0,imagem.getWidth(),imagem.getHeight(),null); }
                finally { g.dispose(); }
                var saida=new ByteArrayOutputStream(); ImageIO.write(imagem,"png",saida); return saida.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException | RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Imagem inválida ou dimensões excedidas."); }
    }
}
