package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.UnidadeRequest;
import br.com.raizesdonordeste.dto.UnidadeResponse;
import br.com.raizesdonordeste.entity.Unidade;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private static final int LIMITE_MAXIMO = 100;

    private final UnidadeRepository unidadeRepository;

    @Transactional(readOnly = true)
    public PageResponse<UnidadeResponse> listar(int page, int limit) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO),
                Sort.by("nome"));
        return PageResponse.de(unidadeRepository.findByAtivaTrue(pageable).map(UnidadeResponse::de));
    }

    @Transactional(readOnly = true)
    public UnidadeResponse buscarPorId(Long id) {
        return UnidadeResponse.de(buscarEntidade(id));
    }

    @Transactional
    public UnidadeResponse criar(UnidadeRequest request) {
        Unidade unidade = new Unidade();
        aplicar(unidade, request);
        return UnidadeResponse.de(unidadeRepository.save(unidade));
    }

    @Transactional
    public UnidadeResponse atualizar(Long id, UnidadeRequest request) {
        Unidade unidade = buscarEntidade(id);
        aplicar(unidade, request);
        return UnidadeResponse.de(unidadeRepository.save(unidade));
    }

    /** Soft delete: pedidos e estoque referenciam a unidade, então ela é desativada, não apagada. */
    @Transactional
    public void desativar(Long id) {
        Unidade unidade = buscarEntidade(id);
        unidade.setAtiva(false);
        unidadeRepository.save(unidade);
    }

    /** Usado por outros services (estoque, pedido) que precisam da entidade. */
    @Transactional(readOnly = true)
    public Unidade buscarEntidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", id));
    }

    private void aplicar(Unidade unidade, UnidadeRequest request) {
        unidade.setNome(request.nome());
        unidade.setCidade(request.cidade());
        unidade.setUf(request.uf().toUpperCase());
    }
}