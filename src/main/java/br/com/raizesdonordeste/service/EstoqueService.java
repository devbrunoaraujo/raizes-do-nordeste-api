package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.EstoqueResponse;
import br.com.raizesdonordeste.dto.MovimentacaoRequest;
import br.com.raizesdonordeste.dto.MovimentacaoResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.entity.Estoque;
import br.com.raizesdonordeste.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.entity.Produto;
import br.com.raizesdonordeste.entity.Unidade;
import br.com.raizesdonordeste.enums.TipoMovimentacao;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.CardapioUnidadeRepository;
import br.com.raizesdonordeste.repository.EstoqueRepository;
import br.com.raizesdonordeste.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private static final int LIMITE_MAXIMO = 100;

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final CardapioUnidadeRepository cardapioRepository;
    private final UnidadeService unidadeService;
    private final ProdutoService produtoService;

    @Transactional(readOnly = true)
    public PageResponse<EstoqueResponse> listar(Long unidadeId, int page, int limit,
                                                UsuarioAutenticado usuario) {
        usuario.exigirAcessoAUnidade(unidadeId);
        unidadeService.buscarEntidade(unidadeId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO),
                Sort.by("produto.nome"));
        return PageResponse.de(estoqueRepository.findByUnidadeId(unidadeId, pageable).map(EstoqueResponse::de));
    }

    @Transactional(readOnly = true)
    public EstoqueResponse consultar(Long unidadeId, Long produtoId, UsuarioAutenticado usuario) {
        usuario.exigirAcessoAUnidade(unidadeId);
        return EstoqueResponse.de(buscarEstoque(unidadeId, produtoId));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimentacaoResponse> historico(Long unidadeId, Long produtoId, int page, int limit,
                                                        UsuarioAutenticado usuario) {
        usuario.exigirAcessoAUnidade(unidadeId);
        Estoque estoque = buscarEstoque(unidadeId, produtoId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO));
        return PageResponse.de(movimentacaoRepository
                .findByEstoqueIdOrderByCriadoEmDescIdDesc(estoque.getId(), pageable)
                .map(MovimentacaoResponse::de));
    }

    @Transactional
    public MovimentacaoResponse movimentar(Long unidadeId, MovimentacaoRequest request,
                                           UsuarioAutenticado usuario) {
        usuario.exigirAcessoAUnidade(unidadeId);

        Unidade unidade = unidadeService.buscarEntidade(unidadeId);
        Produto produto = produtoService.buscarEntidade(request.produtoId());

        if (!cardapioRepository.existsByUnidadeIdAndProdutoId(unidadeId, produto.getId())) {
            throw new NegocioException("PRODUTO_FORA_DO_CARDAPIO",
                    "O produto não faz parte do cardápio desta unidade");
        }

        // Primeira entrada de um produto cria o registro de estoque com saldo zero
        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                .orElseGet(() -> Estoque.builder().unidade(unidade).produto(produto).quantidade(0).build());

        if (request.tipo() == TipoMovimentacao.ENTRADA) {
            estoque.repor(request.quantidade());
        } else {
            estoque.baixar(request.quantidade()); // lança 409 se faltar saldo
        }
        estoque = estoqueRepository.save(estoque);

        MovimentacaoEstoque movimentacao = movimentacaoRepository.save(MovimentacaoEstoque.builder()
                .estoque(estoque)
                .tipo(request.tipo())
                .quantidade(request.quantidade())
                .saldoApos(estoque.getQuantidade())
                .motivo(request.motivo())
                .usuarioId(usuario.id())
                .build());

        return MovimentacaoResponse.de(movimentacao);
    }

    private Estoque buscarEstoque(Long unidadeId, Long produtoId) {
        unidadeService.buscarEntidade(unidadeId);
        return estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque do produto", produtoId));
    }
}