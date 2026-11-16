
import os
import httpx

from fastapi import HTTPException
from dotenv import load_dotenv

load_dotenv()

JAVA_API_URL = os.getenv(
    "JAVA_API_URL",
    "http://localhost:8080"
)


async def buscar_dados_usuario(token: str):
    headers = {
        "Authorization": f"Bearer {token}"
    }

    try:
        async with httpx.AsyncClient(timeout=15) as client:
            resposta_avaliacoes = await client.get(
                f"{JAVA_API_URL}/api/avaliacoes/minhas",
                headers=headers
            )

            resposta_favoritos = await client.get(
                f"{JAVA_API_URL}/api/favoritos",
                headers=headers
            )

            resposta_avaliacoes.raise_for_status()
            resposta_favoritos.raise_for_status()

            return {
                "avaliacoes": resposta_avaliacoes.json(),
                "favoritos": resposta_favoritos.json()
            }

    except httpx.HTTPStatusError as erro:
        if erro.response.status_code in (401, 403):
            raise HTTPException(
                status_code=erro.response.status_code,
                detail="Acesso não autorizado pelo backend Java."
            )

        raise HTTPException(
            status_code=502,
            detail="Erro ao consultar o backend Java."
        )

    except httpx.RequestError:
        raise HTTPException(
            status_code=502,
            detail="Não foi possível conectar ao backend Java."
        )
