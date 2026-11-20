package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.PagamentoRequest;
import br.com.raizesdonordeste.dto.PagamentoResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos/{pedidoId}/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    public ResponseEntity<PagamentoResponse> pagar(
            @PathVariable Long pedidoId,
            @RequestBody(required = false) PagamentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        PagamentoResponse resposta = pagamentoService.pagar(pedidoId, request, usuario);

        HttpStatus status = switch (resposta.status()) {
            case APROVADO -> HttpStatus.CREATED;               // 201
            case RECUSADO -> HttpStatus.PAYMENT_REQUIRED;      // 402
            case ERRO -> HttpStatus.SERVICE_UNAVAILABLE;       // 503
            default -> HttpStatus.CREATED;
        };
        return ResponseEntity.status(status).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<PagamentoResponse>> listar(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(pagamentoService.listar(pedidoId, usuario));
    }
}