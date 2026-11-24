package br.com.raizesdonordeste.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Documentação OpenAPI da API. A leitura dos controladores é automática (springdoc);
 * aqui entram o que ela não sabe: segurança JWT, resumos, exemplos e códigos de status reais.
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";
    private static final String REF_ERRO = "#/components/schemas/ErroResponse";

    static {
        // @AuthenticationPrincipal é injetado pelo Spring Security e não faz parte do contrato HTTP
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal.class);
    }

    private static final String DESCRICAO_API = """
            API REST do back-end da rede **Raízes do Nordeste**: autenticação com perfis, cardápio e estoque por \
            unidade, pedidos multicanal, pagamento simulado (mock), fidelidade com consentimento (LGPD).

            **Como testar nesta tela**
            1. Em *Autenticação*, execute `POST /auth/login` e copie o `accessToken` da resposta.
            2. Clique em **Authorize** (cadeado, no topo), cole o token **sem** o prefixo `Bearer` e confirme.
            3. Execute os demais endpoints. O cadeado fechado indica que a rota exige token.

            **Padrões**
            - Listagens paginadas por `page` (a partir de 1) e `limit` (máximo 100).
            - Todo erro, inclusive 401 e 403, segue o mesmo JSON (schema `ErroResponse`).
            - `canalPedido` (APP, TOTEM, BALCAO, PICKUP, WEB) é obrigatório na criação de pedidos.
            """;

    private static final List<Tag> TAGS = List.of(
            new Tag().name("Autenticação").description("Cadastro público de cliente e login (JWT)"),
            new Tag().name("Usuários").description("Cadastro de usuários internos (ADMIN)"),
            new Tag().name("Unidades").description("Lojas da rede"),
            new Tag().name("Produtos").description("Catálogo global de produtos"),
            new Tag().name("Cardápio").description("O que cada unidade vende e a que preço"),
            new Tag().name("Estoque").description("Saldo por unidade, entradas, saídas e histórico"),
            new Tag().name("Pedidos").description("Criação, consulta por canal e status, andamento e cancelamento"),
            new Tag().name("Pagamentos").description("Cobrança via gateway externo simulado"),
            new Tag().name("Fidelidade").description("Consentimento, pontos, histórico e resgate"),
            new Tag().name("Cliente (LGPD)").description("Dados pessoais do titular: acesso, correção e anonimização"));

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Raízes do Nordeste")
                        .version("0.1.0")
                        .description(DESCRICAO_API)
                        .contact(new Contact().name("Equipe de Back-end")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Ambiente local")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtido em POST /auth/login. Cole apenas o token, sem o prefixo Bearer.")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }

    @Bean
    public OpenApiCustomizer documentacaoDosEndpoints() {
        return openApi -> {
            registrarSchemaDeErro(openApi);
            openApi.setTags(TAGS);
            if (openApi.getPaths() == null) return;

            openApi.getPaths().forEach((caminho, item) ->
                    item.readOperationsMap().forEach((metodo, operacao) -> {
                        Doc doc = DOCS.get(metodo.name() + " " + caminho);
                        if (doc == null) {
                            log.warn("Operação sem documentação central: {} {}", metodo, caminho);
                            return;
                        }
                        aplicar(doc, operacao, caminho);
                    }));
        };
    }

    // ------------------------------------------------------------------
    // Aplicação da documentação em cada operação
    // ------------------------------------------------------------------
    private void aplicar(Doc doc, Operation op, String caminho) {
        op.setTags(List.of(doc.tag));
        op.setSummary(doc.resumo);
        if (doc.descricao != null) op.setDescription(doc.descricao);
        if (doc.publico) op.setSecurity(List.of()); // sem cadeado: rota pública

        descreverParametros(op);

        boolean temCorpo = op.getRequestBody() != null;
        if (temCorpo && doc.exemploReq != null && op.getRequestBody().getContent() != null) {
            op.getRequestBody().getContent().values().forEach(m -> m.setExample(doc.exemploReq));
        }

        ApiResponse modelo = op.getResponses() != null ? op.getResponses().get("200") : null;
        TreeMap<String, ApiResponse> respostas = new TreeMap<>();

        // respostas com corpo do tipo de retorno do método (sucesso, 402, 503...)
        doc.comCorpo.forEach((codigo, desc) ->
                respostas.put(codigo, comCorpo(codigo, desc, modelo, doc.exemplos.get(codigo))));

        // erros padronizados que se aplicam por regra
        if (temCorpo) {
            respostas.put("400", erro("400", "JSON malformado ou valor de enumeração inexistente",
                    "CORPO_INVALIDO", caminho, List.of()));
            if (!doc.sem422) {
                respostas.put("422", erro("422", "Dados inválidos: o campo detalhes lista cada erro",
                        "VALIDACAO", caminho, List.of("campo: mensagem do erro")));
            }
        }
        if (!doc.publico) {
            respostas.put("401", erro("401", "Token ausente, inválido ou expirado", "NAO_AUTENTICADO", caminho, List.of()));
            respostas.put("403", erro("403", "Perfil sem permissão ou fora do escopo da unidade", "ACESSO_NEGADO", caminho, List.of()));
        }
        respostas.put("500", erro("500", "Erro interno, sem detalhes técnicos na resposta", "ERRO_INTERNO", caminho, List.of()));

        // erros específicos do endpoint (sobrescrevem os automáticos)
        doc.erros.forEach((codigo, e) -> respostas.put(codigo, erro(codigo, e[0], e[1], caminho, List.of())));

        ApiResponses finais = new ApiResponses();
        respostas.forEach(finais::addApiResponse);
        op.setResponses(finais);
    }

    private ApiResponse comCorpo(String codigo, String descricao, ApiResponse modelo, Object exemplo) {
        ApiResponse r = new ApiResponse().description(descricao);
        if (!"204".equals(codigo) && modelo != null && modelo.getContent() != null) {
            Content copia = new Content();
            modelo.getContent().forEach((tipo, mt) -> {
                MediaType novo = new MediaType().schema(mt.getSchema());
                if (exemplo != null) novo.setExample(exemplo);
                copia.addMediaType(tipo, novo);
            });
            r.setContent(copia);
        }
        return r;
    }

    private ApiResponse erro(String codigo, String descricao, String codigoErro, String caminho, List<String> detalhes) {
        Map<String, Object> exemplo = obj(
                "timestamp", "2026-11-18T19:51:37",
                "status", Integer.parseInt(codigo),
                "codigo", codigoErro,
                "mensagem", descricao,
                "caminho", caminho,
                "detalhes", detalhes);
        MediaType mt = new MediaType().schema(new Schema<>().$ref(REF_ERRO)).example(exemplo);
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/json", mt));
    }

    private void descreverParametros(Operation op) {
        if (op.getParameters() == null) return;
        for (Parameter p : op.getParameters()) {
            if (p.getName() == null) continue;
            switch (p.getName()) {
                case "page" -> p.setDescription("Página, a partir de 1");
                case "limit" -> p.setDescription("Itens por página (padrão 10, máximo 100)");
                case "Idempotency-Key" -> {
                    p.setDescription("Opcional. A mesma chave devolve o pedido original (200), sem nova baixa de estoque. "
                            + "Só vale para quem a criou. Máximo de 80 caracteres");
                    p.setExample("pedido-2026-0001");
                }
                default -> { }
            }
        }
    }

    private void registrarSchemaDeErro(OpenAPI openApi) {
        Schema<?> erro = new ObjectSchema()
                .description("Formato único de erro de toda a API")
                .addProperty("timestamp", new StringSchema().format("date-time").example("2026-11-18T19:51:37"))
                .addProperty("status", new IntegerSchema().example(409))
                .addProperty("codigo", new StringSchema().example("ESTOQUE_INSUFICIENTE")
                        .description("Identificador estável do erro: o cliente deve reagir a ele, e não ao texto"))
                .addProperty("mensagem", new StringSchema().example("Estoque insuficiente para o produto 2"))
                .addProperty("caminho", new StringSchema().example("/pedidos"))
                .addProperty("detalhes", new ArraySchema().items(new StringSchema())
                        .description("Erros de validação campo a campo (vazio nos demais casos)"));
        if (openApi.getComponents() == null) openApi.setComponents(new Components());
        openApi.getComponents().addSchemas("ErroResponse", erro);
    }

    // ------------------------------------------------------------------
    // Estrutura da tabela central
    // ------------------------------------------------------------------
    private static final class Doc {
        final String tag;
        final String resumo;
        String descricao;
        boolean publico;
        boolean sem422;
        Object exemploReq;
        final Map<String, String> comCorpo = new LinkedHashMap<>();
        final Map<String, Object> exemplos = new LinkedHashMap<>();
        final Map<String, String[]> erros = new LinkedHashMap<>();

        Doc(String tag, String resumo) {
            this.tag = tag;
            this.resumo = resumo;
        }

        Doc descricao(String t) { this.descricao = t; return this; }
        Doc publico() { this.publico = true; return this; }
        Doc sem422() { this.sem422 = true; return this; }
        Doc req(Object exemplo) { this.exemploReq = exemplo; return this; }
        /** Resposta que devolve o corpo do tipo de retorno do método (sucesso, 402, 503...). */
        Doc ok(String codigo, String desc) { comCorpo.put(codigo, desc); return this; }
        Doc exemplo(String codigo, Object exemplo) { exemplos.put(codigo, exemplo); return this; }
        Doc erro(String codigo, String desc, String codigoErro) { erros.put(codigo, new String[]{desc, codigoErro}); return this; }
    }

    private static Doc d(String tag, String resumo) {
        return new Doc(tag, resumo);
    }

    private static Map<String, Object> obj(Object... paresChaveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            m.put((String) paresChaveValor[i], paresChaveValor[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> pagina(Object item) {
        return obj("content", List.of(item), "page", 1, "limit", 10, "totalElements", 1, "totalPages", 1);
    }

    // ------------------------------------------------------------------
    // Exemplos de resposta
    // ------------------------------------------------------------------
    private static final Map<String, Object> USUARIO = obj("id", 5, "nome", "Maria Silva",
            "email", "maria@email.com", "role", "CLIENTE", "unidadeId", null, "ativo", true);
    private static final Map<String, Object> UNIDADE = obj("id", 1, "nome", "Raízes Boa Viagem",
            "cidade", "Recife", "uf", "PE", "ativa", true);
    private static final Map<String, Object> PRODUTO = obj("id", 1, "nome", "Tapioca de queijo coalho",
            "descricao", "Tapioca recheada com queijo coalho", "categoria", "Tapiocas", "preco", 12.00, "ativo", true);
    private static final Map<String, Object> ITEM_CARDAPIO = obj("produtoId", 2, "nome", "Cuscuz recheado",
            "descricao", "Cuscuz com carne de sol", "categoria", "Cuscuz", "preco", 15.00, "disponivel", true);
    private static final Map<String, Object> ESTOQUE = obj("produtoId", 1,
            "produto", "Tapioca de queijo coalho", "quantidade", 97);
    private static final Map<String, Object> MOVIMENTACAO = obj("id", 31, "produtoId", 1, "tipo", "SAIDA",
            "quantidade", 3, "saldoApos", 97, "motivo", "Perda", "pedidoId", null, "criadoEm", "2026-11-18T18:30:00");
    private static final Map<String, Object> PEDIDO = obj("id", 12, "canalPedido", "APP",
            "status", "AGUARDANDO_PAGAMENTO", "unidadeId", 1, "clienteId", 5, "formaPagamento", "MOCK",
            "subtotal", 29.00, "desconto", 0, "pontosResgatados", 0, "total", 29.00,
            "itens", List.of(obj("produtoId", 2, "produto", "Cuscuz recheado", "quantidade", 2,
                    "precoUnitario", 14.50, "subtotal", 29.00)),
            "criadoEm", "2026-11-18T20:04:32");
    private static final Map<String, Object> PAGAMENTO_APROVADO = obj("id", 8, "pedidoId", 12, "forma", "MOCK",
            "status", "APROVADO", "mensagem", "Pagamento aprovado", "valor", 29.00,
            "referenciaExterna", "MOCK-3f1c9a52",
            "payload", "{\"transactionId\":\"MOCK-3f1c9a52\",\"status\":\"APPROVED\",\"amount\":29.00}",
            "statusPedido", "PAGO", "criadoEm", "2026-11-18T20:06:10");
    private static final Map<String, Object> PAGAMENTO_RECUSADO = obj("id", 7, "pedidoId", 12, "forma", "MOCK",
            "status", "RECUSADO", "mensagem", "Pagamento recusado pelo emissor (simulação)", "valor", 29.00,
            "referenciaExterna", "MOCK-81b7d0e4",
            "payload", "{\"transactionId\":\"MOCK-81b7d0e4\",\"status\":\"DECLINED\",\"reason\":\"insufficient_funds\"}",
            "statusPedido", "AGUARDANDO_PAGAMENTO", "criadoEm", "2026-11-18T20:05:40");
    private static final Map<String, Object> PAGAMENTO_ERRO = obj("id", 6, "pedidoId", 12, "forma", "MOCK",
            "status", "ERRO", "mensagem", "Gateway de pagamento indisponível. Tente novamente", "valor", 29.00,
            "referenciaExterna", null, "payload", null,
            "statusPedido", "AGUARDANDO_PAGAMENTO", "criadoEm", "2026-11-18T20:05:10");
    private static final Map<String, Object> SALDO = obj("consentimentoFidelidade", true, "saldo", 29,
            "valorPorPonto", 0.10, "resgateMinimo", 10);
    private static final Map<String, Object> MOV_PONTOS = obj("id", 3, "tipo", "ACUMULO", "pontos", 29,
            "pedidoId", 12, "descricao", "Compra - pedido #12", "criadoEm", "2026-11-18T20:06:10");
    private static final Map<String, Object> CLIENTE = obj("usuarioId", 5, "nome", "Maria Silva",
            "email", "maria@email.com", "dataNascimento", "1995-05-20",
            "consentimentoFidelidade", true, "consentimentoEm", "2026-11-18T20:00:00");

    // ------------------------------------------------------------------
    // Tabela central: "MÉTODO /rota" -> documentação
    // ------------------------------------------------------------------
    private static final Map<String, Doc> DOCS = montarDocs();

    private static Map<String, Doc> montarDocs() {
        Map<String, Doc> m = new LinkedHashMap<>();

        // ---------- Autenticação ----------
        m.put("POST /auth/register", d("Autenticação", "Cadastrar cliente")
                .descricao("Cadastro público. O perfil criado é **sempre CLIENTE**: perfis internos só são criados por um ADMIN em `POST /usuarios`.")
                .publico().ok("201", "Cliente cadastrado").exemplo("201", USUARIO)
                .req(obj("nome", "Maria Silva", "email", "maria@email.com", "senha", "Senha@123"))
                .erro("409", "E-mail já cadastrado", "EMAIL_JA_CADASTRADO"));
        m.put("POST /auth/login", d("Autenticação", "Entrar e obter o token JWT")
                .descricao("Devolve o token a ser enviado no cabeçalho `Authorization: Bearer <token>`. Mensagem única para e-mail inexistente, usuário inativo ou senha errada.")
                .publico().ok("200", "Token de acesso (expira em 1 hora)")
                .exemplo("200", obj("accessToken", "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJtYXJpYUBlbWFpbC5jb20iLCJyb2xlIjoiQ0xJRU5URSJ9.assinatura",
                        "tokenType", "Bearer", "expiresIn", 3600, "role", "CLIENTE"))
                .req(obj("email", "maria@email.com", "senha", "Senha@123"))
                .erro("401", "E-mail ou senha inválidos", "CREDENCIAIS_INVALIDAS"));

        // ---------- Usuários ----------
        m.put("POST /usuarios", d("Usuários", "Cadastrar usuário interno")
                .descricao("Somente ADMIN. GERENTE, ATENDENTE e COZINHA exigem `unidadeId`.")
                .ok("201", "Usuário criado").exemplo("201", obj("id", 6, "nome", "Gerente Recife",
                        "email", "gerente@raizes.com", "role", "GERENTE", "unidadeId", 1, "ativo", true))
                .req(obj("nome", "Gerente Recife", "email", "gerente@raizes.com", "senha", "Senha@123",
                        "role", "GERENTE", "unidadeId", 1))
                .erro("404", "Unidade inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "E-mail já cadastrado", "EMAIL_JA_CADASTRADO"));
        m.put("GET /usuarios/{id}", d("Usuários", "Consultar usuário")
                .descricao("Somente ADMIN. Nunca devolve a senha nem o hash.")
                .ok("200", "Usuário encontrado").exemplo("200", USUARIO)
                .erro("404", "Usuário inexistente", "RECURSO_NAO_ENCONTRADO"));

        // ---------- Unidades ----------
        m.put("GET /unidades", d("Unidades", "Listar unidades ativas")
                .publico().ok("200", "Página de unidades").exemplo("200", pagina(UNIDADE)));
        m.put("GET /unidades/{id}", d("Unidades", "Consultar unidade")
                .publico().ok("200", "Unidade encontrada").exemplo("200", UNIDADE)
                .erro("404", "Unidade inexistente", "RECURSO_NAO_ENCONTRADO"));
        m.put("POST /unidades", d("Unidades", "Criar unidade")
                .descricao("Somente ADMIN. A UF é normalizada para maiúsculas.")
                .ok("201", "Unidade criada").exemplo("201", UNIDADE)
                .req(obj("nome", "Raízes Boa Viagem", "cidade", "Recife", "uf", "PE")));
        m.put("PUT /unidades/{id}", d("Unidades", "Atualizar unidade")
                .descricao("Somente ADMIN.")
                .ok("200", "Unidade atualizada").exemplo("200", UNIDADE)
                .req(obj("nome", "Raízes Boa Viagem Shopping", "cidade", "Recife", "uf", "PE"))
                .erro("404", "Unidade inexistente", "RECURSO_NAO_ENCONTRADO"));
        m.put("DELETE /unidades/{id}", d("Unidades", "Desativar unidade")
                .descricao("Somente ADMIN. Exclusão lógica: pedidos e estoque referenciam a unidade.")
                .ok("204", "Unidade desativada")
                .erro("404", "Unidade inexistente", "RECURSO_NAO_ENCONTRADO"));

        // ---------- Produtos ----------
        m.put("GET /produtos", d("Produtos", "Listar produtos ativos")
                .publico().ok("200", "Página de produtos").exemplo("200", pagina(PRODUTO)));
        m.put("GET /produtos/{id}", d("Produtos", "Consultar produto")
                .descricao("Devolve também produtos desativados (consulta de histórico).")
                .publico().ok("200", "Produto encontrado").exemplo("200", PRODUTO)
                .erro("404", "Produto inexistente", "RECURSO_NAO_ENCONTRADO"));
        m.put("POST /produtos", d("Produtos", "Criar produto")
                .descricao("GERENTE ou ADMIN.")
                .ok("201", "Produto criado").exemplo("201", PRODUTO)
                .req(obj("nome", "Tapioca de queijo coalho", "descricao", "Tapioca recheada com queijo coalho",
                        "categoria", "Tapiocas", "preco", 12.00)));
        m.put("PUT /produtos/{id}", d("Produtos", "Atualizar produto")
                .descricao("GERENTE ou ADMIN. Pedidos antigos mantêm o preço da época da venda.")
                .ok("200", "Produto atualizado").exemplo("200", PRODUTO)
                .req(obj("nome", "Tapioca de queijo coalho", "descricao", "Tapioca com queijo coalho e manteiga",
                        "categoria", "Tapiocas", "preco", 12.50))
                .erro("404", "Produto inexistente", "RECURSO_NAO_ENCONTRADO"));
        m.put("DELETE /produtos/{id}", d("Produtos", "Desativar produto")
                .descricao("GERENTE ou ADMIN. Exclusão lógica: preserva o histórico de pedidos.")
                .ok("204", "Produto desativado")
                .erro("404", "Produto inexistente", "RECURSO_NAO_ENCONTRADO"));

        // ---------- Cardápio ----------
        m.put("GET /unidades/{unidadeId}/cardapio", d("Cardápio", "Consultar cardápio da unidade")
                .descricao("Público. Só aparecem itens **disponíveis** de produtos **ativos**. O `preco` já é o efetivo (preço local, senão o do catálogo).")
                .publico().ok("200", "Página de itens do cardápio").exemplo("200", pagina(ITEM_CARDAPIO))
                .erro("404", "Unidade inexistente", "RECURSO_NAO_ENCONTRADO"));
        m.put("POST /unidades/{unidadeId}/cardapio", d("Cardápio", "Adicionar produto ao cardápio")
                .descricao("GERENTE (somente da própria unidade) ou ADMIN.")
                .ok("201", "Produto adicionado").exemplo("201", ITEM_CARDAPIO)
                .req(obj("produtoId", 2, "precoLocal", 15.00))
                .erro("404", "Unidade ou produto inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "PRODUTO_INATIVO ou PRODUTO_JA_NO_CARDAPIO", "PRODUTO_JA_NO_CARDAPIO"));
        m.put("PUT /unidades/{unidadeId}/cardapio/{produtoId}", d("Cardápio", "Atualizar item do cardápio")
                .descricao("Define o preço local (nulo volta ao preço do catálogo) e a disponibilidade. `disponivel: false` tira o item de venda.")
                .ok("200", "Item atualizado").exemplo("200", ITEM_CARDAPIO)
                .req(obj("precoLocal", 15.00, "disponivel", true))
                .erro("404", "Item fora do cardápio da unidade", "RECURSO_NAO_ENCONTRADO"));

        // ---------- Estoque ----------
        m.put("GET /unidades/{unidadeId}/estoque", d("Estoque", "Listar estoque da unidade")
                .descricao("Equipe da própria unidade (gerente, atendente, cozinha) ou ADMIN.")
                .ok("200", "Página de saldos").exemplo("200", pagina(ESTOQUE))
                .erro("404", "Unidade inexistente", "RECURSO_NAO_ENCONTRADO"));
        m.put("GET /unidades/{unidadeId}/estoque/{produtoId}", d("Estoque", "Consultar saldo de um produto")
                .ok("200", "Saldo do produto").exemplo("200", ESTOQUE)
                .erro("404", "Produto sem registro de estoque na unidade", "RECURSO_NAO_ENCONTRADO"));
        m.put("GET /unidades/{unidadeId}/estoque/{produtoId}/movimentacoes", d("Estoque", "Histórico de movimentações")
                .descricao("Da mais recente para a mais antiga. Saídas por venda trazem o `pedidoId`.")
                .ok("200", "Página de movimentações").exemplo("200", pagina(MOVIMENTACAO))
                .erro("404", "Produto sem registro de estoque na unidade", "RECURSO_NAO_ENCONTRADO"));
        m.put("POST /unidades/{unidadeId}/estoque/movimentacoes", d("Estoque", "Registrar entrada ou saída")
                .descricao("GERENTE (da unidade) ou ADMIN. O produto precisa estar no cardápio. **SAIDA exige `motivo`**. A primeira ENTRADA cria o registro de estoque.")
                .ok("201", "Movimentação registrada").exemplo("201", MOVIMENTACAO)
                .req(obj("produtoId", 1, "tipo", "ENTRADA", "quantidade", 100, "motivo", "Estoque inicial"))
                .erro("404", "Unidade ou produto inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "ESTOQUE_INSUFICIENTE, PRODUTO_FORA_DO_CARDAPIO ou CONFLITO_CONCORRENCIA", "ESTOQUE_INSUFICIENTE"));

        // ---------- Pedidos ----------
        m.put("POST /pedidos", d("Pedidos", "Criar pedido")
                .descricao("Valida cada item (ativo, no cardápio e disponível), congela o preço, grava o pedido e baixa o estoque **na mesma transação**: se faltar saldo, nada é gravado. "
                        + "`canalPedido` é obrigatório. O cliente sempre compra para si; o funcionário pode informar `clienteId` ou vender sem identificação. "
                        + "Com o cabeçalho `Idempotency-Key`, uma repetição devolve o pedido original com **200**.")
                .ok("201", "Pedido criado, aguardando pagamento")
                .ok("200", "Repetição idempotente: pedido original, sem novo efeito")
                .exemplo("201", PEDIDO).exemplo("200", PEDIDO)
                .req(obj("canalPedido", "APP", "unidadeId", 1, "formaPagamento", "MOCK",
                        "itens", List.of(obj("produtoId", 2, "quantidade", 2))))
                .erro("404", "Unidade, produto ou cliente inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "ESTOQUE_INSUFICIENTE, PRODUTO_INATIVO, PRODUTO_FORA_DO_CARDAPIO, PRODUTO_INDISPONIVEL, UNIDADE_INATIVA, IDEMPOTENCY_KEY_EM_USO ou CONFLITO_CONCORRENCIA", "ESTOQUE_INSUFICIENTE"));
        m.put("GET /pedidos", d("Pedidos", "Listar e filtrar pedidos")
                .descricao("Filtros combináveis: `canalPedido`, `status` e `unidadeId`. **Visão por perfil**: CLIENTE vê só os próprios; ATENDENTE, COZINHA e GERENTE só os da própria unidade; ADMIN vê todos. Mais recentes primeiro.")
                .ok("200", "Página de pedidos").exemplo("200", pagina(PEDIDO))
                .erro("400", "Parâmetro com formato inválido (ex.: canalPedido inexistente)", "PARAMETRO_INVALIDO"));
        m.put("GET /pedidos/{id}", d("Pedidos", "Consultar pedido")
                .descricao("O cliente só enxerga os próprios pedidos: o de outra pessoa responde **404**, e não 403, para não revelar que o id existe.")
                .ok("200", "Pedido encontrado").exemplo("200", PEDIDO)
                .erro("404", "Pedido inexistente ou de outro cliente", "RECURSO_NAO_ENCONTRADO"));
        m.put("PATCH /pedidos/{id}/status", d("Pedidos", "Atualizar andamento do pedido")
                .descricao("Aceita apenas EM_PREPARO, PRONTO e ENTREGUE (PAGO só pelo pagamento e CANCELADO só pelo cancelamento). "
                        + "COZINHA: EM_PREPARO e PRONTO. ATENDENTE: ENTREGUE. GERENTE e ADMIN: todos. Não se pula etapa.")
                .ok("200", "Status atualizado").exemplo("200", PEDIDO)
                .req(obj("status", "EM_PREPARO"))
                .erro("404", "Pedido inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "Transição de status inválida", "TRANSICAO_STATUS_INVALIDA")
                .erro("422", "Status não permitido nesta rota (PAGO ou CANCELADO)", "VALIDACAO"));
        m.put("POST /pedidos/{id}/cancelamento", d("Pedidos", "Cancelar pedido")
                .descricao("Possível até EM_PREPARO. Na mesma transação: devolve o estoque, estorna o pagamento aprovado e estorna os pontos. O cliente não cancela pedido em preparo.")
                .ok("200", "Pedido cancelado")
                .exemplo("200", obj("id", 12, "canalPedido", "APP", "status", "CANCELADO", "unidadeId", 1, "clienteId", 5,
                        "formaPagamento", "MOCK", "subtotal", 29.00, "desconto", 0, "pontosResgatados", 0, "total", 29.00,
                        "itens", List.of(), "criadoEm", "2026-11-18T20:04:32"))
                .erro("404", "Pedido inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "TRANSICAO_STATUS_INVALIDA ou CANCELAMENTO_NAO_PERMITIDO", "TRANSICAO_STATUS_INVALIDA"));

        // ---------- Pagamentos ----------
        m.put("POST /pedidos/{pedidoId}/pagamentos", d("Pagamentos", "Pagar pedido (gateway simulado)")
                .descricao("O corpo é **opcional**. `simulacao` existe só por causa do mock: APROVAR (padrão), RECUSAR ou TIMEOUT. "
                        + "O corpo da resposta tem sempre o mesmo formato, e o **status HTTP** indica o resultado: 201 aprovado, 402 recusado, 503 gateway indisponível. "
                        + "Recusa e indisponibilidade **não cancelam** o pedido, que permite nova tentativa.")
                .sem422()
                .ok("201", "Pagamento aprovado: o pedido passa a PAGO")
                .ok("402", "Pagamento recusado: o pedido continua aguardando pagamento")
                .ok("503", "Gateway indisponível: pagamento registrado como ERRO, tente novamente")
                .exemplo("201", PAGAMENTO_APROVADO).exemplo("402", PAGAMENTO_RECUSADO).exemplo("503", PAGAMENTO_ERRO)
                .req(obj("simulacao", "APROVAR"))
                .erro("404", "Pedido inexistente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "Pedido não aguarda pagamento", "PEDIDO_NAO_ACEITA_PAGAMENTO"));
        m.put("GET /pedidos/{pedidoId}/pagamentos", d("Pagamentos", "Listar tentativas de pagamento")
                .descricao("Todas as tentativas do pedido, da mais recente para a mais antiga.")
                .ok("200", "Tentativas de pagamento").exemplo("200", List.of(PAGAMENTO_APROVADO))
                .erro("404", "Pedido inexistente ou de outro cliente", "RECURSO_NAO_ENCONTRADO"));

        // ---------- Fidelidade ----------
        m.put("GET /fidelidade/saldo", d("Fidelidade", "Consultar saldo de pontos")
                .descricao("O saldo é a soma do extrato. 1 ponto vale R$ 0,10 no resgate.")
                .ok("200", "Saldo e regras").exemplo("200", SALDO));
        m.put("GET /fidelidade/historico", d("Fidelidade", "Consultar extrato de pontos")
                .ok("200", "Página do extrato").exemplo("200", pagina(MOV_PONTOS)));
        m.put("POST /fidelidade/consentimento", d("Fidelidade", "Dar consentimento ao programa")
                .descricao("Registra a data do consentimento (LGPD). Repetir a chamada não altera a data original. Sem consentimento, o cliente compra normalmente, mas não pontua.")
                .ok("200", "Consentimento registrado").exemplo("200", SALDO));
        m.put("DELETE /fidelidade/consentimento", d("Fidelidade", "Revogar consentimento")
                .descricao("Interrompe o acúmulo e o resgate. O histórico é preservado.")
                .ok("200", "Consentimento revogado")
                .exemplo("200", obj("consentimentoFidelidade", false, "saldo", 29, "valorPorPonto", 0.10, "resgateMinimo", 10)));
        m.put("POST /fidelidade/resgates", d("Fidelidade", "Resgatar pontos como desconto")
                .descricao("Aplica o desconto a um pedido **aguardando pagamento**, uma vez por pedido, com no mínimo 10 pontos. O desconto não pode passar do subtotal.")
                .ok("200", "Pedido com desconto aplicado")
                .exemplo("200", obj("id", 13, "canalPedido", "APP", "status", "AGUARDANDO_PAGAMENTO", "unidadeId", 1,
                        "clienteId", 5, "formaPagamento", "MOCK", "subtotal", 29.00, "desconto", 1.00,
                        "pontosResgatados", 10, "total", 28.00,
                        "itens", List.of(obj("produtoId", 2, "produto", "Cuscuz recheado", "quantidade", 2,
                                "precoUnitario", 14.50, "subtotal", 29.00)),
                        "criadoEm", "2026-11-18T20:10:00"))
                .req(obj("pedidoId", 13, "pontos", 10))
                .erro("404", "Pedido inexistente ou de outro cliente", "RECURSO_NAO_ENCONTRADO")
                .erro("409", "PEDIDO_NAO_ACEITA_RESGATE, RESGATE_JA_APLICADO, CONSENTIMENTO_NECESSARIO, SALDO_PONTOS_INSUFICIENTE ou RESGATE_EXCEDE_VALOR_DO_PEDIDO", "SALDO_PONTOS_INSUFICIENTE")
                .erro("422", "Validação (ex.: mínimo de 10 pontos)", "VALIDACAO"));

        // ---------- Cliente (LGPD) ----------
        m.put("GET /clientes/me", d("Cliente (LGPD)", "Consultar meus dados")
                .descricao("Direito de acesso. Não existe id na rota: cada cliente só alcança os próprios dados.")
                .ok("200", "Dados do titular").exemplo("200", CLIENTE));
        m.put("PUT /clientes/me", d("Cliente (LGPD)", "Corrigir meus dados")
                .ok("200", "Dados atualizados").exemplo("200", CLIENTE)
                .req(obj("dataNascimento", "1995-05-20")));
        m.put("DELETE /clientes/me", d("Cliente (LGPD)", "Anonimizar minha conta")
                .descricao("Direito de eliminação. Remove nome, e-mail e data de nascimento, invalida a senha e desativa o acesso (o token atual deixa de valer). Os pedidos permanecem, sem identificar a pessoa.")
                .ok("204", "Conta anonimizada"));

        return m;
    }
}