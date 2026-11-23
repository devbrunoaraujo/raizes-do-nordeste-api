# Requisitos: Rede Raízes do Nordeste (Back-end)

## 1. Escopo

API REST que atende múltiplos canais (APP, TOTEM, BALCÃO, PICKUP e WEB) com foco em
regras de negócio, persistência, integrações e segurança. A interface gráfica está fora do escopo.

**Fluxo obrigatório entregue (Fluxo A):** criar pedido → validar itens e estoque →
solicitar pagamento ao gateway simulado → atualizar o status do pedido.

**Perfis (atores):** CLIENTE (App, Web e Totem), ATENDENTE (balcão), COZINHA,
GERENTE, ADMIN e o Sistema Externo de Pagamento (gateway, simulado por mock).

## 2. Requisitos funcionais

| ID | Requisito | Prioridade | Endpoints | Situação |
|---|---|---|---|---|
| RF01 | Cadastro público de cliente | Essencial | `POST /auth/register` | Implementado |
| RF02 | Autenticação por login, com token JWT | Essencial | `POST /auth/login` | Implementado |
| RF03 | Cadastro de usuários internos por ADMIN, com perfil e unidade | Essencial | `POST /usuarios`, `GET /usuarios/{id}` | Implementado |
| RF04 | Autorização por perfil e por escopo de unidade | Essencial | Transversal | Implementado |
| RF05 | Gestão de unidades da rede | Essencial | `/unidades` | Implementado |
| RF06 | Gestão do catálogo de produtos | Essencial | `/produtos` | Implementado |
| RF07 | Cardápio por unidade: consulta pública e gestão por gerente/admin | Essencial | `/unidades/{id}/cardapio` | Implementado |
| RF08 | Controle de estoque por unidade: entrada, saída, histórico e bloqueio de venda sem saldo | Essencial | `/unidades/{id}/estoque/**` | Implementado |
| RF09 | Criação de pedido com `canalPedido` obrigatório, itens, preço congelado e idempotência | Essencial | `POST /pedidos` | Implementado |
| RF10 | Consulta e filtro de pedidos por canal, status e unidade, com paginação | Essencial | `GET /pedidos`, `GET /pedidos/{id}` | Implementado |
| RF11 | Atualização de status: cozinha → pronto → entregue | Essencial | `PATCH /pedidos/{id}/status` | Implementado |
| RF12 | Cancelamento de pedido com devolução de estoque e estorno | Essencial | `POST /pedidos/{id}/cancelamento` | Implementado |
| RF13 | Solicitação de pagamento ao gateway externo (mock), com registro e retorno de status e payload | Essencial | `POST/GET /pedidos/{id}/pagamentos` | Implementado |
| RF14 | Fidelização: consentimento, saldo, histórico, acúmulo e resgate de pontos | Importante | `/fidelidade/**` | Implementado |
| RF15 | Perfil do cliente e direito de anonimização (LGPD) | Importante | `/clientes/me` | Implementado |
| RF16 | Promoções e campanhas | Conceitual | — | Parcial: o resgate de pontos como desconto está implementado; as campanhas segmentadas estão apenas documentadas (seção 4) |
| RF17 | Auditoria de operações sensíveis | Importante | — | **Não implementado** (seção 5) |
| RF18 | Refresh e logout de token | Opcional | — | Não implementado (seção 5) |

## 3. Requisitos não funcionais

| ID | Requisito | Como é atendido |
|---|---|---|
| RNF01 | Senha com hash | BCrypt (fator de custo 10, com salt embutido). Nunca é registrada em log nem devolvida |
| RNF02 | Autenticação por token | JWT assinado (HS256) com expiração. A API é *stateless*, sem sessão |
| RNF03 | Controle de acesso | Regras por rota (RBAC) e por escopo: funcionário só atua na própria unidade, cliente só vê os próprios dados. A política é *negar por padrão* |
| RNF04 | Perfil e unidade sempre atuais | O filtro JWT recarrega o usuário do banco a cada requisição. Desativar ou rebaixar alguém tem efeito imediato, mesmo com token válido |
| RNF05 | LGPD | Consentimento com data, minimização de dados, DTOs sem dados sensíveis e anonimização (ver documento de LGPD) |
| RNF06 | Desempenho em horário de pico | API sem estado (escala horizontalmente), índices em pedido, paginação com limite máximo de 100 e consultas filtradas no banco |
| RNF07 | Consistência sob concorrência | Transações; bloqueio otimista (`@Version`) no estoque; bloqueio pessimista em pedido e perfil de fidelidade; `UNIQUE` na chave de idempotência |
| RNF08 | Disponibilidade e tolerância a falhas | Falha ou timeout do gateway vira o pagamento `ERRO` (HTTP 503). O pedido continua pendente e permite nova tentativa |
| RNF09 | Integração desacoplada | Gateway atrás da interface `PagamentoGateway`. Trocar o mock por um provedor real não afeta o domínio |
| RNF10 | Erros padronizados | Todo erro, inclusive 401 e 403, segue o mesmo JSON (`ErroResponse`) |
| RNF11 | Manutenibilidade | Camadas com responsabilidades separadas e regras de negócio nas entidades |
| RNF12 | Documentação | OpenAPI/Swagger, README, coleção Postman e documentos de modelagem |
| RNF13 | Logs e auditoria | **Parcial:** logs de aplicação e histórico de movimentações (estoque, pagamento, pontos). Auditoria dedicada não implementada |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | `canalPedido` é obrigatório (APP, TOTEM, BALCAO, PICKUP, WEB). Ausente: 422. Valor inexistente: 400 |
| RN02 | Só se vende produto ativo, presente no cardápio da unidade e marcado como disponível |
| RN03 | Não se vende acima do saldo. Sem saldo: 409 `ESTOQUE_INSUFICIENTE`, e nada é gravado (transação) |
| RN04 | O preço do item é congelado no pedido: preço local da unidade, senão o do catálogo |
| RN05 | Linhas repetidas do mesmo produto são consolidadas em uma só |
| RN06 | Idempotência: a mesma `Idempotency-Key` devolve o pedido original (200), sem nova baixa de estoque. A chave só vale para quem a criou |
| RN07 | Cliente só compra para si. Funcionário pode informar o cliente ou vender sem identificação |
| RN08 | Funcionário só atua na própria unidade. ADMIN atua em todas |
| RN09 | Status do pedido: AGUARDANDO_PAGAMENTO → PAGO → EM_PREPARO → PRONTO → ENTREGUE. CANCELADO só até EM_PREPARO |
| RN10 | PAGO só pelo pagamento, e CANCELADO só pelo cancelamento. O `PATCH /status` não os aceita |
| RN11 | Quem muda o status: COZINHA (EM_PREPARO, PRONTO), ATENDENTE (ENTREGUE), GERENTE e ADMIN (todos) |
| RN12 | Pagamento só em pedido AGUARDANDO_PAGAMENTO. Pagamento recusado não cancela o pedido |
| RN13 | Cancelamento devolve o estoque, estorna o pagamento aprovado e estorna os pontos. O cliente não cancela pedido em preparo |
| RN14 | Acúmulo: 1 ponto por R$ 1,00 do total pago (parte inteira), apenas para quem consentiu |
| RN15 | Resgate: 1 ponto vale R$ 0,10, mínimo de 10 pontos, uma vez por pedido pendente, limitado ao subtotal |
| RN16 | Revogar o consentimento interrompe o acúmulo e o resgate. O histórico é preservado |
| RN17 | Campanhas (documentadas, não implementadas): descontos progressivos por faixa de pontos e ações segmentadas por frequência e idade, apenas para quem consentiu |

## 5. Itens não implementados e justificativas

| Item | Justificativa |
|---|---|
| Auditoria dedicada (RF17) | Priorizamos o Fluxo A completo, a segurança e a LGPD dentro do prazo. A rastreabilidade parcial existe nas tabelas de movimentação (estoque com usuário e pedido, pagamentos com referência externa, extrato de pontos). Evolução prevista: tabela `auditoria` com serviço chamado nos pontos sensíveis |
| Refresh e logout (RF18) | Opcionais no roteiro. O token expira em 1 hora, e desativar o usuário o invalida de imediato (RNF04) |
| Campanhas segmentadas | Documentadas como regra (RN17), conforme o roteiro permite |
| Estorno no gateway | O estorno é registrado localmente (`ESTORNADO`). Um provedor real exigiria um método `estornar` na interface do gateway |
| Comparação do corpo na idempotência | Repetir a chave com itens diferentes devolve o pedido original. Provedores reais costumam rejeitar a divergência |