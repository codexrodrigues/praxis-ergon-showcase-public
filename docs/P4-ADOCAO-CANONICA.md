# P4 — Adoção canônica das Ondas 1–3

As correções posteriores dos três achados da revisão estão registradas em
[Correções P4 R1–R3 — 02/11/2026](CORRECOES-P4-R1-R3-2026-11-02.md), com os
gates e a identificação do candidato local validado.

Implementação local de 01/11/2026. O host usa `praxis-metadata-starter:8.0.0-rc.188`
e `praxis-rules-engine:0.1.0-beta.17`. A linha continua pré-release; não houve tag,
publicação ou troca de major. O starter `.188` foi reconstruído e instalado
localmente com as correções estruturais descritas abaixo; isso não identifica
um binário publicado no Maven Central.

O inventário efetivo contém **25 recursos**, **15 mutáveis**, **14 controllers de
lote** e **41 operações de confirmação**. Setor continua somente de leitura.
Os nomes de serviços e a quantidade de recursos da proposta original não são
o inventário atual do checkout.

## Mudanças e fronteiras

| Área | Implementação final |
| --- | --- |
| Consulta | As duas bases Ergon delegam cursor, locate, seleção, filtros e exportação às bases resource-oriented. O host fornece identidade, ordenação, enriquecimento em lote, acesso por órgão e política CSV/JSON/Excel, cinco escopos e limite de 1.000 linhas. |
| Escrita | O template canônico controla versão, lock, flush e representação. Hidratação, mapper e regras dos serviços concretos passam por `applyResourceUpdate`, compartilhado com a avaliação destacada de lote. |
| Snapshots | Os serviços publicam seus estados e fatos por `getResourceStateSnapshot`; foram removidos `VinculoStateProvider` e `TempoLicencaState`. IDs inválidos e estados indisponíveis fecham a descoberta; leituras ordinárias preservam 403/404. |
| Comandos | Actions e PATCH usam os helpers versionados. `ErgonHttpContract` conserva o corpo direto e os headers das rotas de item existentes. Coleções filhas usam o helper canônico e conservam o erro de paginação 422 do host. |
| Recibos | `ResourceCommandReceiptsConfig` compõe `JdbcResourceCommandReceiptStore` com o datasource e transaction manager operacionais. `TempoLicencaCommandGateway` foi removido. |
| Regras | `ErgonRuleErrorCatalog.canonical()` adapta o catálogo atômico; `GovernedRuleEngineTemplate` assume compilação/cache/avaliação e `PraxisFactTreeBuilder` serializa valores. A exceção Ergon mantém o contrato 409/422 por composição, pois a exceção canônica é `final`. |
| Políticas vigentes | Férias usa `EffectiveDatedMatrixMatcher`; férias e licença usam `TriStateOverrideResolver`, mantendo precedência e códigos de rejeição de domínio. |
| Campos dinâmicos | `CamposCustomizadosProvider` projeta o armazenamento existente no único contrato `CustomFieldDescriptor.validationRules.jsonLogic`, publicado e avaliado pelo validador nativo. A ponte preserva ERG-21501/ERG-21502 e não cria outro executor. |
| Lote | Foram removidos `ErgonBulkSchema` e `ErgonBulkMessageConverter`. Controllers herdam as nove rotas; quatro overrides declarativos preservam os operationIds públicos. O engine é `JpaResourceBulkEngine`, com `service.bulkDomainAdapter`, políticas autenticadas e adapters dos comandos reais do host. |
| Builders | Lookups usam `directoryBuilder`/`standardDirectory`; determinações usam `forCrudResource`, mantendo as operações e regras existentes. |

O host conserva três especializações deliberadas: snapshots escalares destacam
JSON/embedded e não percorrem associações JPA; a atualização de gozo reconcilia
os pais antigo e novo; venda de licença mantém a ordem de locks
gozo → período → venda e a sincronização de abono. Essas regras não são um
segundo template genérico de CRUD. O instalador administrativo existente continua
delimitado à prova isolada; a publicação de operações no runtime usa descoberta
MVC canônica.

## Correções na fonte canônica

O consumo real exigiu ajustes em `praxis-metadata-starter`: hook de mapeamento
compartilhado, escopo obrigatório adicional de seleção de lote, conversão estrita
dos planos, reconhecimento de IDs explicitamente declarados, composição opt-in
bulk + duplicate-draft e configuração construtiva de versionamento. O recibo
JDBC recupera propriedades read-only da representação histórica sem alterar o
ObjectMapper global. A prévia limita os diagnósticos aos códigos utilizados do
catálogo permitido; o recibo de lote conserva `UNCHANGED` quando a versão não muda.

Os testes de protocolo verificam IDs inteiros, longos e compostos, os 41 bindings,
paths, schemas de avaliação, distinção entre avaliação e confirmação, rejeição
de chaves duplicadas e de identidades inválidas. O teste HTTP de duas fases usa
MockMvc e engine simulado; não deve ser interpretado como prova do lote completo
do host em banco remoto.

## Metadata e scorecard

O revisor semântico da versão adotada revelou referências ao próprio registro
em entradas sem `id` e repetição de campos entre cabeçalho e corpo do Quick View.
As referências de título passaram aos getters das respostas persistidas; os
metadataFields foram curados sem remover os campos executivos do corpo.
Descrições de negócio e os campos dos DTOs foram preservados. Essa correção muda
a curadoria `x-ui` do Quick View; não significa igualdade byte a byte dos schemas
anteriores. O resolvedor real é exercitado por `P4MetadataProjectionTest`.

O auditor reconhece os builders efetivamente concluídos e os getters públicos de
propriedades, preserva as annotations herdadas e continua rejeitando referências
sem propriedade/surface real. Não houve redução de critérios nem alteração do
`SemanticMetadataReviewer` nesta tarefa.

O [scorecard completo por recurso](evidence/P4-RESOURCE-COMPLETENESS.md) registra:

| Critério | Resultado |
| --- | --- |
| Colunas de negócio → DTO / destino de escrita | 616 colunas; 100% nos 25 recursos; 15 contratos mutáveis contabilizados |
| QuickViewCols / RichIdentity | 133 campos executivos; identidade rica em 25/25 |
| ResourceReference com preview / MicroVisualization | Todas as referências com preview; 23 microvisualizações |
| RichEntityLookup | 23 fontes confrontadas com inventário independente e subcontratos ricos no gate nativo |
| EditorialIssues | 0 |
| Filterable / variedade | 435 filtros; variedade por recurso no scorecard |
| QuickFilter / ResourceIntent / WorkflowAction | 90 / 24 / 73 |
| BulkEditable / BulkOps | 233 / 41 |
| ResourceFact / UiAnalytics / ReactiveDet | 70 / 65 / 14 |
| Invariantes e determinações | Suíte integral, incluindo contratos positivos/negativos de vínculo, dependente, frequência, afastamento, férias e tempo/licença |

## Validação e operação

| Gate executado | Resultado |
| --- | --- |
| `mvn test "-Dtest=!*Neon*"` no host, Java 21 | 353 testes, 0 falhas, 0 erros, 0 ignorados |
| `python scripts/workspace/audit-java-resource-completeness.py praxis-ergon-showcase --strict` | exit 0; 25/25 PASS, zero falhas nos checks aplicáveis |
| Regressões do auditor | 58 testes aprovados |
| Suíte focal do starter: serviços/versionamento, helpers, snapshots, bulk JPA/HTTP, custom fields e recibos JDBC | 69 testes, 0 falhas/erros; `mvn install -DskipTests` concluído |

O texto efetivamente emitido pelo auditor é **STATIC PASS**, não `STATUS:
APPROVED`. Seu escopo é estrutural; os testes JUnit são evidência separada. A
suíte inclui PostgreSQL embarcado do Slice 6 e a suíte focal do starter usa suas
fixtures JPA/JDBC; isso não constitui aceitação integrada de todos os recursos
do showcase em Neon, browser ou ambiente de publicação.

Fechamento da suíte do host: **01/11/2026, 21:29:21 −03**, `BUILD SUCCESS`.
Os 406 arquivos de entrada conferidos, incluindo fontes, POM e JAR instalado,
permaneceram inalterados durante essa execução final.

A migração nova é `V7__canonical_resource_command_receipts.sql`, posterior à V6
existente do ciclo de vida de tempo/licença. Cria
`praxis_resource_command_receipts`, nome real da tabela canônica, com instalação
explícita pelo owner. Flyway permanece desabilitado no startup. Nenhuma migração
foi aplicada ao banco do host ou ao Neon nesta tarefa; o deploy precisa incluir
a V7 antes de usar os comandos idempotentes. A V5 histórica permanece intacta.

As 267 entradas de worktree preexistentes foram registradas em
`target/p4-baseline/git-status.txt`; os arquivos iniciais em
`target/p4-baseline/tree` permitem comparar o estado inicial. As novas projeções
concorrentes dos cinco mappers do Slice 6 foram preservadas; a integração corrigiu
apenas seus caminhos de número documental para `numeroDocumento`, propriedade
real da entidade. O diff do checkout inclui esse trabalho concorrente e não deve
ser atribuído integralmente à P4. Não houve commit, push ou alteração de aceite
histórico. README, guia canônico de bulk e este scorecard
foram sincronizados. Os paths/operationIds públicos foram preservados; não há
artefato Angular, landing page ou corpus HTTP a atualizar para esta adoção do
host. Os ajustes de curadoria de metadata estão documentados acima.

A conferência final compara os hashes de fontes, POM e JAR antes/depois da suíte
em `target/p4-gate-source-manifest.json`, para detectar edições e instalações
concorrentes durante o gate.

Logs locais: `target/p4-showcase-tests.log`, `target/p4-core-tests.log`,
`target/p4-core-install.log`, `target/p4-auditor-tests.log` e
`target/p4-audit-final.txt`. O JAR local do starter validado tem SHA-256
`7657ef56812a4bee2d2f1f198710573cba71f217fdcb0dff5de794c3d53e3956`.
