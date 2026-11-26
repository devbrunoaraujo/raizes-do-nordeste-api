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
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UnidadeService unidadeService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService; // AUDITORIA

    /** Cadastro público: sempre cria CLIENTE. */
    @Transactional
    public UsuarioResponse registrarCliente(RegistroRequest request) {
        return UsuarioResponse.de(salvar(request.nome(), request.email(), request.senha(), Role.CLIENTE, null));
    }

    /** Cadastro feito por um ADMIN: qualquer perfil; funcionários ficam vinculados a uma unidade. */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request, UsuarioAutenticado admin) { // AUDITORIA: recebe o admin
        Unidade unidade = request.role().exigeUnidade()
                ? unidadeService.buscarEntidade(request.unidadeId())
                : null;
        Usuario criado = salvar(request.nome(), request.email(), request.senha(), request.role(), unidade);
        auditoriaService.registrar(admin.id(), "USUARIO_CRIADO", "Usuario", criado.getId(),
                "role=" + criado.getRole()); // AUDITORIA
        return UsuarioResponse.de(criado);
    }

    /** Não é mais readOnly: a consulta de dados de terceiros por um ADMIN é auditada. */
    @Transactional
    public UsuarioResponse buscarPorId(Long id, UsuarioAutenticado admin) { // AUDITORIA: recebe o admin
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
        auditoriaService.registrar(admin.id(), "USUARIO_CONSULTADO", "Usuario", id, null); // AUDITORIA
        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    /** Direito de eliminação (LGPD): remove os dados pessoais e impede novo acesso à conta. */
    @Transactional
    public void anonimizar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));

        usuario.setNome("Cliente anonimizado");
        usuario.setEmail("anonimizado-" + usuario.getId() + "@anonimizado.invalid");
        usuario.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
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