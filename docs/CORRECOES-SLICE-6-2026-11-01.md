# Correções da revisão — Slice 6

> Histórico da contenção temporária, agora removida por solicitação explícita. O estado atual do CRUD e das invariantes está em [SLICE-6-FASE-1.md](SLICE-6-FASE-1.md).

01/11/2026. Classificação: `contrato-publico`. Baseline preservado:
`praxis-metadata-starter:8.0.0-rc.186`.

**Estado atual: consultas corrigidas e escritas operacionais indisponíveis. A
Issue #9 não está integralmente aceita.**

## Resultado por achado

| Achado da revisão | Tratamento executado | Limite do resultado |
|---|---|---|
| R1 — campos herdados ignorados | Os 17 predicados agora são declarados em cada FilterDTO concreto. A base mantém apenas o contrato de acesso ao órgão. | O suporte à herança do starter não foi alterado; o host passou a usar a forma executável do rc.186. |
| R2 — lookup sem escopo | Os quatro serviços normalizam filter, GET by-ids e POST by-ids com o órgão resolvido; payload divergente é rejeitado. O filtro concreto produz a restrição efetiva. | Não substitui autorização por identidade autenticada; X-Tenant mantém a convenção demonstrativa do host. |
| R3 — JSONB sem contêiner | `@CustomFields` declarado nas quatro entidades e no radar, com os mesmos resourceKeys dos DTOs. | Critérios foram exercitados localmente; SQL real exige a branch isolada admitida. |
| R4 — atos sem invariantes completas | Create, update e delete dos quatro recursos são recusados antes de mapper, consultas ou flush. A mesma indisponibilidade é publicada pela política canônica do host. | Contenção: concessão, venda, conversão e sincronização contábil continuam sem implementação operacional admitida. |
| R5 — PUT sem precondição | Os quatro serviços aderem ao contrato versionado; o caminho Java de dois argumentos rejeita ausência de precondição. A escrita com precondição continua indisponível pela política de domínio. | Contenção: não existe PUT bem-sucedido aprovado. Reabertura exige implementação transacional com requireMatch e prova de 412/428/sucesso em HTTP/PostgreSQL. |
| R6 — revisão incompleta do radar | O radar devolve representação sem revisão persistida. O helper canônico não emite ETag forte sem essa revisão. | Não foi inventada versão derivada de contadores ou timestamps. Uma revisão completa da projeção depende de contrato próprio. |
| R7 — finalidades/desaverbações sem eventos | Escritas foram fechadas, impedindo substituir posições/histórico pelo CRUD atual. O modelo consolidado permanece explicitamente de consulta. | Contenção: não foi criada trilha histórica fictícia. Certidão→finalidades→atos de desaverbação ainda precisa de fechamento de domínio. |
| R8 — herança ambígua e campos genéricos | Override do período usa HERDAR/PERMITIR/VEDAR; rótulos de serviço, aquisição, gozo e venda foram especializados. Quick View da venda inclui pagamento; gozo distingue término previsto e efetivo. | Schema/resolver validados. Não houve aceite visual em navegador. |

## Fronteira de escrita

`TempoLicencaWriteAvailability` é composto pelo provider de disponibilidade já
existente do host. Não há segundo bean concorrente para o SPI singular do rc.186.
A política nega `create`, `edit`, `delete` e `duplicate-draft` somente nos quatro
recursos novos. As operações de leitura e os demais recursos preservam sua
política existente.

A guarda também existe nos serviços, antes de qualquer consulta, mapper ou
persistência. Chamadas Java diretas não contornam a indisponibilidade da UI/MVC.
Não há feature flag, modo de compatibilidade ou comando alternativo para liberar
escrita sem fechar os gates de domínio.

As classes e os contratos de entrada continuam na hierarquia mutável solicitada,
mas isso representa suporte estrutural, não permissão operacional. Formulários e
agentes devem consumir disponibilidade/capabilities antes de oferecer atos. A
retomada dos atos depende da Fase 2/3, com fonte única de saldo, registros de ato,
concorrência e idempotência. Os métodos versionados hoje recusam a operação;
não constituem implementação de um fluxo transacional de concessão.

## Isolamento e consultas

Os cinco recursos aplicam ao acesso normal o órgão resolvido por
`TenantContext.orgao()`. Sem X-Tenant, o contexto documentado do host é órgão 1;
não há mais consulta global implícita nesses recursos. Outros órgãos exigem o
contexto correspondente. Essa escolha alinha consulta, cadastro de campos
customizados e reload dos lookups.

O payload não escolhe um órgão diferente do contexto nos caminhos de lookup.
Pessoa e vínculo continuam sendo dependências públicas de pesquisa, combinadas
com a restrição de órgão. Os testes executam as specifications entregues ao
executor, verificando os predicados de órgão, pessoa e vínculo. Não se limita à
contagem de annotations ou à inspeção do descriptor.

O catálogo de campos customizados do radar reutiliza o resourceKey do período de
origem de forma explícita no contêiner da entidade e nos DTOs. Os critérios JSONB
rejeitam chaves não declaradas/filterable. Isso não certifica políticas de
autorização de campo em runtime: a governança continua declarativa no baseline.

## Mudança pública de UX e migração beta

Somente no período de licença, `permGozoLicEspAntFimPe` passa de boolean anulável
para escolha textual fechada no wire:

| Valor público | Significado | Valor na coluna existente |
|---|---|---|
| HERDAR | Usar a matriz estatutária | NULL |
| PERMITIR | Override individual permissivo | TRUE |
| VEDAR | Override individual impeditivo | FALSE |

O converter JPA preserva o armazenamento da V5. A matriz geral em
`ParametroContagemFerias` continua booleana e default false. Não foram criados
aliases para aceitar boolean e enum em paralelo. Payloads e consumidores do
campo individual precisam usar as três strings; isso é mudança incompatível
intencional de contrato beta, restrita ao showcase e ainda sem publicação.

O schema declara explicitamente string e os três valores, evitando duplicar a
enumeração durante a composição Swagger. O resolver foi testado: o SELECT emite
exatamente três valores e o round-trip JSON/JPA mantém cada significado.
Os metadados da venda distinguem registro da venda e pagamento; o término
histórico não ocupa a tabela principal nem o Quick View. Os metadados de gozo
distinguem previsão e encerramento efetivo.

Impacto: DTOs/serializadores do host, fixtures de formulário e consumidores dos
schemas desses cinco recursos. Nenhuma biblioteca, starter ou aplicação Angular
foi alterada. A documentação e os testes do host foram atualizados. Não foi
realizada release nem alteração do baseline de dependências.

## Validação executada

Toolchain: Java 21 e Maven 3.9.6.

```text
mvn test "-Dtest=!*Neon*"
Tests run: 228, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os oito testes de `TempoLicencaCorrectionsContractTest` cobrem:

- Predicados efetivos dos campos comuns nos cinco FilterDTOs, usando o executor selecionado;
- Filter/GET by-ids/POST by-ids dos quatro lookups, com restrição de órgão, dependências e rejeição de contexto divergente;
- Construção de critérios JSONB nas cinco entidades e rejeição de chave não declarada;
- Bloqueio de todos os caminhos diretos de escrita antes dos repositórios e rejeição de PUT sem precondição;
- Composição da disponibilidade do host, preservando leitura e outros recursos;
- Round-trip dos três estados de antecipação e preservação do parâmetro geral booleano;
- Schema emitido com os três valores, data de venda e pagamento em Quick View;
- Ausência da revisão persistida incompleta no radar.

O [scorecard atual](evidence/SLICE-6-CORRECOES-SCORECARD.md) foi executado com
`--strict`: 21/25 linhas PASS, zero issues editoriais, quatro reprovações por
capacidades de Fases 2/3 e cinco decisões de duplicação pendentes. O código do
auditor não foi alterado. As saídas antigas de implementação/revisão permanecem
como histórico, não como evidência dos bytes atuais.

Não foram executados Flyway, Hibernate contra banco real, consultas SQL, HTTP
integrado nem navegador. A V5 não foi aplicada e o checkout continua sem V1–V4.
A ausência de baseline de banco isolado admitido impede afirmar persistência e
paridade. Sem API Slice 6 alimentada por esse baseline, não há prova visual
desktop/narrow representativa; os testes de resolver não a substituem.

## Pendências para aceite integral

Fechar o contrato relacional e de eventos de R7 e implementar os atos
transacionais de direito antes de reabrir escrita. Provar precondições, saldos,
elegibilidade, idempotência e concorrência reais. Validar a V5 no schema admitido,
executar os fluxos HTTP e UI e resolver a formalização dos gates de fases e
duplicação. A indisponibilidade segura não deve ser contabilizada como entrega
funcional dessas capacidades.
