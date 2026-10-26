package br.com.senai.tv_score.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Regras de validação lançadas pelos Services.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Validações dos DTOs: @NotNull, @Size, @Positive etc.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String mensagens = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(campo ->
                        campo.getField() + ": " + campo.getDefaultMessage()
                )
                .distinct()
                .collect(Collectors.joining("; "));

        return erro(
                HttpStatus.BAD_REQUEST,
                "Erro de validação: " + mensagens
        );
    }

    // Exemplos: pagina=abc ou tipoConteudo=INVALIDO.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        Class<?> tipo = ex.getRequiredType();

        String mensagem =
                "Valor inválido para o parâmetro '" + ex.getName() + "'.";

        if (tipo != null && tipo.isEnum()) {
            String permitidos = Arrays.stream(tipo.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            mensagem += " Valores permitidos: " + permitidos + ".";
        } else if (tipo == Integer.class || tipo == int.class
                || tipo == Long.class || tipo == long.class) {
            mensagem += " Informe um número inteiro válido.";
        } else if (tipo == Double.class || tipo == double.class) {
            mensagem += " Informe um número válido.";
        }

        return erro(HttpStatus.BAD_REQUEST, mensagem);
    }

    // Exemplo: não informar tipoConteudo em uma rota que o exige.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> handleMissingParameter(
            MissingServletRequestParameterException ex
    ) {
        return erro(
                HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório."
        );
    }

    // JSON malformado, tipo inválido no corpo ou corpo obrigatório ausente.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleInvalidBody(
            HttpMessageNotReadableException ex
    ) {
        return erro(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Confira o JSON, "
                        + "os tipos dos campos e os valores MOVIE ou TV."
        );
    }

    // Preserva os status definidos explicitamente nos Services.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> handleResponseStatus(
            ResponseStatusException ex
    ) {
        String mensagem = ex.getReason() != null
                ? ex.getReason()
                : "Não foi possível concluir a requisição.";

        return ResponseEntity.status(ex.getStatusCode())
                .headers(ex.getHeaders())
                .body(new ErroResponse(mensagem));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> handleNotFound(
            NoResourceFoundException ex
    ) {
        return erro(HttpStatus.NOT_FOUND, "Rota não encontrada.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(ex.getHeaders())
                .body(new ErroResponse(
                        "Método HTTP não permitido para esta rota."
                ));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex
    ) {
        return erro(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato não suportado. Para enviar JSON, "
                        + "use Content-Type: application/json."
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAuthentication(
            AuthenticationException ex
    ) {
        return erro(
                HttpStatus.UNAUTHORIZED,
                "Autenticação necessária ou credenciais inválidas."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(
            AccessDeniedException ex
    ) {
        return erro(
                HttpStatus.FORBIDDEN,
                "Você não tem permissão para realizar esta operação."
        );
    }

    // Trata duplicidade do MySQL, inclusive em requisições simultâneas.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(
            DataIntegrityViolationException ex
    ) {
        Throwable causa = ex;

        while (causa != null) {
            if (causa instanceof SQLException sql
                    && sql.getErrorCode() == 1062) {
                return erro(
                        HttpStatus.CONFLICT,
                        "Já existe um registro com esses dados."
                );
            }

            causa = causa.getCause();
        }

        // Outras falhas de integridade não são automaticamente duplicidade.
        return erroInterno(ex);
    }

    // Respostas de erro recebidas do TMDB.
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErroResponse> handleExternalResponse(
            RestClientResponseException ex
    ) {
        int status = ex.getStatusCode().value();

        log.warn("Serviço externo retornou HTTP {}.", status);

        if (status == 404) {
            return erro(
                    HttpStatus.NOT_FOUND,
                    "Conteúdo não encontrado no serviço de catálogo."
            );
        }

        if (status == 429) {
            return erro(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "O catálogo atingiu o limite de consultas. "
                            + "Tente novamente em instantes."
            );
        }

        return erro(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível obter os dados do serviço de catálogo."
        );
    }

    // Falhas de rede, conexão ou timeout.
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErroResponse> handleExternalConnection(
            ResourceAccessException ex
    ) {
        Throwable causa = ex;

        while (causa != null) {
            if (causa instanceof SocketTimeoutException
                    || causa instanceof HttpTimeoutException) {
                return erro(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "O serviço de catálogo demorou para responder."
                );
            }

            causa = causa.getCause();
        }

        return erro(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível conectar ao serviço de catálogo."
        );
    }

    // Outras falhas do cliente HTTP, como conversão de resposta.
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErroResponse> handleExternalClient(
            RestClientException ex
    ) {
        return erro(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível processar a resposta do catálogo."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleUnexpected(
            Exception ex
    ) {
        return erroInterno(ex);
    }

    private ResponseEntity<ErroResponse> erroInterno(Exception ex) {
        String referencia = UUID.randomUUID().toString();

        // Não registra corpo, URL externa, senha ou token.
        log.error(
                "Falha interna. referencia={}, tipo={}",
                referencia,
                ex.getClass().getName()
        );

        return erro(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno. Referência: " + referencia
        );
    }

    private ResponseEntity<ErroResponse> erro(
            HttpStatus status,
            String mensagem
    ) {
        return ResponseEntity.status(status)
                .body(new ErroResponse(mensagem));
    }
}