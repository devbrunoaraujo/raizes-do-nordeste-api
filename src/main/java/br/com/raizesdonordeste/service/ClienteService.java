package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.ClienteRequest;
import br.com.raizesdonordeste.dto.ClienteResponse;
import br.com.raizesdonordeste.entity.ClientePerfil;
import br.com.raizesdonordeste.repository.ClientePerfilRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClientePerfilRepository perfilRepository;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService; // AUDITORIA

    /** Cria o perfil no primeiro acesso: clientes cadastrados antes deste bloco também funcionam. */
    @Transactional
    public ClientePerfil obterOuCriarPerfil(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> perfilRepository.save(ClientePerfil.builder()
                        .usuario(usuarioService.buscarEntidade(usuarioId))
                        .build()));
    }

    @Transactional
    public ClienteResponse consultar(UsuarioAutenticado usuario) {
        return ClienteResponse.de(obterOuCriarPerfil(usuario.id()));
    }

    @Transactional
    public ClienteResponse atualizar(UsuarioAutenticado usuario, ClienteRequest request) {
        ClientePerfil perfil = obterOuCriarPerfil(usuario.id());
        perfil.setDataNascimento(request.dataNascimento());
        return ClienteResponse.de(perfilRepository.save(perfil));
    }

    @Transactional
    public void anonimizar(UsuarioAutenticado usuario) {
        ClientePerfil perfil = obterOuCriarPerfil(usuario.id());
        perfil.anonimizar();
        perfilRepository.save(perfil);
        auditoriaService.registrar(usuario.id(), "CLIENTE_ANONIMIZADO", "Usuario", usuario.id(), null); // AUDITORIA
        usuarioService.anonimizar(usuario.id());
    }
}