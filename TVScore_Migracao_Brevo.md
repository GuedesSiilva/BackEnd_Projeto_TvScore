# TV Score — envio de recuperação pela Brevo

Este guia migra o transporte HTTPS do Resend para a Brevo. Use o checkout local mais recente como base. Se o novo assunto e a logo já estiverem aplicados, conserve os dois. Se ainda estiverem somente no Markdown visual, aplique os trechos pertinentes daquele arquivo na mesma revisão.

## Alterações Java

Em `EmailRecuperacao.java`, mantenha o cliente `RestClient` e sua fábrica HTTP. Ajuste a base para `https://api.brevo.com` e aponte a chave injetada para `${senha.reset.brevo-api-key:}`. Atualize a mensagem de configuração inválida para citar `BREVO_API_KEY`. Preserve o comportamento de deixar o envio desligado quando a flag estiver em false.

Substitua o payload Resend pelos campos JSON da Brevo. Os records abaixo podem ficar dentro do serviço, usando o import `java.util.List` existente:

```java
private record Remetente(String name, String email) {}
private record Destinatario(String email) {}

private record Mensagem(
        Remetente sender,
        List<Destinatario> to,
        String subject,
        String textContent,
        String htmlContent) {
    @Override
    public String toString() {
        return "Mensagem de recuperação";
    }
}
```

Na chamada de envio, conserve a construção de `texto` e `html`, incluindo a imagem HTTPS publicada. Troque somente o destino e a autenticação:

```java
var resposta = http.post().uri("/v3/smtp/email")
        .headers(headers -> headers.set("api-key", apiKey))
        .contentType(MediaType.APPLICATION_JSON)
        .body(new Mensagem(
                new Remetente("TV Score", remetente),
                List.of(new Destinatario(destinatario)),
                "Solicitação de redefinição de senha | TV Score",
                texto,
                html))
        .retrieve()
        .toBodilessEntity();
```

Mantenha o tratamento existente de resposta 2xx, falhas HTTP e fila. A aceitação pela Brevo é distinta da entrega na caixa de entrada. Se o remetente estiver no formato antigo `TV Score <email>`, configure apenas o endereço na variável. Atualize o mock para verificar a URL, o cabeçalho `api-key`, `sender.name`, `sender.email`, `to[0].email`, assunto, `textContent` e `htmlContent`; simule a resposta 201 com `messageId` e preserve os testes de falha e prazo.

## Configuração

No `application.properties`, mapeie `senha.reset.brevo-api-key=${BREVO_API_KEY:}` no lugar da propriedade de chave do Resend, depois de conferir seus usos no checkout. Preserve o padrão false para `RESET_EMAIL_HABILITADO`.

| Variável do Java | Valor |
| --- | --- |
| `BREVO_API_KEY` | Chave API restrita com leitura e escrita em E-mail transacional |
| `RESET_REMETENTE` | E-mail verificado na seção Remetentes da Brevo, sem nome entre sinais de menor e maior |
| `RESET_EMAIL_HABILITADO` | `true` para testar o envio |
| `RESET_URL` | URL HTTPS da tela de redefinição do ambiente |
| `RESET_VALIDADE_SEGUNDOS` | `1200` |

Use a chave nas variáveis do IntelliJ ou Render. Para testar entrega, solicite a recuperação com uma conta de teste no TV Score cujo endereço seja diferente do seu remetente. Verifique os registros transacionais da Brevo e a caixa de entrada. Depois valide a abertura do link na tela, a troca da senha e o login com a nova senha.

Esta entrega é um guia de mudanças. A compilação e o envio real dependem da aplicação no checkout local e da configuração do remetente.

[Referência oficial da API transacional da Brevo](https://developers.brevo.com/reference/send-transac-email).
