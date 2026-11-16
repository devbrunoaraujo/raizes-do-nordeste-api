package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.CardapioItemAtualizacaoRequest;
import br.com.raizesdonordeste.dto.CardapioItemRequest;
import br.com.raizesdonordeste.dto.CardapioItemResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.CardapioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/unidades/{unidadeId}/cardapio")
@RequiredArgsConstructor
public class CardapioController {

    private final CardapioService cardapioService;

    @GetMapping
    public ResponseEntity<PageResponse<CardapioItemResponse>> listar(
            @PathVariable Long unidadeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(cardapioService.listar(unidadeId, page, limit));
    }

    @PostMapping
    public ResponseEntity<CardapioItemResponse> adicionar(
            @PathVariable Long unidadeId,
            @Valid @RequestBody CardapioItemRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        CardapioItemResponse criado = cardapioService.adicionar(unidadeId, request, usuario);
        return ResponseEntity
                .created(URI.create("/unidades/" + unidadeId + "/cardapio/" + criado.produtoId()))
                .body(criado);
    }

    @PutMapping("/{produtoId}")
    public ResponseEntity<CardapioItemResponse> atualizar(
            @PathVariable Long unidadeId,
            @PathVariable Long produtoId,
            @Valid @RequestBody CardapioItemAtualizacaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(cardapioService.atualizar(unidadeId, produtoId, request, usuario));
    }
}