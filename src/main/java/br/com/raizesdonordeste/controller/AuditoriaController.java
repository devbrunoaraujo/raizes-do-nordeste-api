package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.AuditoriaResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<PageResponse<AuditoriaResponse>> listar(
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String entidade,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditoriaService.listar(acao, entidade, page, limit));
    }
}