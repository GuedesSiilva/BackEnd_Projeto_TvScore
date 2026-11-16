package br.com.senai.tv_score.controller;
import br.com.senai.tv_score.filme.NavegacaoCatalogoService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/conteudos")
public class NavegacaoCatalogoController {
    private final NavegacaoCatalogoService service;
    public NavegacaoCatalogoController(NavegacaoCatalogoService service){this.service=service;}
    @io.swagger.v3.oas.annotations.Operation(summary="Pesquisa de títulos por cursor",description="Sem total exato. temMais=true confirma lookahead; null indica orçamento esgotado. podeContinuar permite retomar. Cursor local, TTL 15 min, vinculado à consulta/idioma/tamanho. Rotas antigas mantêm totais upstream incluindo pessoas.")
    @GetMapping("/pesquisa") public NavegacaoCatalogoService.Pagina pesquisa(@RequestParam String q,
            @RequestParam(defaultValue="20") int tamanho,@RequestParam(required=false) String cursor){return service.pesquisar(q,tamanho,cursor);}
    @io.swagger.v3.oas.annotations.Operation(summary="Merge incremental por popularidade",description="MOVIE/TV com posições independentes. Sem snapshot global. Popularidade ausente fica no fim. Limite upstream 500 páginas por tipo; truncado sinaliza corte. Onboarding legado permanece na rota anterior.")
    @GetMapping("/por-genero/ordenados") public NavegacaoCatalogoService.Pagina genero(@RequestParam String genero,
            @RequestParam(defaultValue="20") int tamanho,@RequestParam(required=false) String cursor){return service.genero(genero,tamanho,cursor);}
}
