package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.ClienteRequest;
import br.com.raizesdonordeste.dto.ClienteResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes/me")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<ClienteResponse> consultar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(clienteService.consultar(usuario));
    }

    @PutMapping
    public ResponseEntity<ClienteResponse> atualizar(
            @Valid @RequestBody ClienteRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(clienteService.atualizar(usuario, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> anonimizar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        clienteService.anonimizar(usuario);
        return ResponseEntity.noContent().build();
    }
}