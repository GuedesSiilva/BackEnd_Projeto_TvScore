"""Contratos com transporte falso; não carrega modelos nem certifica inferência."""
import asyncio
import ast
import importlib.util
import pathlib
import sys
import types
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).parent / "overlay"


class HTTPException(Exception):
    def __init__(self, status_code, detail):
        self.status_code = status_code
        super().__init__(detail)


class RequestError(Exception):
    pass


class TimeoutException(RequestError):
    pass


class HTTPStatusError(Exception):
    def __init__(self, status):
        self.response = types.SimpleNamespace(status_code=status)


class ClienteFalso:
    chamadas = []
    falha = None
    async def __aenter__(self):
        return self
    async def __aexit__(self, *args):
        return False
    def __init__(self, **kwargs):
        pass
    async def get(self, url, headers=None, params=None):
        self.chamadas.append((url, headers))
        await asyncio.sleep(0)
        if self.falha:
            raise self.falha
        if url.endswith("/api/usuarios/eu/generos"):
            valor = {"generos": []}
        elif url.endswith("/conteudos-preferidos"):
            valor = {"conteudos": []}
        else:
            valor = []
        return types.SimpleNamespace(raise_for_status=lambda: None, json=lambda: valor)


def carregar(nome, arquivo):
    httpx = types.SimpleNamespace(AsyncClient=ClienteFalso, Limits=lambda **kw: kw,
                                  HTTPStatusError=HTTPStatusError, TimeoutException=TimeoutException,
                                  RequestError=RequestError, HTTPError=RequestError)
    modulos = {"httpx": httpx, "fastapi": types.SimpleNamespace(HTTPException=HTTPException),
               "dotenv": types.SimpleNamespace(load_dotenv=lambda: None),
               "config": types.SimpleNamespace(TMDB_API_KEY="", TMDB_URL="http://tmdb.test")}
    with patch.dict(sys.modules, modulos):
        spec = importlib.util.spec_from_file_location(nome, ROOT / arquivo)
        modulo = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(modulo)
        return modulo


class ContratoPythonTest(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        ClienteFalso.chamadas = []
        ClienteFalso.falha = None
        self.integracao = carregar("integracao_teste", "services/integracao_java_service.py")
        self.onboarding = carregar("onboarding_teste", "services/onboarding_service.py")

    async def test_quatro_fontes_mesmo_token_sem_historia(self):
        dados = await self.integracao.buscar_dados_usuario("conta-a")
        self.assertEqual({"avaliacoes", "favoritos", "generos", "preferidos"}, set(dados))
        self.assertEqual(4, len(ClienteFalso.chamadas))
        self.assertTrue(all(h["Authorization"] == "Bearer conta-a" for _, h in ClienteFalso.chamadas))

    async def test_autorizacao_rate_limit_timeout_nao_viram_vazio(self):
        for erro, status in [(HTTPStatusError(401), 401), (HTTPStatusError(403), 403),
                             (HTTPStatusError(429), 429), (TimeoutException(), 504), (ValueError(), 502)]:
            ClienteFalso.falha = erro
            with self.assertRaises(HTTPException) as ctx:
                await self.integracao.buscar_dados_usuario("conta-b")
            self.assertEqual(status, ctx.exception.status_code)

    async def test_preferidos_mistos_mesmo_id_sementes_separadas(self):
        item = {"id": 7, "generos": [{"id": 18}], "sinopse": "Texto"}
        dados = {"preferidos": {"conteudos": [{**item, "tipoConteudo": "MOVIE"},
                    {**item, "tipoConteudo": "TV"}, {**item, "tipoConteudo": "SERIE"}]}}
        res = await self.onboarding.preparar_onboarding(dados)
        self.assertEqual(1, len(res["filmes"]["preferidos"]))
        self.assertEqual(1, len(res["series"]["preferidos"]))
        self.assertEqual("Texto", res["series"]["preferidos"][0]["sinopse"])

    async def test_pesos_aditivos_sem_transformar_preferido_em_favorito(self):
        original = {18: 3}
        pontos = self.onboarding.aplicar_sinais(original, {"generos": [18], "preferidos": [{"generos": [18]}]})
        self.assertEqual(6, pontos[18])
        self.assertEqual({18: 3}, original)
        self.assertEqual({}, self.onboarding.aplicar_sinais({}, {}))

    async def test_sintaxe_dos_seis_arquivos(self):
        arquivos = list(ROOT.rglob("*.py"))
        self.assertEqual(6, len(arquivos))
        for arquivo in arquivos:
            ast.parse(arquivo.read_text(encoding="utf-8"), filename=str(arquivo))


if __name__ == "__main__":
    unittest.main()
