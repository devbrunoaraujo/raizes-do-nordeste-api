package br.com.raizesdonordeste.security;

import br.com.raizesdonordeste.entity.Usuario;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lê o token do cabeçalho Authorization e, se válido, identifica o usuário na requisição. */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIXO)) {
            jwtService.extrairEmail(header.substring(PREFIXO.length()))
                    .flatMap(usuarioRepository::findByEmail)
                    .filter(Usuario::isAtivo)
                    .ifPresent(this::autenticar);
        }

        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario) {
        Long unidadeId = usuario.getUnidade() != null ? usuario.getUnidade().getId() : null;
        UsuarioAutenticado principal =
                new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRole(), unidadeId);
        var autenticacao = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}