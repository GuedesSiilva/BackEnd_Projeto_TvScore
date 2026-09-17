package br.com.senai.tv_score.auth;

import br.com.senai.tv_score.exceptions.ErroResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
     LoginResponse response = authService.login(request);

     if(response == null){
         return ResponseEntity.status(401)
                 .body(new ErroResponse("E-mail ou senha inválidos"));
     }
     return ResponseEntity.ok(response);
    }
}
