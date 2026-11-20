package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.PedidoRequest;
import br.com.raizesdonordeste.dto.PedidoResponse;
import br.com.raizesdonordeste.dto.StatusRequest;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.CancelamentoService;
import br.com.raizesdonordeste.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.enums.CanalPedido;
import br.com.raizesdonordeste.enums.StatusPedido;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final CancelamentoService cancelamentoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody PedidoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        PedidoService.Resultado resultado = pedidoService.criar(request, idempotencyKey, usuario);

        if (resultado.criado()) {
            return ResponseEntity
                    .created(URI.create("/pedidos/" + resultado.pedido().id()))
                    .body(resultado.pedido());
        }
        return ResponseEntity.ok(resultado.pedido()); // repetição idempotente
    }

    @GetMapping
    public ResponseEntity<PageResponse<PedidoResponse>> listar(
            @RequestParam(required = false) CanalPedido canalPedido,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Long unidadeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(
                pedidoService.listar(canalPedido, status, unidadeId, page, limit, usuario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id, usuario));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(pedidoService.atualizarStatus(id, request.status(), usuario));
    }

    @PostMapping("/{id}/cancelamento")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(cancelamentoService.cancelar(id, usuario));
    }
}