package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.RegistroRequest;
import br.com.raizesdonordeste.dto.UsuarioRequest;
import br.com.raizesdonordeste.dto.UsuarioResponse;
import br.com.raizesdonordeste.entity.Unidade;
import br.com.raizesdonordeste.entity.Usuario;
import br.com.raizesdonordeste.enums.Role;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UnidadeService unidadeService;
    private final PasswordEncoder passwordEncoder;

    /** Cadastro público: sempre cria CLIENTE. */
    @Transactional
    public UsuarioResponse registrarCliente(RegistroRequest request) {
        return UsuarioResponse.de(salvar(request.nome(), request.email(), request.senha(), Role.CLIENTE, null));
    }

    /** Cadastro feito por um ADMIN: qualquer perfil; funcionários ficam vinculados a uma unidade. */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        Unidade unidade = request.role().exigeUnidade()
                ? unidadeService.buscarEntidade(request.unidadeId())
                : null;
        return UsuarioResponse.de(salvar(request.nome(), request.email(), request.senha(), request.role(), unidade));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private Usuario salvar(String nome, String emailBruto, String senha, Role role, Unidade unidade) {
        String email = emailBruto.trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail");
        }

        return usuarioRepository.save(Usuario.builder()
                .nome(nome.trim())
                .email(email)
                .senhaHash(passwordEncoder.encode(senha))
                .role(role)
                .unidade(unidade)
                .build());
    }
}