package br.com.senai.tv_score.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final JwtParser parser;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET deve conter pelo menos 32 bytes "
                            + "aleatórios codificados em Base64."
            );
        }

        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "A validade do token deve ser positiva."
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String gerarToken(Long usuarioId) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject("usuario:" + java.util.Objects.requireNonNull(usuarioId))
                .issuedAt(Date.from(agora))
                .expiration(
                        Date.from(agora.plusSeconds(expirationSeconds))
                )
                .signWith(signingKey)
                .compact();
    }

    public Optional<Long> extrairUsuarioIdSeValido(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            if (claims.getExpiration() == null || subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            if (!subject.matches("usuario:[1-9][0-9]*")) return Optional.empty();
            return Optional.of(Long.parseLong(subject.substring(8)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
