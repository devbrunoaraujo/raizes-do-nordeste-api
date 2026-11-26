package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.UsuarioRequest;
import br.com.raizesdonordeste.dto.UsuarioResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(
            @Valid @RequestBody UsuarioRequest request,
            @AuthenticationPrincipal UsuarioAutenticado admin) { // AUDITORIA
        UsuarioResponse criado = usuarioService.criar(request, admin);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado admin) { // AUDITORIA
        return ResponseEntity.ok(usuarioService.buscarPorId(id, admin));
    }
}