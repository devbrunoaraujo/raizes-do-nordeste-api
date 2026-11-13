package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.ProdutoRequest;
import br.com.raizesdonordeste.dto.ProdutoResponse;
import br.com.raizesdonordeste.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<PageResponse<ProdutoResponse>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(produtoService.listar(page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        ProdutoResponse criado = produtoService.criar(request);
        return ResponseEntity.created(URI.create("/produtos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        produtoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}