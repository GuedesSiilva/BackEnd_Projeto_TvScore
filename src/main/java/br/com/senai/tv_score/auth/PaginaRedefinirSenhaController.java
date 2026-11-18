package br.com.senai.tv_score.auth;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaginaRedefinirSenhaController {

    @GetMapping("/redefinir-senha")
    public ResponseEntity<Resource> pagina() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("Content-Security-Policy",
                        "default-src 'none'; script-src 'self'; style-src 'self'; "
                                + "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
                .body(new ClassPathResource("paginas/redefinir-senha.html"));
    }
}
