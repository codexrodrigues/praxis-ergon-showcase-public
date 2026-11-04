# P5-B — Correções dos achados B01–B08

Data: 2026-11-02. Classificação: `contrato-publico` e `transversal`.
Este registro complementa a revisão ampliada de 2026-11-02,
preservada como evidência do candidato anterior. O recorte é a correção dos oito
achados, com validação local; não constitui revisão independente ou release.

**B01–B08 corrigidos; gates locais concluídos.**

## Correções e provas

| Achado | Dono canônico e correção | Regressão permanente |
| --- | --- | --- |
| B01 / P1 | O PATCH financeiro de abono vinculado usa o preparo nativo de custom fields e a projeção hidratada antes de alterar/salvar valor e observações. Mantém escopo, If-Match, locks e manutenção especializada do abono. | `P5BWriteAndScopeRegressionTest`: limite 100 rejeita valor 200 sem save, preserva 50; HTTP publica ERG-21502/target e aceita apuração 75 |
| B02 / P1 | O starter resolve o contrato no DTO de resposta confiável, sem condicionar validação ao marker JPA. Captura as definições uma vez e valida o resultado da mutação antes do save final. | `CustomFieldDomainMutationRegressionTest`: entidade sem marker; PUT e comando rejeitam valor 0; save não é chamado |
| B03 / P1 | Os nove outputs informativos da Slice 5 declaram `@JsonProperty(READ_ONLY)`. Continuam no schema para renderização/binding, mas o servidor ignora seu conteúdo no request. O `@Null` defensivo recebe null e os campos graváveis conservam a validação. | `P5BPreviewTransportTest`: POST/PUT dos quatro DTOs aceitam prévias preenchidas, ignoram todos os nove valores e rejeitam `diasGozo=-1`; serializer oficial preservado |
| B04 / P2 | A venda publica sua identidade somente leitura no contexto do formulário. A determinação envia o ID opcional, e o servidor verifica órgão, período e modalidade antes de recompor o consumo anterior. CREATE usa contexto nulo; canceladas/estornadas não recebem crédito novamente. | `P5BSalePreviewRegressionTest`: 90/60/30 em EDIT, aumento/redução, excesso, CREATE, cancelada, ID ausente, outro órgão/período e abono vinculado; compiler das 14 determinações |
| B05 / P2 | `prepareDomainMutation` distingue resultado confiável do servidor e atribuição recebida do cliente. Permite determinações read-only do callback; requests seguem as guardas read-only existentes. | `CustomFieldDomainMutationRegressionTest` e `CustomFieldAdoptionRegressionTest`: hook e comando podem produzir audit=42; PUT recebido com audit alterado falha antes do hook/save |
| B06 / P2 | O comando não copia automaticamente a entidade inteira. Somente regras que observam `entity` capturam seus fatos escalares e o container JSON anterior, sem travessia de associações; regras de valor dispensam captura. | `CustomFieldDomainMutationRegressionTest`: associação cujo getter falha não é lida; fatos/Map anteriores não sofrem aliasing; definições lidas uma vez; comando pode reparar valor antigo inválido |
| B07 / P2 | O helper de PATCH usa os nomes e membros resolvidos pelo ObjectMapper HTTP. Os quatro escritores da Slice 6 fornecem o mapper do host. Preserva rename, alias admitido, naming strategy, acrônimos e valores explícitos. | `ResourceStateCopySupportTest`: `externalNote`, alias, snake_case, `getURL()`/`url`, omissão/null/false/zero e cópia independente de coleção |
| B08 / P2 | O starter oferece uma guarda de coleção, com default individual. O host carrega memberships de dependentes em lotes limitados a 1000 titulares, sem cache entre chamadas. Página, cursor, includeIds e export usam a fronteira; SQL scope e lookup individual permanecem. | `P5BWriteAndScopeRegressionTest`: página de 100 linhas faz uma consulta de membership; troca de órgão e acesso individual negado continuam protegidos |

No B02, um contrato de escrita com custom fields exige que sua projeção confiável
publique esse container. Se faltar o marker no DTO de resposta, a base falha
explicitamente, sem omitir a validação. A entidade JPA continua sem obrigação de
marker para regras de valor/resultado. O overload que pede explicitamente
`previous/current` continua sujeito ao contrato próprio de snapshot; não é usado
implicitamente para validar todo comando.

Os comandos devem ser chamados pelo método público transacional do host. Uma
rejeição após o callback deve reverter seus efeitos de banco, inclusive flushes.
O helper não autoriza efeitos externos fora dessa transação. Essa fronteira já
é parte do contrato do executor e não foi convertida em um mapper genérico de
requests para entidades.

## Semântica de prévia e UX

Read-only de apresentação, proteção de atribuição e valor informativo são
conceitos distintos. Os outputs informativos são ignorados na desserialização
da escrita e recalculados pelo servidor; o frontend não precisa adivinhar nomes
de campos ou remover genericamente todo read-only. Custom fields protegidos
continuam permitindo seu round-trip inalterado em UPDATE, conforme o starter.

A identidade da venda é carregada pelo servidor e não é atribuída pelo POST/PUT.
Seu binding de determinação é contexto, sem adicioná-lo aos gatilhos de mudança.
O runtime oficial foi exercitado em CREATE com ID nulo e EDIT com ID carregado,
inclusive controle desabilitado. Não se aceita um crédito anterior calculado no
cliente. A estimativa não reserva saldo nem autoriza retificação de estado.

Linhagem de negócio mantida: `PCK_ERG_VENDA_LICESP` para consumo de vendas,
`PCK_LIC_ESP` para distinção de abono vinculado e `FERIAS.tab`/`peraqfer.tab`
para as projeções da Slice 5, sob
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps`.
A política já investigada de duplicate-draft de abono pecuniário foi conservada.
Não há alias ou novo campo `abonoConstitucional` na licença especial.

## Gates

| Gate | Resultado |
| --- | --- |
| Starter: custom fields, lifecycle, cópia de estado/PATCH, core JPA/H2, keyset, executor, handler HTTP e adapter bulk | **97 testes aprovados; zero falhas, erros ou ignorados**; candidato instalado no Maven local |
| Showcase: `mvn test "-Dtest=!*Neon*"` | **404 testes aprovados; zero falhas, erros ou ignorados**, conclusão às 21:57:38 −03 |
| Reconfirmação com os JARs preservados em `validated-dependencies/`, via JUnit Platform | **97 testes do starter e 404 do host aprovados**, zero falhas/erros/ignorados; mesmos recortes dos gates Maven acima |
| Angular: Karma/Chrome Headless, runtime de determinações e submit payload | **57 specs aprovadas**, incluindo o contexto readonly opcional de CREATE/EDIT |
| Auditor de completude `--strict` | **25/25 STATIC PASS; zero EditorialIssues**, reconfirmado contra o sources.jar preservado |

Evidências locais: [starter](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/starter-focal-install.log),
[host](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/showcase-gate.log),
[starter com JAR preservado](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/starter-pinned-all-regressions.log),
[host com JAR preservado](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/showcase-pinned-regressions.log),
[Chrome](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/angular-form-gate.log),
[scorecard por recurso](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/resource-completeness-pinned.md)
e [manifesto de entradas/delta](D:/Developer/praxis-plataform/.codex/p5-b-fixes-20261102/validated-inputs.json).

Comandos conclusivos:

```powershell
# praxis-metadata-starter, Java 21
mvn install "-Dtest=CustomFieldDomainMutationRegressionTest,CustomFieldAdoptionRegressionTest,CustomFieldResourceLifecycleTest,ResourceStateCopySupportTest,CoreResourceEvolutionIntegrationTest,ResourceKeysetSupportTest,GovernedResourceCommandExecutorTest,RuleViolationHttpTest,JpaResourceBulkEngineTest"
# praxis-ergon-showcase, Java 21
mvn test "-Dtest=!*Neon*"
python ../scripts/workspace/audit-java-resource-completeness.py . --strict --metadata-sources ../.codex/p5-b-fixes-20261102/validated-dependencies/praxis-metadata-starter-8.0.0-rc.188-sources.jar
# praxis-ui-angular, instalação Windows
cmd.exe /c npm run test:form -- --include=projects/praxis-dynamic-form/src/lib/services/reactive-determination-runtime.service.spec.ts --include=projects/praxis-dynamic-form/src/lib/utils/prepare-submit-payload.spec.ts --browsers=ChromeHeadless
```

As execuções e tentativas ficam em `.codex/p5-b-fixes-20261102/`, na raiz da
plataforma. O candidato inicial de 96 testes do starter e 404 do host foi
aprimorado para cobrir também acrônimos no helper. A primeira campanha ampla do
starter foi interrompida ao alcançar testes operacionais Bulk/PostgreSQL fora
do recorte; não recebe aceite. O primeiro teste read-only incompatível com B05
foi corrigido mantendo o teste negativo de atribuição pelo cliente.

O primeiro auditor não reconheceu a factory que devolvia um builder. A factory
agora termina a construção e devolve definições imutáveis, com inputs de contexto
explícitos; o gate não foi enfraquecido. O compiler nativo verifica 14 definições
e 28 projeções CREATE/EDIT com operações e DTOs reais.

## Scorecard de completude

| Critério | Resultado estrutural verificado |
| --- | --- |
| Colunas da entidade × campos de resposta | 616 / 773; 100% contabilizadas em 25 recursos |
| QuickViewCols / RichIdentity | 133 / 25 de 25 |
| ResourceReference com preview | 193 / 193 |
| MicroVisualization / RichEntityLookup | 23 / 23 fontes ricas |
| EditorialIssues | 0 |
| Filterable / variedade | 435; 4–16 operadores por recurso |
| QuickFilter | 90 |
| ResourceIntent / WorkflowAction | 24 / 73 |
| BulkEditable / BulkOps | 233 / 41 |
| ResourceFact / UiAnalytics | 70 / 65 |
| ReactiveDet | 14, em ambos os modos |
| Invariantes/Determinações testadas | Suites nativas e regressões acima; sem equiparar estrutura a integração |

## Procedência e limites

A snapshot inicial preserva 2272 arquivos de entrada do host, starter e consumidores
Angular relevantes. O delta desta execução é comparado com essa snapshot; o diff
contra HEAD contém trabalho anterior e não é atribuído integralmente à P5-B.
Os complementos P5-A já presentes foram preservados. Esta execução não substitui
a avaliação própria de `P5-A-CORRECTIONS-2026-11-02.md`.

O manifesto distingue também alterações concorrentes observadas em arquivos
Bulk/testes P5-A e em um spec de core Angular. Essas diferenças não foram
revertidas nem atribuídas a estas correções.

Coordenadas locais pré-release: engine `0.1.0-beta.17` e metadata
`8.0.0-rc.188`. Durante o último gate Maven do host, o metadata tinha SHA-256
`331590a814eb57b374a1160d3b6fecab6ff01260bd7c2ebeffb27c3253bf4e1b` e o engine
tinha o hash abaixo; ambos permaneceram estáveis nesse gate. Depois, outra
execução substituiu o metadata no Maven local. Foram preservadas cópias dos
JARs e repetidos os mesmos recortes de 97/404 testes com JUnit Platform,
substituindo no classpath a dependência compartilhada pela cópia abaixo.
O diretório `target/classes` do starter também foi substituído pelo JAR nessa
reconfirmação. O auditor foi repetido com o sources.jar preservado.

| Artefato | SHA-256 |
| --- | --- |
| Metadata `.188`, cópia reconfirmada | `bb971002e2ab1cc278861f24f6745095a839b90c83e59225880a5a7458c80c45` |
| Engine `.17` | `e7ae99edde4350389e81ede78c3c140f56f1343cc21531f02e8a7e267e9cc13b` |
| Metadata sources `.188` | `b6f948d49dca7b65ea1c89b3042006dee21daf8cadbe61bbe33f1f5c47eee2c3` |

Os launchers `PinnedAllStarterTests.java`/`PinnedCandidateTests.java`, classpaths
e logs ficam junto ao manifesto. Usam os testes previamente compilados pelo
Maven e o launcher oficial de JUnit; não substituem o build. Mudanças posteriores
no `.m2` não alteram essas cópias nem a identidade da evidência registrada.

O starter é instalado somente no Maven local para que o host consuma o candidato.
Não houve publicação, tag, commit, push ou workflow remoto. Os testes HTTP citados
são MockMvc; a prova de transporte usa os DTOs reais, e a prova financeira usa o
serviço real com persistência simulada. O gate do host inclui sua fixture
PostgreSQL local existente, sem certificar todos os fluxos, concorrência ou IAM.
O query count de dependentes mede chamadas ao EntityManager; não foi feita nova
medição de latência/SQL em um host implantado.

Não houve Neon, implantação, jornada visual completa no showcase, AXE ou aceite
de produção. As 57 specs em Chrome exercitam o runtime oficial, não uma tela
corporativa implantada. A narrativa dos READMEs/índice do starter foi revisada;
os detalhes mudaram no changelog, especificações e guia de serviço. Não há novo
dialeto x-ui, rota, export Angular ou contrato de config que exija regenerar
landing, registry, corpus HTTP ou pacotes frontend. A única mudança Angular é
uma regressão de teste; configurações, dependências e bundles de produção não
foram editados.
