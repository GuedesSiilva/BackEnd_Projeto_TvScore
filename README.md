<div align="center">
  <img src="https://raw.githubusercontent.com/victorbispoo/tvScoreFrontFlutter/1beefd9c7c7db9ed5963d6ee073c3e7b3be7f45a/lib/assets/tvScoreLogin.png" alt="Logo do TV Score" width="180" />

  <h1>TV Score · Backend</h1>

  <p><strong>A API por trás da sua próxima história favorita.</strong></p>
  <p>Catálogo de filmes e séries, avaliações e favoritos para o aplicativo TV Score.</p>

  <img src="https://img.shields.io/badge/Java-21-091426?style=flat-square" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4.1.1" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/TMDB-01B4E4?style=flat-square" alt="TMDB" />

  <p>
    <a href="https://tvscore-backend.onrender.com/swagger-ui/index.html">Swagger</a>
    · <a href="#executar-localmente">Como rodar</a>
    · <a href="#rotas-principais">Rotas da API</a>
    · <a href="https://github.com/victorbispoo/tvScoreFrontFlutter">Frontend Flutter</a>
  </p>
</div>

---

## Sobre o projeto

API REST do **TV Score**, aplicativo Flutter para descobrir, avaliar e favoritar filmes e séries. O backend consulta o TMDB para obter dados do catálogo e usa MySQL para persistir contas, preferências, favoritos e avaliações.

## O que a API oferece

- Pesquisa de filmes e séries na mesma rota, com nomes dos gêneros nos resultados e nos detalhes.
- Catálogo, destaques e filtros por gênero, nota, ano e serviço de streaming.
- Seção **Onde assistir** nos detalhes, com ofertas no Brasil agrupadas por modalidade, quando informadas pelo TMDB.
- Painel de episódios por temporada, com notas do TMDB e ranking dos episódios mais bem avaliados.
- Cadastro, login com JWT, edição de perfil e escolha de até cinco gêneros preferidos.
- Favoritos e avaliações próprias com nota de 0 a 10 e comentário opcional.

## Tecnologias

| Tecnologia | Uso no projeto |
| --- | --- |
| Java 21 e Spring Boot | Aplicação e rotas da API REST |
| Spring Security e JWT | Login, autenticação e controle de acesso |
| Spring Data JPA e MySQL | Persistência de contas, preferências, favoritos e avaliações |
| TMDB | Catálogo, detalhes, episódios e dados de streaming |
| Swagger e OpenAPI | Documentação interativa da API |
| Maven Wrapper e Docker | Build, testes e execução em contêiner |

## Executar localmente

**Pré-requisitos:** JDK 21, uma instância MySQL com o banco criado e uma chave da API do TMDB. O projeto inclui o Maven Wrapper.

```bash
git clone https://github.com/GuedesSiilva/BackEnd_Projeto_TvScore.git
cd BackEnd_Projeto_TvScore
```

Configure as variáveis de ambiente no terminal ou em **Run → Edit Configurations → Environment variables**, no IntelliJ.

| Variável | Valor esperado |
|---|---|
| `DB_URL` | URL JDBC, por exemplo `jdbc:mysql://localhost:3306/tv_score`. |
| `DB_USERNAME` | Usuário do MySQL. |
| `DB_PASSWORD` | Senha do MySQL. |
| `TMDB_API_KEY` | Chave da API v3 do TMDB, usada no parâmetro `api_key`. |
| `JWT_SECRET` | Segredo em Base64 gerado a partir de pelo menos 32 bytes aleatórios. |
| `JWT_EXPIRATION_SECONDS` | Validade positiva do token em segundos, por exemplo `2592000` para 30 dias. |

Use um banco de desenvolvimento para executar localmente e testar. A configuração atual usa `spring.jpa.hibernate.ddl-auto=update`, que permite ao Hibernate atualizar o esquema ao iniciar. Mantenha credenciais fora do Git.

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Linux e macOS
./mvnw spring-boot:run
```

- API local: `http://localhost:8080/api`
- [Swagger local](http://localhost:8080/swagger-ui/index.html)
- [Swagger no Render](https://tvscore-backend.onrender.com/swagger-ui/index.html)

## Executar com Docker

Crie um arquivo local `.env` com as seis variáveis da tabela, uma por linha no formato `NOME=valor`. A URL do banco deve ser acessível **a partir do contêiner**: se o MySQL estiver fora dele, `localhost` não aponta para o banco.

```bash
docker build -t tvscore-backend .
docker run --rm -p 8080:8080 --env-file .env tvscore-backend
```

## Rotas principais

| Área | Método e rota | Uso |
|---|---|---|
| Autenticação | `POST /api/auth/cadastrar` | Cadastro com `nome`, `email` e `senha`. |
| Autenticação | `POST /api/auth/login` | Login com `email` e `senha`; retorna `token`, `nome` e `email`. |
| Perfil | `GET /api/usuarios/eu`, `PUT /api/usuarios/eu` | Consultar perfil ou atualizar `nome`. |
| Preferências | `GET /api/usuarios/eu/generos`, `PUT /api/usuarios/eu/generos` | Consultar ou salvar códigos de gêneros preferidos. |
| Destaques | `GET /api/filmes/em-alta`, `GET /api/series/em-alta` | Buscar títulos em alta. |
| Pesquisa | `GET /api/filmes/pesquisar?q=Batman&pagina=1` | Buscar filmes e séries na mesma lista. |
| Detalhes | `GET /api/filmes/{id}`, `GET /api/series/{id}` | Consultar um título pelo ID do TMDB. |
| Gêneros | `GET /api/conteudos/generos`, `GET /api/conteudos/por-genero?genero=drama` | Listar códigos e filtrar filmes e séries. |
| Gêneros de filmes | `GET /api/filmes/generos`, `GET /api/filmes/generos/{generoId}` | Listar gêneros do TMDB ou filtrar filmes pelo ID do gênero. |
| Catálogo | `GET /api/filmes/catalogo`, `GET /api/filmes/por-nota?minima=7&maxima=10`, `GET /api/filmes/por-ano/{ano}` | Listar e filtrar filmes. |
| Streamings | `GET /api/filmes/streamings`, `GET /api/filmes/streaming/{streamingId}` | Listar plataformas ou buscar filmes por assinatura na plataforma. |
| Episódios | `GET /api/series/{id}/episodios/painel?limite=10` | Retornar temporadas, episódios e `topRated`. |
| Episódios | `GET /api/series/{id}/temporadas/{temporada}`, `GET /api/series/{id}/temporadas/{temporada}/episodios/{episodio}` | Consultar uma temporada ou os detalhes de um episódio. |
| Favoritos | `GET /api/favoritos`, `POST /api/favoritos`, `DELETE /api/favoritos/{filmeSerieId}?tipoConteudo=TV` | Listar, adicionar ou remover favoritos. |
| Avaliações | `GET /api/avaliacoes/minhas`, `POST /api/avaliacoes` | Listar avaliações próprias ou salvar uma avaliação. |
| Avaliações | `PUT /api/avaliacoes/{id}`, `DELETE /api/avaliacoes/{id}` | Editar ou remover uma avaliação própria pelo **ID da avaliação**. |
| Avaliações de um título | `GET /api/avaliacoes/filme/{filmeSerieId}?tipoConteudo=TV` | Listar avaliações de um filme ou série. |
| Resumo | `GET /api/avaliacoes/conteudo/{filmeSerieId}/resumo?tipoConteudo=TV` | Retornar a média e a quantidade de avaliações do TV Score. |

Nas rotas autenticadas, use `Authorization: Bearer <token>`. Consulte o Swagger para os parâmetros e esquemas completos. O Flutter Web usa as origens permitidas em `CorsConfig.java`; a configuração atual aceita origens locais em `localhost` e `127.0.0.1`.

## Integração com o Flutter

- **Identificação de títulos:** use o ID do TMDB junto com `tipoConteudo` (`MOVIE` ou `TV`). Filmes e séries podem ter o mesmo ID numérico.
- **Gêneros:** pesquisa e detalhes retornam `genres`, uma lista de nomes. Outras listagens podem usar `genre_ids`. No filtro misto e nas preferências, use os códigos de `/api/conteudos/generos`, como `drama`. Essa lista reúne os gêneros de filmes e séries consultados no TMDB, com cache de seis horas, sem uma lista fixa local. Os códigos são os nomes sem acentos, em minúsculas e com hífens no lugar dos espaços (ex.: `ficcao-cientifica`); os oito códigos anteriores continuam compatíveis. Gêneros exclusivos de filmes ou de séries filtram somente o tipo correspondente.
- **Onde assistir:** o campo opcional `ondeAssistir` vem nos detalhes, com grupos como `assinatura`, `aluguel` e `compra`. O `link` aponta para a página de disponibilidade do título; a API não retorna um link direto para cada streaming.
- **Avaliações:** o `POST` recebe `filmeSerieId`, `tipoConteudo`, `nota` e `comentario`. Se já houver avaliação do mesmo usuário para aquele título e tipo, o `POST` a atualiza. O `PUT` usa o `id` da avaliação e recebe `nota` e `comentario`. Omitir o comentário no `PUT` limpa o valor anterior.

Exemplo de corpo para `POST /api/avaliacoes`:

```json
{
  "filmeSerieId": 550,
  "tipoConteudo": "MOVIE",
  "nota": 9.5,
  "comentario": "Gostei do filme."
}
```

## Organização do código

```text
src/main/
├── java/br/com/senai/tv_score/
│   ├── auth/        # Cadastro e login
│   ├── security/    # JWT e controle de acesso
│   ├── controller/  # Rotas HTTP
│   ├── filme/       # Catálogo do TMDB e episódios
│   ├── usuarios/    # Perfil e preferências
│   ├── favorito/    # Favoritos por usuário
│   ├── avaliacao/   # Notas, comentários e resumo
│   ├── dto/         # Contratos JSON
│   ├── config/      # CORS, OpenAPI e cliente HTTP
│   └── exceptions/  # Respostas de erro
└── resources/
    └── application.properties
```

## Testes e empacotamento

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Em Linux ou macOS, substitua `.\mvnw.cmd` por `./mvnw`. O teste de carregamento do contexto usa a configuração da aplicação; execute-o com as variáveis preenchidas e um banco de teste acessível.

## Créditos de dados

Metadados de filmes, séries e episódios: [The Movie Database (TMDB)](https://www.themoviedb.org/). Dados de disponibilidade de streaming: [JustWatch](https://www.justwatch.com/br), via TMDB.

---

<p align="center"><strong>TV Score</strong> · Backend do aplicativo Flutter.</p>
