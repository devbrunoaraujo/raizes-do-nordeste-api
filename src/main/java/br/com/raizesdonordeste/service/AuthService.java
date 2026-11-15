package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.LoginRequest;
import br.com.raizesdonordeste.dto.TokenResponse;
import br.com.raizesdonordeste.entity.Usuario;
import br.com.raizesdonordeste.exception.CredenciaisInvalidasException;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import br.com.raizesdonordeste.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);

        return new TokenResponse(
                jwtService.gerarToken(usuario),
                "Bearer",
                jwtService.getExpiracaoSegundos(),
                usuario.getRole());
    }
}