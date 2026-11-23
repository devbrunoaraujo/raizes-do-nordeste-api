# Modelo de dados (DER)

Banco: MySQL 8. As tabelas são geradas pelo Hibernate a partir das entidades (`ddl-auto=update`).

```mermaid
erDiagram
    UNIDADE |o--o{ USUARIO : "equipe"
    USUARIO ||--o| CLIENTE_PERFIL : "possui"
    UNIDADE ||--o{ CARDAPIO_UNIDADE : "oferece"
    PRODUTO ||--o{ CARDAPIO_UNIDADE : "compõe"
    UNIDADE ||--o{ ESTOQUE : "mantém"
    PRODUTO ||--o{ ESTOQUE : "é estocado"
    ESTOQUE ||--o{ MOVIMENTACAO_ESTOQUE : "registra"
    USUARIO |o--o{ PEDIDO : "cliente"
    UNIDADE ||--o{ PEDIDO : "recebe"
    PEDIDO ||--|{ ITEM_PEDIDO : "contém"
    PRODUTO ||--o{ ITEM_PEDIDO : "vendido em"
    PEDIDO ||--o{ PAGAMENTO : "tentativas"
    CLIENTE_PERFIL ||--o{ FIDELIDADE_MOVIMENTO : "extrato"

    UNIDADE {
        bigint id PK
        varchar nome
        varchar cidade
        char uf
        boolean ativa
    }
    USUARIO {
        bigint id PK
        varchar nome
        varchar email UK
        varchar senha_hash
        enum role "CLIENTE, ATENDENTE, COZINHA, GERENTE, ADMIN"
        boolean ativo
        bigint unidade_id FK "só funcionários de loja"
        datetime criado_em
    }
    CLIENTE_PERFIL {
        bigint id PK
        bigint usuario_id FK, UK
        date data_nascimento
        boolean consentimento_fidelidade
        datetime consentimento_em
        datetime consentimento_revogado_em
        datetime anonimizado_em
    }
    PRODUTO {
        bigint id PK
        varchar nome
        varchar descricao
        varchar categoria
        decimal preco
        boolean ativo
    }
    CARDAPIO_UNIDADE {
        bigint id PK
        bigint unidade_id FK
        bigint produto_id FK
        decimal preco_local "opcional"
        boolean disponivel
    }
    ESTOQUE {
        bigint id PK
        bigint unidade_id FK
        bigint produto_id FK
        int quantidade "CHECK nunca negativa, via regra de domínio"
        bigint versao "bloqueio otimista"
    }
    MOVIMENTACAO_ESTOQUE {
        bigint id PK
        bigint estoque_id FK
        enum tipo "ENTRADA, SAIDA"
        int quantidade
        int saldo_apos
        varchar motivo
        bigint pedido_id "referência lógica"
        bigint usuario_id "referência lógica"
        datetime criado_em
    }
    PEDIDO {
        bigint id PK
        bigint cliente_id FK "nulo = venda anônima"
        bigint unidade_id FK
        enum canal_pedido "APP, TOTEM, BALCAO, PICKUP, WEB"
        enum forma_pagamento "MOCK, PIX, CARTAO"
        enum status "AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO"
        decimal subtotal
        decimal desconto
        int pontos_resgatados
        decimal total
        varchar idempotency_key UK
        bigint criado_por_id "referência lógica"
        datetime criado_em
        datetime atualizado_em
    }
    ITEM_PEDIDO {
        bigint id PK
        bigint pedido_id FK
        bigint produto_id FK
        int quantidade
        decimal preco_unitario "preço congelado"
    }
    PAGAMENTO {
        bigint id PK
        bigint pedido_id FK
        enum forma
        enum status "PENDENTE, APROVADO, RECUSADO, ERRO, ESTORNADO"
        decimal valor
        varchar referencia_externa
        varchar mensagem
        varchar payload_resposta "JSON do gateway"
        datetime criado_em
    }
    FIDELIDADE_MOVIMENTO {
        bigint id PK
        bigint cliente_id FK
        bigint pedido_id "referência lógica"
        enum tipo "ACUMULO, RESGATE, ESTORNO"
        int pontos "com sinal"
        varchar descricao
        datetime criado_em
    }
```

## Restrições relevantes

| Restrição | Onde | Motivo |
|---|---|---|
| `UNIQUE (email)` | usuario | Um e-mail, uma conta |
| `UNIQUE (unidade_id, produto_id)` | cardapio_unidade e estoque | Um produto aparece uma vez por unidade |
| `UNIQUE (idempotency_key)` | pedido | Barreira final contra pedido duplicado |
| `UNIQUE (usuario_id)` | cliente_perfil | Um perfil por usuário |
| `versao` (`@Version`) | estoque | Bloqueio otimista contra venda duplicada do último item |
| Saldo nunca negativo | `Estoque.baixar()` | Regra no domínio, vale para qualquer fluxo |

## Decisões de modelagem

1. **Estoque por unidade:** `estoque` liga unidade e produto. Cada loja tem saldo próprio.
2. **Pagamento desacoplado:** tabela própria, 1:N com o pedido (cada tentativa é um registro), sem dado de cartão.
3. **Preço congelado:** `item_pedido.preco_unitario` preserva o valor da venda, mesmo que o catálogo mude.
4. **Saldo de pontos derivado:** não existe coluna de saldo. O saldo é a soma de `fidelidade_movimento.pontos`, e o extrato é imutável.
5. **Cliente do pedido aponta para `usuario`** (e não para `cliente_perfil`), porque a venda precisa funcionar sem perfil de fidelidade.
6. **Referências lógicas (sem chave estrangeira):** `pedido_id` e `usuario_id` nos movimentos, `criado_por_id` no pedido. Registros históricos não devem impedir a remoção ou anonimização dos dados referenciados.
7. **Anonimização:** a conta do cliente é anonimizada, e não apagada. Os pedidos permanecem, mas deixam de identificar a pessoa.