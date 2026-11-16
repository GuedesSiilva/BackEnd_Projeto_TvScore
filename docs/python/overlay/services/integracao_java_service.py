import asyncio
import os
import httpx
from fastapi import HTTPException
from dotenv import load_dotenv

load_dotenv()
JAVA_API_URL = os.getenv("JAVA_API_URL", "http://localhost:8080").rstrip("/")


async def buscar_dados_usuario(token: str):
    rotas = {
        "avaliacoes": "/api/avaliacoes/minhas",
        "favoritos": "/api/favoritos",
        "generos": "/api/usuarios/eu/generos",
        "preferidos": "/api/usuarios/eu/conteudos-preferidos",
    }
    try:
        # Cliente reutilizado pelas quatro chamadas, fechado também em falhas.
        async with httpx.AsyncClient(timeout=15, limits=httpx.Limits(max_connections=4)) as client:
            async def consultar(chave, rota):
                resposta = await client.get(JAVA_API_URL + rota, headers={"Authorization": f"Bearer {token}"})
                resposta.raise_for_status()
                valor = resposta.json()
                if chave in ("avaliacoes", "favoritos"):
                    if not isinstance(valor, list):
                        raise ValueError("Lista inválida")
                else:
                    campo = "generos" if chave == "generos" else "conteudos"
                    if not isinstance(valor, dict) or not isinstance(valor.get(campo), list):
                        raise ValueError("Preferências inválidas")
                return chave, valor
            resultados = await asyncio.wait_for(asyncio.gather(*(consultar(k, v) for k, v in rotas.items()), return_exceptions=True), 20)
            erros = [r for r in resultados if isinstance(r, BaseException)]
            # Aguarda as independentes e prioriza autorização; não converte 401 em histórico vazio.
            erros.sort(key=lambda e: 0 if isinstance(e, httpx.HTTPStatusError) and e.response.status_code in (401, 403) else 1)
            if erros:
                raise erros[0]
            return dict(resultados)
    except httpx.HTTPStatusError as erro:
        status = erro.response.status_code
        raise HTTPException(status_code=status if status in (401, 403, 429) else 502,
                            detail="Falha ao consultar histórico autenticado no Java.") from None
    except (httpx.TimeoutException, asyncio.TimeoutError):
        raise HTTPException(status_code=504, detail="Prazo do backend Java excedido.") from None
    except (httpx.RequestError, ValueError):
        raise HTTPException(status_code=502, detail="Resposta inválida ou backend Java indisponível.") from None
