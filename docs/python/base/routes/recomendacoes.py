
from fastapi import APIRouter, Query, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from models.schemas import DadosUsuario
from services.tmdb_service import buscar_filmes
from services.recomendacao_service import gerar_recomendacoes
from services.recomendacao_series_service import gerar_recomendacoes_series
from services.integracao_java_service import buscar_dados_usuario
from services.preparacao_dados_service import preparar_dados_usuario


router = APIRouter(prefix="/recomendacoes", tags=["Recomendações"])


@router.get("")
async def recomendar(
    generos: list[int] = Query(default=[878, 28]),
    quantidade: int = Query(default=10, ge=1, le=20)
):
    filmes = await buscar_filmes(
        generos,
        quantidade,
        ordenar_por_nota=True
    )

    return {
        "generos_selecionados": generos,
        "total": len(filmes),
        "recomendacoes": filmes
    }


@router.get("/personalizadas")
async def recomendacoes_personalizadas():
    # Dados simulados para testes
    avaliacoes = [
        {"id": 157336, "nota": 10, "generos": [878, 12, 18]},
        {"id": 27205, "nota": 9, "generos": [28, 878, 12]},
        {"id": 550, "nota": 2, "generos": [18]}
    ]

    favoritos = [
        {"id": 603, "generos": [28, 878]}
    ]

    return await gerar_recomendacoes(
        avaliacoes, favoritos
    )


@router.post("/usuario")
async def recomendar_usuario(dados: DadosUsuario):
    avaliacoes = [
        a.model_dump() for a in dados.avaliacoes
    ]

    favoritos = [
        f.model_dump() for f in dados.favoritos
    ]

    return await gerar_recomendacoes(
        avaliacoes, favoritos
    )


@router.post("/series")
async def recomendar_series_usuario(dados: DadosUsuario):
    avaliacoes = [
        avaliacao.model_dump()
        for avaliacao in dados.avaliacoes
    ]

    favoritos = [
        favorito.model_dump()
        for favorito in dados.favoritos
    ]

    return await gerar_recomendacoes_series(
        avaliacoes,
        favoritos
    )


seguranca = HTTPBearer()


@router.get("/teste-java")
async def testar_integracao_java(
    credenciais: HTTPAuthorizationCredentials = Depends(seguranca)
):
    token = credenciais.credentials

    return await buscar_dados_usuario(token)


@router.get("/teste-preparacao")
async def testar_preparacao(
    credenciais: HTTPAuthorizationCredentials = Depends(seguranca)
):
    token = credenciais.credentials

    dados_java = await buscar_dados_usuario(token)

    dados_preparados = await preparar_dados_usuario(dados_java)

    return dados_preparados

@router.get("/series/java")
async def recomendar_series_com_java(
    credenciais: HTTPAuthorizationCredentials = Depends(seguranca)
):
    token = credenciais.credentials

    dados_java = await buscar_dados_usuario(token)

    dados = await preparar_dados_usuario(dados_java)

    avaliacoes = dados["series"]["avaliacoes"]
    favoritos = dados["series"]["favoritos"]

    return await gerar_recomendacoes_series(
        avaliacoes,
        favoritos
    )

@router.get("/filmes/java")
async def recomendar_filmes_com_java(
    credenciais: HTTPAuthorizationCredentials = Depends(seguranca)
):
    token = credenciais.credentials

    # Busca avaliações e favoritos no Java
    dados_java = await buscar_dados_usuario(token)

    # Organiza os dados e consulta os gêneros no TMDB
    dados = await preparar_dados_usuario(dados_java)

    # Seleciona somente os filmes
    avaliacoes = dados["filmes"]["avaliacoes"]
    favoritos = dados["filmes"]["favoritos"]

    # Gera recomendações de filmes com IA
    return await gerar_recomendacoes(
        avaliacoes,
        favoritos
    )





