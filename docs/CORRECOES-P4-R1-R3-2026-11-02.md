# Correções dos achados P4 R1–R3 — 02/11/2026

Os três contraexemplos R1–R3 da revisão de 01/11 receberam correções no
`praxis-metadata-starter`, consumidas pelo showcase. Os gates abaixo são o
registro histórico dessa correção. As sondagens temporárias foram substituídas
por testes JUnit permanentes no fechamento [P5-B](P5-B-FECHAMENTO.md).
Este documento não constitui uma nova revisão independente ou aceite integrado.

## Contrato e implementação

| Achado | Correção canônica | Adoção no showcase |
| --- | --- | --- |
| R1: campo opcional omitido produz erro interno | `CustomFieldValidator` dispensa apenas a restrição local de um campo opcional ausente. A dependência de `draft`, `entity`, `customFields` ou do contexto inteiro é reconhecida pelo validador de paths do engine, incluindo defaults aninhados. Essas regras continuam obrigatórias. Campo presente com `null` ou zero não equivale a omissão. | A facade delega a mesma política ao validador nativo; foi removida a heurística por substring. O descritor montado pela facade preserva `required`, impedindo que a correção torne opcionais os campos obrigatórios. |
| R2: admissão e publicação aceitam dialetos diferentes | A API pública `CustomFieldValidationRules.validate` é usada também pelo construtor de `CustomFieldDescriptor`. Ambos aplicam as mesmas raízes fechadas, limites e recusa de operadores contextuais. O adapter não recebe relógio nem timezone implícitos. | A admissão de definições usa essa API e traduz a rejeição para `ERG-21501`/`422`, antes de persistir. CREATE, UPDATE e manutenção da definição compartilham essa admissão. |
| R3: regras recebem draft anterior à hidratação | O template oferece `prepareCreateValidationDraft` e `prepareUpdateValidationDraft`. A validação primitiva antecede o preparo; a avaliação que observa `draft` recebe uma projeção explícita, hidratada e capturada antes dos hooks de negócio. UPDATE ordinário, versionado e avaliação de lote compartilham o helper canônico. | Gozo de licença especial, venda de licença especial e gozo de férias fornecem a projeção derivada do pai autorizado. O cliente pode omitir órgão/pessoa/vínculo quando o serviço os deriva. A atualização especializada de férias usa o mesmo helper. |

No UPDATE, `entity` continua representando o estado persistido autorizado e
anterior à mutação. O preparo recebe uma cópia destacada, conserva as guardas
de versão/acesso e não pode persistir nem executar efeitos de negócio. As
definições e os valores de campos dinâmicos são capturados uma vez por
preparação; o hook não relê as definições.

Uma regra incompatível eventualmente gravada antes desta correção exige
manutenção explícita da definição. Ela deve ser reescrita no contrato sem
contexto temporal, ou tratada como decisão governada com contexto temporal
explícito, conforme seu domínio. Não há conversão automática de `now`, remoção
de regra ou desativação silenciosa. Não foi consultado nem migrado um banco
remoto; a existência dessas linhas em ambientes implantados não foi apurada.
Os descritores continuam recusando definições incompatíveis.

## Provas de regressão

- `CustomFieldAdoptionRegressionTest`: omissão, valor presente, `null`, campo
  obrigatório, exigência condicional, paths e defaults, admissão, CREATE
  hidratado, UPDATE ordinário/versionado, isolamento do estado persistido e
  avaliação destacada de lote. Rejeições não executam hooks de negócio/save.
- `CustomFieldAdoptionHttpTest`: MockMvc com validador e lifecycle reais;
  omissão válida retorna `201`, violação retorna `400` com o campo identificado
  e o draft hidratado é aceito. O repositório da fixture é simulado.
- `P4CustomFieldAdoptionRegressionTest`: usa a facade, o serviço de definição e
  os serviços reais de gozo/venda com o validador nativo injetado. Cobre
  `ERG-21501`/`422` sem save para regra temporal, equivalência da omissão,
  hidratação dos dados do pai, rejeição de regra e pai de outro órgão (`403`).

Essas provas não equivalem a HTTP/JPA/PostgreSQL integrado dos três cenários.
A suíte HTTP também inclui os 13 testes existentes de projeção de violações.

## Gates executados com Java 21

| Gate | Resultado | Evidência |
| --- | --- | --- |
| Starter: regressão, lifecycle, schemas, P2/P4 e versão | 84 testes; zero falhas, erros ou ignorados; `BUILD SUCCESS` às 08:09:17 −03 | [Log](evidence/p4-r1-r3-fix/core-tests.txt) |
| Starter: HTTP focal | 16 testes; zero falhas, erros ou ignorados; `BUILD SUCCESS` às 08:16:51 −03 | [Log](evidence/p4-r1-r3-fix/core-wire-tests.txt) |
| Showcase: `mvn test "-Dtest=!*Neon*"` | 357 testes; zero falhas, erros ou ignorados; `BUILD SUCCESS` às 08:17:23 −03 | [Log](evidence/p4-r1-r3-fix/showcase-tests.txt) |
| Auditor estrito do host | exit 0; 25/25 recursos; `STATIC PASS` | [Scorecard integral](evidence/p4-r1-r3-fix/resource-completeness.md) |
| `CustomFieldsResourceE2ETest` existente | 5 testes ignorados pela condição da fixture Neon; não contabilizados nos testes aprovados | [Log](evidence/p4-r1-r3-fix/core-http-tests.txt) |
| Instalação Maven local | `mvn -q install -DskipTests`, exit 0 | Candidato local identificado abaixo |

Comandos focais do starter:

```powershell
mvn test "-Dtest=CustomFieldAdoptionRegressionTest,CustomFieldJsonLogicTest,CustomFieldValidatorTest,CustomFieldSpecContractTest,CustomFieldSchemaProjectionTest,CustomFieldResourceLifecycleTest,P2CustomFieldContextTest,P4ResourceAdoptionContractTest,VersionedBaseResourceServiceTest,AbstractBaseResourceServiceTest"
mvn test "-Dtest=CustomFieldAdoptionHttpTest,RuleViolationHttpTest"
```

Comando do auditor, executado na raiz do monorepo:

```powershell
python scripts/workspace/audit-java-resource-completeness.py praxis-ergon-showcase --strict
```

## Scorecard de completude

Contagens do auditor desta execução; variedade de filtros e resultados por
recurso constam no scorecard integral. Os testes nativos de metadata continuam
incluídos nos 357 testes do host.

| Critério | Resultado |
| --- | --- |
| Colunas da entidade vs. campos nos DTOs | 616 colunas de negócio; cobertura de 100% nos 25 recursos; 773 campos de resposta, incluindo projeções |
| Destino de escrita | 15 contratos mutáveis contabilizados; demais recursos somente leitura |
| QuickViewCols | 133 |
| RichIdentity | 25/25 |
| ResourceReference com preview | 193/193 |
| MicroVisualization | 23; expressões verificadas pelo teste nativo |
| RichEntityLookup | 23 fontes; inventário de publicação e subcontratos ricos verificados pelo teste nativo |
| EditorialIssues | 0 |
| Filterable / variedade | 435 campos; matriz de operadores por recurso no scorecard integral |
| QuickFilter | 90 |
| ResourceIntent / WorkflowAction | 24 / 73 |
| BulkEditable / BulkOps | 233 / 41 |
| ResourceFact | 70 |
| UiAnalytics | 65 |
| ReactiveDet | 14 |
| Invariantes/determinações testadas | Suíte não-Neon do host aprovada; regressões positivas/negativas específicas de R1–R3 adicionadas; nenhuma certificação de runtime remoto |

`STATIC PASS` atesta os gates de fonte aplicáveis, incluindo os checks de
completude. Não certifica integração, concorrência/rollback em banco remoto,
paridade integral com o legado, browser, publicação ou implantação.

## Candidato, proveniência e superfícies afetadas

O candidato mantém `praxis-metadata-starter:8.0.0-rc.188` e
`praxis-rules-engine:0.1.0-beta.17`, ambos pré-release. O engine não foi alterado.
O JAR local do starter usado pelo host tem SHA-256:

```text
8bccfd844b4691be4950c526237a8c975dffc8d25fa214162d25851879b2ae78
```

Os cinco fontes canônicos envolvidos foram conferidos contra o sources JAR
instalado. O [snapshot de entradas](evidence/p4-r1-r3-fix/validated-inputs.json)
registra os hashes dos fontes Java, configurações, SQL, POMs e JAR ao final dos
gates. Não se atribui todo o diff Git a esta correção: os dois worktrees já
continham alterações anteriores. Os backups iniciais e os patches incrementais
estão em `.codex/p4-r1-r3-fix-20261102`, fora de `target`.

Os guias canônicos `GOVERNED-RULE-BRIDGE.md`, `CORE-RESOURCE-SERVICE.md`, a
spec `custom-fields-and-resource-facts.md` e o changelog foram sincronizados.
Não houve alteração no shape público de `x-ui`, no dialeto do engine ou nas
rotas HTTP; não há artefato Angular, JSON Schema, landing ou corpus HTTP a
regenerar por estas correções. As skills de resource/host foram consultadas e
continuam encaminhando para essas fontes canônicas.

Não houve commit, push, tag, publicação, workflow remoto, execução Neon,
provisionamento de banco ou aplicação de migração no host.
