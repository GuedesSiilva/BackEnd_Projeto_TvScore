package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.usuarios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service
public class RecuperacaoService {
    private final UsuarioRepository usuarios;
    private final ResetSenhaRepository tokens;
    private final PasswordEncoder encoder;
    private final EmailRecuperacao email;
    private final LimiteRecuperacao limite;
    private final TransactionTemplate tx;
    private final long validade;
    private final SecureRandom random = new SecureRandom();
    public RecuperacaoService(UsuarioRepository usuarios, ResetSenhaRepository tokens, PasswordEncoder encoder,
            EmailRecuperacao email, LimiteRecuperacao limite, PlatformTransactionManager manager,
            @Value("${senha.reset.validade-segundos:1200}") long validade) {
        if (validade < 1) throw new IllegalArgumentException("Validade de recuperação inválida.");
        this.usuarios = usuarios; this.tokens = tokens; this.encoder = encoder; this.email = email;
        this.limite = limite; this.tx = new TransactionTemplate(manager); this.validade = validade;
    }
    public void solicitar(String endereco, String ip) {
        String normalizado = endereco.strip().toLowerCase(Locale.ROOT);
        limite.verificar("ip:" + hash(ip)); limite.verificar("email:" + hash(normalizado));
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String bruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String destinatario = tx.execute(status -> {
            // SMTP nunca bloqueia esta chamada; limpeza periódica evita locks globais no pedido.
            var encontrado = usuarios.findByEmailIgnoreCase(normalizado);
            if (encontrado.isEmpty()) return null;
            var usuario = usuarios.buscarPorIdComBloqueio(encontrado.get().getId()).orElseThrow();
            tokens.apagarDoUsuario(usuario.getId());
            ResetSenha reset = new ResetSenha(); reset.setUsuario(usuario); reset.setHash(hash(bruto));
            reset.setCriadoEm(Instant.now()); reset.setExpiraEm(Instant.now().plusSeconds(validade));
            tokens.save(reset); return usuario.getEmail();
        });
        if (destinatario != null) email.enviar(destinatario, bruto);
    }
    public void redefinir(String token, String novaSenha, String ip) {
        limite.verificar("reset-ip:" + hash(ip));
        PoliticaSenha.validar(novaSenha);
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
        tx.executeWithoutResult(status -> {
            var primeiro = tokens.findByHash(hash(token)).orElseThrow(this::invalido);
            var usuario = usuarios.buscarPorIdComBloqueio(primeiro.getUsuario().getId()).orElseThrow(this::invalido);
            // Reconsulta após lock; outro consumidor pode ter removido o token enquanto esperávamos.
            if (tokens.consumir(hash(token), Instant.now()) != 1) throw invalido();
            if (encoder.matches(novaSenha, usuario.getSenha())) throw new IllegalArgumentException("A nova senha deve ser diferente da atual.");
            usuario.setSenha(encoder.encode(novaSenha));
            usuario.setVersaoCredenciais(Math.incrementExact(usuario.getVersaoCredenciais()));
            tokens.apagarDoUsuario(usuario.getId());
        });
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString="${senha.reset.limpeza-intervalo-ms:3600000}")
    public void limparExpirados() { tx.executeWithoutResult(s -> tokens.apagarExpirados(Instant.now())); }
    static String hash(String valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private ResponseStatusException invalido() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperação inválido ou expirado."); }
}
