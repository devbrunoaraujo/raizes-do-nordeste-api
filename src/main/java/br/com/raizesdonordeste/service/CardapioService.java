package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.CardapioItemAtualizacaoRequest;
import br.com.raizesdonordeste.dto.CardapioItemRequest;
import br.com.raizesdonordeste.dto.CardapioItemResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.entity.CardapioUnidade;
import br.com.raizesdonordeste.entity.Produto;
import br.com.raizesdonordeste.entity.Unidade;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.CardapioUnidadeRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardapioService {

    private static final int LIMITE_MAXIMO = 100;

    private final CardapioUnidadeRepository cardapioRepository;
    private final UnidadeService unidadeService;
    private final ProdutoService produtoService;

    /** Consulta pública: só itens disponíveis de produtos ativos. */
    @Transactional(readOnly = true)
    public PageResponse<CardapioItemResponse> listar(Long unidadeId, int page, int limit) {
        unidadeService.buscarEntidade(unidadeId); // 404 se a unidade não existe

        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO),
                Sort.by("produto.nome"));

        return PageResponse.de(cardapioRepository
                .findByUnidadeIdAndDisponivelTrueAndProdutoAtivoTrue(unidadeId, pageable)
                .map(CardapioItemResponse::de));
    }

    @Transactional
    public CardapioItemResponse adicionar(Long unidadeId, CardapioItemRequest request,
                                          UsuarioAutenticado usuario) {
        usuario.exigirAcessoAUnidade(unidadeId);

        Unidade unidade = unidadeService.buscarEntidade(unidadeId);
        Produto produto = produtoService.buscarEntidade(request.produtoId());

        if (!produto.isAtivo()) {
            throw new NegocioException("PRODUTO_INATIVO", "Produto inativo não pode entrar no cardápio");
        }
        if (cardapioRepository.existsByUnidadeIdAndProdutoId(unidadeId, produto.getId())) {
            throw new NegocioException("PRODUTO_JA_NO_CARDAPIO", "Este produto já está no cardápio da unidade");
        }

        CardapioUnidade item = CardapioUnidade.builder()
                .unidade(unidade)
                .produto(produto)
                .precoLocal(request.precoLocal())
                .build();
        return CardapioItemResponse.de(cardapioRepository.save(item));
    }

    @Transactional
    public CardapioItemResponse atualizar(Long unidadeId, Long produtoId,
                                          CardapioItemAtualizacaoRequest request,
                                          UsuarioAutenticado usuario) {
        usuario.exigirAcessoAUnidade(unidadeId);

        CardapioUnidade item = cardapioRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto no cardápio", produtoId));

        item.setPrecoLocal(request.precoLocal());
        item.setDisponivel(request.disponivel());
        return CardapioItemResponse.de(cardapioRepository.save(item));
    }

    /** Item que pode ser vendido agora: está no cardápio, disponível e o produto está ativo. */
    @Transactional(readOnly = true)
    public CardapioUnidade buscarItemVendavel(Long unidadeId, Produto produto) {
        return cardapioRepository.findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                .filter(c -> c.isDisponivel() && produto.isAtivo())
                .orElseThrow(() -> new NegocioException("PRODUTO_INDISPONIVEL",
                        "O produto " + produto.getId() + " não está disponível nesta unidade"));
    }
}