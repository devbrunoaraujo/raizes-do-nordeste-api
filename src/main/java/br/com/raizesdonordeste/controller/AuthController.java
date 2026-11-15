package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.LoginRequest;
import br.com.raizesdonordeste.dto.RegistroRequest;
import br.com.raizesdonordeste.dto.TokenResponse;
import br.com.raizesdonordeste.dto.UsuarioResponse;
import br.com.raizesdonordeste.service.AuthService;
import br.com.raizesdonordeste.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse criado = usuarioService.registrarCliente(request);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }
}