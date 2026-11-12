package br.com.senai.tv_score.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import br.com.senai.tv_score.usuarios.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
class JwtServiceTest {
    private final JwtService jwt = new JwtService("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", 2592000);
    @Test void identidadeImutavelERejeicaoDeEmail() {
        assertEquals(12L, jwt.extrairUsuarioIdSeValido(jwt.gerarToken(12L)).orElseThrow());
        String antigo = io.jsonwebtoken.Jwts.builder().subject("a@example.com")
            .expiration(new Date(System.currentTimeMillis()+100000))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[32])).compact();
        assertTrue(jwt.extrairUsuarioIdSeValido(antigo).isEmpty());
        assertTrue(jwt.extrairUsuarioIdSeValido("invalido").isEmpty());
    }
    @Test void contaRecriadaNaoRecebeIdentidadeDaContaExcluida() throws Exception {
        var repo = mock(UsuarioRepository.class);
        var nova = new Usuario("Nova", "a@example.com", "hash");
        when(repo.findById(12L)).thenReturn(Optional.empty());
        when(repo.findByEmail("a@example.com")).thenReturn(Optional.of(nova));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.gerarToken(12L));
        SecurityContextHolder.clearContext();
        try {
            new JwtAuthenticationFilter(jwt, repo).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(repo).findById(12L);
            verify(repo, never()).findByEmail(anyString());
        } finally { SecurityContextHolder.clearContext(); }
    }
}
