package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.EstoqueResponse;
import br.com.raizesdonordeste.dto.MovimentacaoRequest;
import br.com.raizesdonordeste.dto.MovimentacaoResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/unidades/{unidadeId}/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    public ResponseEntity<PageResponse<EstoqueResponse>> listar(
            @PathVariable Long unidadeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(estoqueService.listar(unidadeId, page, limit, usuario));
    }

    @GetMapping("/{produtoId}")
    public ResponseEntity<EstoqueResponse> consultar(
            @PathVariable Long unidadeId,
            @PathVariable Long produtoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(estoqueService.consultar(unidadeId, produtoId, usuario));
    }

    @GetMapping("/{produtoId}/movimentacoes")
    public ResponseEntity<PageResponse<MovimentacaoResponse>> historico(
            @PathVariable Long unidadeId,
            @PathVariable Long produtoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(estoqueService.historico(unidadeId, produtoId, page, limit, usuario));
    }

    @PostMapping("/movimentacoes")
    public ResponseEntity<MovimentacaoResponse> movimentar(
            @PathVariable Long unidadeId,
            @Valid @RequestBody MovimentacaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estoqueService.movimentar(unidadeId, request, usuario));
    }
}