param([string]$Origem = 'C:/Users/25163231/Documents/recomendador-TvScore-main/recomendador-TvScore-main')
$ErrorActionPreference='Stop'
$destino=Join-Path (Get-Location) 'docs/python/overlay'
$utf8=[Text.UTF8Encoding]::new($false)
function Gravar([string]$Nome,[string]$Texto) { [IO.File]::WriteAllText((Join-Path $destino $Nome),$Texto,$utf8) }
$s=[IO.File]::ReadAllText((Join-Path $Origem 'services/preparacao_dados_service.py'))
if(!$s.Contains('preparar_onboarding')) {
    $s="from services.onboarding_service import preparar_onboarding`n"+$s
    $idx=$s.LastIndexOf('    return {')
    if($idx -lt 0){throw 'Retorno de preparação não encontrado'}
    $s=$s.Insert($idx+('    return {').Length,"`n        `"onboarding`": await preparar_onboarding(dados_java),")
}
Gravar 'services/preparacao_dados_service.py' $s
foreach($nome in @('services/recomendacao_service.py','services/recomendacao_series_service.py')) {
    $s=[IO.File]::ReadAllText((Join-Path $Origem $nome))
    if(!$s.Contains('aplicar_sinais')) {
        $s="from services.onboarding_service import aplicar_sinais`n"+$s
        $s=$s.Replace('(avaliacoes, favoritos):','(avaliacoes, favoritos, onboarding=None):')
        $idx=$s.IndexOf('    generos = [')
        if($idx -lt 0){throw 'Seleção de gêneros não encontrada'}
        $s=$s.Insert($idx,"    onboarding = onboarding or {}`n    preferencias = aplicar_sinais(preferencias, onboarding)`n`n")
        $s=$s.Replace('    resultado = recomendar_filmes(',"    ids_excluidos.update(p[`"id`"] for p in onboarding.get(`"preferidos`", []))`n    resultado = recomendar_filmes(")
        $s=$s.Replace('    sinopses_preferidas = []','    sinopses_preferidas = [p["sinopse"] for p in onboarding.get("preferidos", []) if p.get("sinopse")]')
        if($nome.Contains('series')) { $s=$s.Replace('"tipo_conteudo": "SERIE"','"tipo_conteudo": "TV"') }
    }
    Gravar $nome $s
}
New-Item -ItemType Directory -Force (Join-Path $destino 'routes') | Out-Null
$s=[IO.File]::ReadAllText((Join-Path $Origem 'routes/recomendacoes.py'))
foreach($tipo in @('filmes','series')) {
    $anchor='@router.get("/'+$tipo+'/java")';$idx=$s.IndexOf($anchor)
    if($idx -lt 0){throw "Rota $tipo não encontrada"}
    $tail=$s.Substring($idx);$fim=$tail.IndexOf('@router.',1);if($fim -lt 0){$fim=$tail.Length}
    $trecho=$tail.Substring(0,$fim)
    if(!$trecho.Contains('onboarding=')) {
        $trecho=$trecho.Replace('        favoritos'+[char]13+[char]10+'    )','        favoritos,'+[char]13+[char]10+'        onboarding=dados["onboarding"]["'+$tipo+'"]'+[char]13+[char]10+'    )')
        $trecho=$trecho.Replace("        favoritos`n    )","        favoritos,`n        onboarding=dados[`"onboarding`"][`"$tipo`"]`n    )")
        $s=$s.Remove($idx,$fim).Insert($idx,$trecho)
    }
}
Gravar 'routes/recomendacoes.py' $s
