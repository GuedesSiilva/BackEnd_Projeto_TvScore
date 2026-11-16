
from services.tmdb_service import (
    buscar_detalhes_filme,
    buscar_detalhes_serie
)


async def preparar_dados_usuario(dados_java):
    avaliacoes_filmes = []
    avaliacoes_series = []
    favoritos_filmes = []
    favoritos_series = []

    for avaliacao in dados_java.get("avaliacoes", []):
        tipo = avaliacao.get("tipoConteudo")
        conteudo_id = avaliacao.get("filmeSerieId")

        if tipo not in ("MOVIE", "TV") or conteudo_id is None:
            continue

        try:
            if tipo == "MOVIE":
                detalhes = await buscar_detalhes_filme(conteudo_id)
            else:
                detalhes = await buscar_detalhes_serie(conteudo_id)

            dados = {
                "id": conteudo_id,
                "nota": avaliacao["nota"],
                "generos": detalhes["generos"],
                "comentario": avaliacao.get("comentario")
            }

            if tipo == "MOVIE":
                avaliacoes_filmes.append(dados)
            else:
                avaliacoes_series.append(dados)

        except Exception:
            continue

    for favorito in dados_java.get("favoritos", []):
        tipo = favorito.get("tipoConteudo")
        conteudo_id = favorito.get("filmeSerieId")

        if tipo not in ("MOVIE", "TV") or conteudo_id is None:
            continue

        try:
            if tipo == "MOVIE":
                detalhes = await buscar_detalhes_filme(conteudo_id)
            else:
                detalhes = await buscar_detalhes_serie(conteudo_id)

            dados = {
                "id": conteudo_id,
                "generos": detalhes["generos"]
            }

            if tipo == "MOVIE":
                favoritos_filmes.append(dados)
            else:
                favoritos_series.append(dados)

        except Exception:
            continue

    return {
        "filmes": {
            "avaliacoes": avaliacoes_filmes,
            "favoritos": favoritos_filmes
        },
        "series": {
            "avaliacoes": avaliacoes_series,
            "favoritos": favoritos_series
        }
    }
