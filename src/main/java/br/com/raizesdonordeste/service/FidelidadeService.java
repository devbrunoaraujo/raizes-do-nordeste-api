package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.MovimentoFidelidadeResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.SaldoResponse;
import br.com.raizesdonordeste.entity.ClientePerfil;
import br.com.raizesdonordeste.repository.ClientePerfilRepository;
import br.com.raizesdonordeste.repository.FidelidadeMovimentoRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FidelidadeService {

    /** 1 ponto vale R$ 0,10 de desconto. */
    public static final BigDecimal VALOR_PONTO = new BigDecimal("0.10");
    public static final int RESGATE_MINIMO = 10;
    private static final int LIMITE_MAXIMO = 100;

    private final FidelidadeMovimentoRepository movimentoRepository;
    private final ClientePerfilRepository perfilRepository;
    private final ClienteService clienteService;

    @Transactional
    public SaldoResponse consultarSaldo(UsuarioAutenticado usuario) {
        return montarSaldo(clienteService.obterOuCriarPerfil(usuario.id()));
    }

    @Transactional
    public PageResponse<MovimentoFidelidadeResponse> historico(UsuarioAutenticado usuario, int page, int limit) {
        ClientePerfil perfil = clienteService.obterOuCriarPerfil(usuario.id());
        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO));
        return PageResponse.de(movimentoRepository
                .findByClienteIdOrderByCriadoEmDescIdDesc(perfil.getId(), pageable)
                .map(MovimentoFidelidadeResponse::de));
    }

    @Transactional
    public SaldoResponse darConsentimento(UsuarioAutenticado usuario) {
        ClientePerfil perfil = clienteService.obterOuCriarPerfil(usuario.id());
        if (!perfil.isConsentimentoFidelidade()) {
            perfil.darConsentimento();
            perfilRepository.save(perfil);
        }
        return montarSaldo(perfil);
    }

    @Transactional
    public SaldoResponse revogarConsentimento(UsuarioAutenticado usuario) {
        ClientePerfil perfil = clienteService.obterOuCriarPerfil(usuario.id());
        if (perfil.isConsentimentoFidelidade()) {
            perfil.revogarConsentimento();
            perfilRepository.save(perfil);
        }
        return montarSaldo(perfil);
    }

    private SaldoResponse montarSaldo(ClientePerfil perfil) {
        long saldo = movimentoRepository.saldo(perfil.getId());
        return new SaldoResponse(perfil.isConsentimentoFidelidade(), saldo, VALOR_PONTO, RESGATE_MINIMO);
    }
}