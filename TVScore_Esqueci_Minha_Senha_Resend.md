# TV Score — esqueci minha senha com Resend

Esta é uma proposta revisável para o backend Java. O código abaixo é o adaptador de envio completo. Os pontos de ligação com a recuperação existente dependem do código atual: o checkout Java não está disponível no workspace e a leitura do GitHub não retornou o código. Nenhuma rota, assinatura, tabela ou migração nova é assumida.

A abordagem segue [Ponytail](https://github.com/DietrichGebert/ponytail/blob/main/skills/ponytail/SKILL.md): reaproveitar o fluxo e a política de senha existentes, usar o cliente HTTP do Spring e acrescentar somente o necessário para entregar o e-mail.

## Alterações para revisão

| Local | Mudança |
| --- | --- |
| Serviço atual de envio de recuperação | Trocar o transporte SMTP por HTTPS onde hoje é entregue o link, reaproveitando a interface se houver. |
| Configuração do Java | Adicionar apenas a propriedade da chave Resend. Reaproveitar a flag, o remetente e a URL existentes. |
| Serviço de recuperação e transação | Inspecionar a emissão e o consumo do token. Corrigir somente as lacunas que impedirem o fluxo seguro. |
| Flutter | Ligar “Esqueci minha senha” ao contrato atual e confirmar que o destino HTTPS do link abre uma tela real de redefinição. |

## Código de referência do envio HTTPS

Caminho sugerido, se não houver classe de envio a reaproveitar: `src/main/java/br/com/senai/tv_score/usuarios/ResendRecuperacaoEmail.java`. Se o projeto já tiver um remetente dedicado, transfira o comportamento para ele e mantenha os chamadores. Use o estilo de cliente HTTP existente se ele já oferecer os mesmos limites e isolamento dos cabeçalhos.

```java
package br.com.senai.tv_score.usuarios;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ResendRecuperacaoEmail {

    private final RestClient http;
    private final boolean habilitado;
    private final String apiKey;
    private final String remetente;

    public ResendRecuperacaoEmail(
            @Value("${senha.reset.email-habilitado:false}") boolean habilitado,
            @Value("${senha.reset.resend-api-key:}") String apiKey,
            @Value("${senha.reset.remetente:no-reply@localhost}") String remetente) {
        if (habilitado && (apiKey.isBlank() || remetente.isBlank()
                || remetente.equals("no-reply@localhost"))) {
            throw new IllegalStateException(
                    "Configure RESEND_API_KEY e RESET_REMETENTE para enviar e-mails de recuperação.");
        }
        this.habilitado = habilitado;
        this.apiKey = apiKey;
        this.remetente = remetente;

        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .requestFactory(requestFactory)
                .build();
    }

    public boolean enviar(String destinatario, String link, long validadeSegundos) {
        if (!habilitado) {
            return false;
        }
        String mensagem = """
                Recebemos um pedido para redefinir sua senha no TV Score.

                Abra este link para escolher uma nova senha:
                %s

                O link é de uso único e expira em %d minutos ou menos.
                Se você não fez esse pedido, ignore este e-mail.
                """.formatted(link, Math.max(1, (validadeSegundos + 59) / 60));

        try {
            return http.post()
                    .uri("/emails")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", remetente, "to", List.of(destinatario),
                            "subject", "Redefina sua senha do TV Score", "text", mensagem))
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            return false;
        }
    }
}
```

O chamador mantém a verificação de `senha.reset.email-habilitado` antes do envio. Passe o e-mail cadastrado da conta, o link já montado pelo serviço existente e a validade configurada. Trate `false` como falha de envio com log operacional genérico, sem adicionar a exceção original ao log. O retorno `true` indica aceitação pelo provedor. A entrega na caixa de entrada é verificada separadamente. Se a assinatura existente for diferente, mantenha o contrato e adapte esse trecho ao serviço atual.

## Configuração

Acrescente no `src/main/resources/application.properties`:

```properties
senha.reset.resend-api-key=${RESEND_API_KEY:}
```

No Render do **backend Java**, configure:

| Variável | Valor |
| --- | --- |
| `RESEND_API_KEY` | Chave de envio criada no Resend |
| `RESET_REMETENTE` | Exemplo: `TV Score <conta@seudominio.com>`, em domínio verificado no Resend |
| `RESET_URL` | URL HTTPS da tela pública de redefinição |
| `RESET_EMAIL_HABILITADO` | `true` após configurar o remetente, a chave e a tela |
| `RESET_VALIDADE_SEGUNDOS` | Preserve o padrão de 1200 segundos mostrado no projeto |

No IntelliJ, configure as mesmas propriedades só quando for testar envio real. Para testes automatizados, intercepte o cliente HTTP ou substitua o remetente. Desative rastreamento de cliques para mensagens de recuperação, se habilitado no provedor, e evite analytics na tela que recebe o token. O token, a chave e a senha ficam fora de logs, métricas, artefatos e repositório.

## Fluxo a conferir no Java e no Flutter

1. O Flutter pede recuperação com o e-mail digitado, sem JWT. Para um endereço sintaticamente válido, devolva resposta pública uniforme para conta existente, inexistente e falha de entrega. Preserve o status e o JSON que o Flutter já usa. Reaproveite limites por endereço e origem, considerando apenas IP de proxy confiável. Mantenha a latência semelhante entre os caminhos. A chamada externa deve ficar fora da resposta pública e usar a infraestrutura assíncrona existente ou, se ausente, execução limitada com fila pequena, após o commit.
2. Para a conta existente, gere um token imprevisível e vinculado à conta. Persista só o hash e a expiração. Reuse o esquema existente se ele suportar isso. Monte o link com a base fixa de `RESET_URL` e parâmetro codificado. Em produção, exija HTTPS. Envie o token somente depois de confirmar a transação que o gravou. Se o envio falhar, permita nova tentativa dentro dos limites.
3. Abrir o link apenas mostra a tela. A pessoa digita e confirma a nova senha. O Flutter envia token e nova senha à rota de confirmação, sem expor esses dados em logs. Se a tela ainda não existe, configure uma página HTTPS funcional ou um link HTTPS associado ao app, com destino funcional quando o app não estiver instalado.
4. Na confirmação, valide o token e a senha com a política atual. Grave o hash da senha com o `PasswordEncoder` existente e consuma o token na mesma transação, com bloqueio ou atualização condicional para impedir dois usos concorrentes. Invalide os demais tokens ativos da conta. Se o projeto já tiver versão de credencial ou invalidação de sessão, use esse mecanismo para encerrar sessões anteriores e peça novo login.

## Aceite antes de publicar

- Compile o Java. Teste com provedor simulado: POST esperado e corpo com link, rejeição e timeout sem revelar dados e pedido público uniforme.
- Teste um link válido, expirado e reutilizado, inclusive duas confirmações concorrentes. Somente uma confirmação deve trocar a senha. Confirme que abrir o link sozinho não altera nada.
- Confira a tela pelo caminho que a pessoa realmente usa no celular. Só depois teste entrega real para uma conta sua e confirme o login com a nova senha.
- Se o esquema atual não suportar uso único ou expiração, apresente a migração separadamente com o motivo. Use banco isolado durante os testes.

Nenhum código foi aplicado ao checkout do usuário. A compilação, os testes integrados, as rotas atuais e a entrega real permanecem pendentes. O trecho é uma referência para mesclar, não um patch já validado.

Fontes: [Resend Email API](https://resend.com/docs/api-reference/emails/send-email), [clientes HTTP do Spring](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html) e [limites de SMTP do Render](https://render.com/docs/free).
