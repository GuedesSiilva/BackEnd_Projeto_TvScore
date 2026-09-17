package br.com.senai.tv_score.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        boolean loginValido = request.email().equals("teste@email.com")
                && request.senha().equals("1234");

        if (!loginValido) {
            return null;
        }

        return new LoginResponse(
                "token-mock-abc123",
                "Usuario teste",
                request.email()
        );
    }

    public boolean tokenValido(String token){
          return "token-mock-abc123".equals(token);
    }
}
