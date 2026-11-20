package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.PedidoResponse;
import br.com.raizesdonordeste.entity.ItemPedido;
import br.com.raizesdonordeste.entity.Pedido;
import br.com.raizesdonordeste.enums.Role;
import br.com.raizesdonordeste.enums.StatusPedido;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso que orquestra pedido, estoque e pagamento. Por isso fica fora dos três services. */
@Service
@RequiredArgsConstructor
public class CancelamentoService {

    private final PedidoService pedidoService;
    private final EstoqueService estoqueService;
    private final PagamentoService pagamentoService;

    @Transactional
    public PedidoResponse cancelar(Long pedidoId, UsuarioAutenticado usuario) {
        Pedido pedido = pedidoService.buscarParaAtualizar(pedidoId, usuario);

        if (usuario.role() == Role.CLIENTE && pedido.getStatus() == StatusPedido.EM_PREPARO) {
            throw new NegocioException("CANCELAMENTO_NAO_PERMITIDO",
                    "O pedido já está em preparo. Procure o atendimento da unidade");
        }

        pedido.avancarPara(StatusPedido.CANCELADO); // 409 se já estiver pronto, entregue ou cancelado

        for (ItemPedido item : pedido.getItens()) {
            estoqueService.devolverParaPedido(pedido.getUnidade().getId(), item.getProduto(),
                    item.getQuantidade(), pedido.getId(), usuario.id());
        }
        pagamentoService.estornarAprovados(pedido.getId());

        return PedidoResponse.de(pedido);
    }
}