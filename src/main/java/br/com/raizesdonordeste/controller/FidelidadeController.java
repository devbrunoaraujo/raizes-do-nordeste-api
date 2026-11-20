package br.com.raizesdonordeste.controller;

import br.com.raizesdonordeste.dto.MovimentoFidelidadeResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.SaldoResponse;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import br.com.raizesdonordeste.service.FidelidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fidelidade")
@RequiredArgsConstructor
public class FidelidadeController {

    private final FidelidadeService fidelidadeService;

    @GetMapping("/saldo")
    public ResponseEntity<SaldoResponse> saldo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(fidelidadeService.consultarSaldo(usuario));
    }

    @GetMapping("/historico")
    public ResponseEntity<PageResponse<MovimentoFidelidadeResponse>> historico(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(fidelidadeService.historico(usuario, page, limit));
    }

    @PostMapping("/consentimento")
    public ResponseEntity<SaldoResponse> darConsentimento(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(fidelidadeService.darConsentimento(usuario));
    }

    @DeleteMapping("/consentimento")
    public ResponseEntity<SaldoResponse> revogarConsentimento(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(fidelidadeService.revogarConsentimento(usuario));
    }
}