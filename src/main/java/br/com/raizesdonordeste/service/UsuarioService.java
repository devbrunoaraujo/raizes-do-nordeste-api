package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.RegistroRequest;
import br.com.raizesdonordeste.dto.UsuarioResponse;
import br.com.raizesdonordeste.entity.Usuario;
import br.com.raizesdonordeste.enums.Role;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Cadastro público: sempre cria CLIENTE. Perfis de funcionário são criados por um ADMIN. */
    @Transactional
    public UsuarioResponse registrarCliente(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senhaHash(passwordEncoder.encode(request.senha()))
                .role(Role.CLIENTE)
                .build();

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }
}