# Slice 8 · Onda 2 · Superfícies de leitura

## Contratos publicados

- `GET /api/ergon/setores/arvore-organograma` publica `organograma-tree` como `HIERARCHY_TREE` de coleção. Aceita filtros escalares em query, `page` a partir de zero, `size` até 200, ordenação repetível e `includeIds`. A consulta usa a data solicitada ou a data corrente e a hierarquia solicitada ou a oficial. A resposta usa `data.content`, paginação e link de descoberta. Cada unidade mantém `id` String e publica `referenciaFicha` com período, data e hierarquia.
- `ficha-unidade` abre a composição exata da referência ou o diretório corrente pelo código. Os seis novos recursos mantêm `detail` de item.
- O outlet 360° pode enviar o identificador contextual completo como valor do filtro filho. A referência de vigência é resolvida dentro do escopo de órgão e da validade do período. O radar fixa a data e a hierarquia recebidas na própria referência. O código simples continua aceito nos filtros.

| Surface de item em Setor | Coleção filha | Filtro publicado | Recorte |
|---|---|---|---|
| `historico-vigencias` | `historico-setores` | `setor` | Vigências da unidade |
| `parametrizacoes-ergon` | `setores-ergon` | `setor` | Parametrizações da unidade |
| `cargos-autorizados` | `cargos-por-setor` | `setor` | Autorizações diretamente registradas |
| `designacoes-chefia` | `designacoes` | `setorId` | Atos da unidade no órgão autorizado |
| `radar-subarvore` | `radar-organograma-chefias-ocupacao` | `raizSubarvore` | Raiz e descendentes em toda profundidade |

`raizSubarvore` é o seletor do relacionamento transitivo. `setorPai` preserva o filtro de subordinados diretos. A decisão mantém os dois predicados independentes no POST que a Table usa.

## Apuração e governança

| Recurso | Fatos publicados | Composição e comparação | Distribuição da projeção | Medida adicional na API |
|---|---|---|---|---|
| Setor | Classificação e subordinados diretos | Tipo de setor | UF de lotação | Histograma de subordinados diretos |
| Histórico | Integridade hierárquica | Tipo de setor | UF de lotação | — |
| Parâmetros Ergon | Centro de custo | Tipo de acesso | Grau de risco | — |
| Cargos por setor | Limite autorizado | Controle de quadro | Unidade | Histograma de lotação autorizada |
| Subempresas/localidades | Natureza da unidade | Natureza fiscal ou territorial | UF | — |
| Grupos | Tipo de agrupamento | Finalidade do agrupamento | UF de abrangência | — |
| Radar | Diagnóstico de comando, vagas diretas e escopo da apuração | Situação do comando | Tipo de setor | Histograma de vagas diretas |

Os sete recursos registram agrupamento nominal com abertura da ficha pelo identificador do bucket, composição, distribuição por termos, série pelo início de vigência e comparação entre períodos de início. As séries e comparações operam sobre o conjunto filtrado. No radar, a composição continua sendo um recorte temporal por data e hierarquia. No Setor, stats e export mantêm o escopo do diretório corrente. Histórico e Radar oferecem os recortes temporais.

O exportador canônico atende CSV, JSON e Excel nos sete recursos. Cada serviço declara campos permitidos, apresentação de datas e, onde aplicável, rótulos para booleanos. O teto do host é de 1.000 linhas, com escopos AUTO, SELECTED, FILTERED, CURRENT_PAGE e ALL. Nomes e identificadores pessoais de chefia, motivo de ausência e campos fora das listas editoriais ficam fora das exportações organizacionais.

O radar mantém nulos para ocupação, provimento, exercício, apropriação de folha e cobertura de chefia quando a fonte não permite apuração. `escopoApuracao` acompanha o resultado e a exportação. Vagas autorizadas somam autorizações diretas vigentes. Reservas de subordinados continuam visíveis como autorizações próprias e ficam fora dessa soma. A semântica de quadro e parametrização foi conferida em `v7x/aps/cargo_setor.tab`, `v7x/aps/fontes_ergon/pck_erg_quadro.pkb`, `v7x/aps/setores_ergon.tab` e `v7x/aps/fontes_ergon/ergadm00102_setores_ergon.vw` no acervo legado. A apuração executável é a view V11 já aceita na Onda 1.

## Matriz comparativa

Cobertura e indicadores editoriais comparados com o aceite R1–R4. Cada par de contagens mostra Onda 1 → esta entrega.

| Recurso | Colunas de negócio → Response | Cobertura | Quick View | Identidade rica | Refs/preview | Micro | Lookup rico | Editorial | Filtros/ops | Quick filters | Surfaces | Facts | Projeções de analytics | Export CSV/JSON/Excel |
|---|---:|---:|---:|---|---:|---:|---|---:|---:|---:|---:|---:|---:|---|
| Setor | 40 → 41 | 100% | 5 | Sim | 12/12 | 1 | Sim | 0 | 19/8 | 2 | 1 → 7 | 1 → 2 | 0 → 5 | Sim |
| Histórico | 37 → 39 | 100% | 6 | Sim | 11/11 | 1 | Sim | 0 | 22/9 | 2 | 1 → 1 | 0 → 1 | 0 → 5 | Sim |
| Parâmetros Ergon | 23 → 24 | 100% | 6 | Sim | 6/6 | 1 | Sim | 0 | 20/7 | 2 | 1 → 1 | 0 → 1 | 0 → 5 | Sim |
| Cargos por setor | 16 → 18 | 100% | 6 | Sim | 7/7 | 1 | Sim | 0 | 20/8 | 2 | 1 → 1 | 0 → 1 | 0 → 5 | Sim |
| Subempresas/localidades | 25 → 25 | 100% | 6 | Sim | 4/4 | 1 | Sim | 0 | 21/7 | 2 | 1 → 1 | 0 → 1 | 0 → 5 | Sim |
| Grupos | 14 → 14 | 100% | 6 | Sim | 4/4 | 1 | Sim | 0 | 18/8 | 2 | 1 → 1 | 0 → 1 | 0 → 5 | Sim |
| Radar | 41 → 43 | 100% | 6 | Sim | 15/15 | 2 | N/A | 0 | 21/5 | 2 | 1 → 1 | 0 → 3 | 0 → 5 | Sim |

O auditor conta 21 filtros anotados no Radar. `raizSubarvore` é um 22º critério, implementado como predicado correlacionado pela identidade, vigência e caminho do ancestral. Os testes do fluxo 360° exercitam esse critério.

## Pendências de escrita e regras

| Recursos | Intent | Workflow | BulkEditable/BulkOps | Determinações | Invariantes e fronteira |
|---|---:|---:|---:|---:|---|
| Setor e Radar | 0 | 0 | 0/0 | 0 | Leitura e isolamento por órgão |
| Histórico, Parâmetros, Cargos, Subempresas e Grupos | 0 | 0 | 0/0 | 0 | Bloqueio de escrita provado no gate existente. Regras de negócio transacionais ficam para a fase correspondente |

O auditor estrito mantém **CHANGES REQUESTED** para o escopo integral do host. Restam cinco decisões de duplicação e quatro reprovações estruturais pelos gates de escrita dos recursos Histórico, Parâmetros, Cargos e Subempresas. Caminho e nível são derivados. Lotação ocupada segue sem fonte temporal admitida. Esses campos e as capacidades de comando devem ser contabilizados no handoff de escrita.
