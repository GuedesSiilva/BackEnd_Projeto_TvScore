
import asyncio

from recomendador import identificar_preferencias, recomendar_filmes
from services.tmdb_service import (
    buscar_series_diversificadas,
    buscar_detalhes_serie
)
from services.similaridade_service import carregar_modelo


async def gerar_recomendacoes_series(avaliacoes, favoritos):
    # Calcula as preferências do usuário
    preferencias = await asyncio.to_thread(
        identificar_preferencias,
        avaliacoes,
        favoritos
    )

    generos = [
        genero
        for genero, pontos in preferencias.items()
        if pontos > 0
    ][:3]

    if not generos:
        generos = [18, 80]

    # Busca séries no TMDB
    series = await buscar_series_diversificadas(
        generos,
        quantidade=40
    )

    ids_excluidos = (
        {a["id"] for a in avaliacoes}
        | {f["id"] for f in favoritos}
    )

    # Reutiliza o algoritmo de recomendação
    resultado = recomendar_filmes(
        series,
        preferencias,
        ids_excluidos
    )

    # Séries avaliadas positivamente e favoritas
    ids_preferidos = {
        a["id"]
        for a in avaliacoes
        if a["nota"] >= 7
    }

    ids_preferidos.update(
        f["id"] for f in favoritos
    )

    sinopses_preferidas = []

    for serie_id in ids_preferidos:
        try:
            detalhes = await buscar_detalhes_serie(serie_id)

            if detalhes["sinopse"]:
                sinopses_preferidas.append(
                    detalhes["sinopse"]
                )
        except Exception:
            continue

    # Compara as sinopses usando IA
    if sinopses_preferidas and resultado:
        modelo = carregar_modelo()

        textos = sinopses_preferidas + [
            serie.get("sinopse") or ""
            for serie in resultado
        ]

        embeddings = await asyncio.to_thread(
            modelo.encode,
            textos,
            normalize_embeddings=True
        )

        quantidade_referencias = len(sinopses_preferidas)

        referencias = embeddings[:quantidade_referencias]
        candidatos = embeddings[quantidade_referencias:]

        similaridades = candidatos @ referencias.T

        for serie, valores in zip(resultado, similaridades):
            similaridade = max(0, float(valores.max()))

            serie["similaridade_sinopse"] = round(
                similaridade, 4
            )

            serie["score_recomendacao"] = round(
                serie["score_recomendacao"] * 0.8
                + similaridade * 100 * 0.2,
                2
            )

        resultado.sort(
            key=lambda serie: serie["score_recomendacao"],
            reverse=True
        )

    recomendacoes = resultado[:10]

    return {
        "tipo_conteudo": "SERIE",
        "preferencias": preferencias,
        "total": len(recomendacoes),
        "recomendacoes": recomendacoes
    }
