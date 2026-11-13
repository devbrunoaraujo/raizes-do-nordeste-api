package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.ProdutoRequest;
import br.com.raizesdonordeste.dto.ProdutoResponse;
import br.com.raizesdonordeste.entity.Produto;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private static final int LIMITE_MAXIMO = 100;

    private final ProdutoRepository produtoRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProdutoResponse> listar(int page, int limit) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO),
                Sort.by("nome"));
        return PageResponse.de(produtoRepository.findByAtivoTrue(pageable).map(ProdutoResponse::de));
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return ProdutoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto();
        aplicar(produto, request);
        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        aplicar(produto, request);
        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    @Transactional
    public void desativar(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    private void aplicar(Produto produto, ProdutoRequest request) {
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setCategoria(request.categoria());
        produto.setPreco(request.preco());
    }
}