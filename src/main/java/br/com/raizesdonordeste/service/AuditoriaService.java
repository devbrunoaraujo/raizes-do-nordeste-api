package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.AuditoriaResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.entity.Auditoria;
import br.com.raizesdonordeste.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int LIMITE_MAXIMO = 100;
    private static final int TAMANHO_DETALHES = 500;

    private final AuditoriaRepository auditoriaRepository;

    /**
     * Participa da transação de quem chama: se a operação for desfeita, o registro também é.
     * Assim a trilha só contém o que de fato aconteceu.
     */
    @Transactional
    public void registrar(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        String texto = detalhes != null && detalhes.length() > TAMANHO_DETALHES
                ? detalhes.substring(0, TAMANHO_DETALHES) : detalhes;
        auditoriaRepository.save(Auditoria.builder()
                .usuarioId(usuarioId).acao(acao).entidade(entidade)
                .entidadeId(entidadeId).detalhes(texto).build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaResponse> listar(String acao, String entidade, int page, int limit) {
        var pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(limit, 1), LIMITE_MAXIMO));
        return PageResponse.de(auditoriaRepository.filtrar(acao, entidade, pageable).map(AuditoriaResponse::de));
    }
}