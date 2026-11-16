
import asyncio

from recomendador import identificar_preferencias, recomendar_filmes
from services.tmdb_service import buscar_filmes_diversificados, buscar_detalhes_filme
from services.similaridade_service import carregar_modelo


async def gerar_recomendacoes(avaliacoes, favoritos):
    preferencias = identificar_preferencias(avaliacoes, favoritos)

    generos = [
        genero
        for genero, pontos in preferencias.items()
        if pontos > 0
    ][:3]

    if not generos:
        generos = [28, 878]

    filmes = await buscar_filmes_diversificados(generos, quantidade=40)

    ids_excluidos = (
        {a["id"] for a in avaliacoes}
        | {f["id"] for f in favoritos}
    )

    resultado = recomendar_filmes(
        filmes, preferencias, ids_excluidos
    )

    # Filmes que o usuário avaliou positivamente
    filmes_preferidos = [
        a["id"] for a in avaliacoes if a["nota"] >= 7
    ]

    filmes_preferidos += [
        f["id"] for f in favoritos
    ]

    sinopses_preferidas = []

    for filme_id in set(filmes_preferidos):
        try:
            detalhes = await buscar_detalhes_filme(filme_id)
            if detalhes["sinopse"]:
                sinopses_preferidas.append(detalhes["sinopse"])
        except Exception:
            continue

    if sinopses_preferidas and resultado:
        modelo = carregar_modelo()

        textos = sinopses_preferidas + [
            filme.get("sinopse") or ""
            for filme in resultado
        ]

        embeddings = await asyncio.to_thread(
            modelo.encode,
            textos,
            normalize_embeddings=True
        )

        quantidade_referencias = len(sinopses_preferidas)
        referencias = embeddings[:quantidade_referencias]
        candidatos = embeddings[quantidade_referencias:]

        # Similaridade máxima com os filmes favoritos
        similaridades = candidatos @ referencias.T

        for filme, valores in zip(resultado, similaridades):
            similaridade = max(0, float(valores.max()))

            filme["similaridade_sinopse"] = round(
                similaridade, 4
            )

            # Combina algoritmo anterior (80%)
            # com similaridade semântica (20%)
            filme["score_recomendacao"] = round(
                filme["score_recomendacao"] * 0.8
                + similaridade * 100 * 0.2,
                2
            )

        resultado.sort(
            key=lambda filme: filme["score_recomendacao"],
            reverse=True
        )

    recomendacoes = resultado[:10]

    return {
        "preferencias": preferencias,
        "total": len(recomendacoes),
        "recomendacoes": recomendacoes
    }
