package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.PedidoRequest;
import br.com.raizesdonordeste.dto.PedidoResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

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
}