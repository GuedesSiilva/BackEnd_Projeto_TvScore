import asyncio
import re
import unicodedata
import httpx
from fastapi import HTTPException
from config import TMDB_API_KEY, TMDB_URL


def normalizar(nome):
    texto = unicodedata.normalize("NFKD", str(nome))
    return re.sub(r"[^a-z0-9]+", "-", "".join(c for c in texto if not unicodedata.combining(c)).lower()).strip("-")


async def preparar_onboarding(dados):
    saida = {"filmes": {"generos": [], "preferidos": []}, "series": {"generos": [], "preferidos": []}}
    vistos = set()
    for item in dados.get("preferidos", {}).get("conteudos", []):
        tipo = "TV" if item.get("tipoConteudo") == "SERIE" else item.get("tipoConteudo")
        chave = (tipo, item.get("id"))
        if tipo not in ("MOVIE", "TV") or not isinstance(chave[1], int) or chave in vistos:
            continue
        vistos.add(chave)
        destino = saida["filmes" if tipo == "MOVIE" else "series"]
        destino["preferidos"].append({"id": chave[1], "generos": [g["id"] for g in item.get("generos", []) if isinstance(g.get("id"), int)], "sinopse": item.get("sinopse") or ""})
    selecionados = dados.get("generos", {}).get("generos", [])
    if not selecionados:
        return saida
    if not TMDB_API_KEY:
        raise HTTPException(status_code=503, detail="Catálogo de gêneros não configurado.")
    async with httpx.AsyncClient(timeout=10, limits=httpx.Limits(max_connections=2)) as client:
        async def resolver(tipo, destino):
            try:
                r = await client.get(f"{TMDB_URL}/genre/{tipo}/list", params={"api_key": TMDB_API_KEY, "language": "pt-BR"})
                r.raise_for_status()
                catalogo = r.json()["genres"]
                for genero in catalogo:
                    for selecionado in selecionados:
                        codigo = selecionado.get("codigo", "")
                        if codigo == f"tmdb-{genero['id']}" or normalizar(selecionado.get("nome")) == normalizar(genero["name"]):
                            destino["generos"].append(genero["id"])
                            break
            except (httpx.HTTPError, ValueError, KeyError, TypeError):
                raise HTTPException(status_code=502, detail="Não foi possível resolver os gêneros do onboarding.") from None
        await asyncio.gather(resolver("movie", saida["filmes"]), resolver("tv", saida["series"]))
    return saida


def aplicar_sinais(preferencias, onboarding):
    pontos = dict(preferencias)
    # Gênero +2, título +1/gênero; favorito permanece +3. Sementes não viram favoritos.
    for genero in set(onboarding.get("generos", [])):
        pontos[genero] = pontos.get(genero, 0) + 2
    for item in onboarding.get("preferidos", []):
        for genero in set(item.get("generos", [])):
            pontos[genero] = pontos.get(genero, 0) + 1
    return dict(sorted(pontos.items(), key=lambda p: (-p[1], p[0])))
