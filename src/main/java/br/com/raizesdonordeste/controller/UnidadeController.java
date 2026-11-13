package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.UnidadeRequest;
import br.com.raizesdonordeste.dto.UnidadeResponse;
import br.com.raizesdonordeste.service.UnidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @GetMapping
    public ResponseEntity<PageResponse<UnidadeResponse>> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(unidadeService.listar(page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadeService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody UnidadeRequest request) {
        UnidadeResponse criada = unidadeService.criar(request);
        return ResponseEntity.created(URI.create("/unidades/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadeResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody UnidadeRequest request) {
        return ResponseEntity.ok(unidadeService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        unidadeService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}