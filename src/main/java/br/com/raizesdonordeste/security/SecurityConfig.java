package br.com.raizesdonordeste.security;

import br.com.raizesdonordeste.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/unidades/**", "/produtos/**").permitAll()
                        .requestMatchers("/unidades/*/cardapio/**").hasAnyRole("GERENTE", "ADMIN")
                        .requestMatchers("/unidades/**").hasRole("ADMIN")
                        .requestMatchers("/produtos/**").hasAnyRole("GERENTE", "ADMIN")
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Erros de segurança passam pelo GlobalExceptionHandler: mesmo JSON padrão da API
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> resolver.resolveException(req, res, null, ex))
                        .accessDeniedHandler((req, res, ex) -> resolver.resolveException(req, res, null, ex)))
                .addFilterBefore(new JwtAuthFilter(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Evita o usuário/senha gerados automaticamente: nossa autenticação é feita via JWT. */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticação é feita via JWT");
        };
    }
}