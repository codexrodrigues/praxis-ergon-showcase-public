# Policy Studio do Ergon Showcase

O catálogo em `/api/ergon/policy-studio` expõe os 18 conjuntos governados pela fachada do Ergon. O detalhe mostra o plano efetivo do órgão, a referência de produto, a linhagem catalogada e os slots de decisão. Guardas operacionais do host, hidratação de entidades, permissões de recursos e efeitos transacionais pertencem aos serviços do Ergon e ficam fora da simulação desses 18 conjuntos.

## Acesso por órgão

No showcase com HTTP Basic, o servidor emite as autoridades por órgão da conta operacional a partir de `SHOWCASE_POLICY_READ_ORGS` e `SHOWCASE_POLICY_PUBLISH_ORGS`. Os valores são listas de códigos separados por vírgula. Publicação também concede leitura do mesmo órgão. Valores vazios deixam a conta sem autorização de Policy Studio. Para um laboratório que consulte os quatro órgãos e publique em três, configure leitura em `1,2,3,7` e publicação em `2,3,7`. Cabeçalhos de tenant da sessão não concedem essas autoridades.

| Operação | Caminho relativo | Autoridades |
|---|---|---|
| Descobrir órgãos | `GET /organizations` | `ERGON_READ` |
| Listar | `GET /rule-sets?empCodigo=2` | `ERGON_READ` e leitura do órgão |
| Inspecionar | `GET /rule-sets/{ruleSetKey}?empCodigo=2` | `ERGON_READ` e leitura do órgão |
| Simular | `POST /simulate` | `ERGON_READ` e leitura dos órgãos informados |
| Simular proposta | `POST /rule-sets/{ruleSetKey}/orgaos/{empCodigo}/preview` | `ERGON_WRITE` e publicação no órgão |
| Customizar | `POST /rule-sets/{ruleSetKey}/orgaos/{empCodigo}/customize` | `ERGON_WRITE` e publicação no órgão |
| Restaurar | `POST /rule-sets/{ruleSetKey}/orgaos/{empCodigo}/reset` | `ERGON_WRITE` e publicação no órgão |

## Revisão e ativação

O detalhe retorna `ETag` e o mesmo token em `data.activationTag`. Prévia, customização e reset exigem o token exato em `If-Match`. A API retorna 428 para token ausente e 412 quando outra publicação ou reset mudou a ativação. A revisão muda mesmo se o reset recuperar um digest antigo.

A prévia compila a proposta e compara o plano ativo com o candidato. Envie `customization` e um de `facts` ou `presetScenarioKey`. A operação preserva o plano ativo. Uma customização válida atualiza imediatamente o plano aplicado pela fachada no órgão. O reset restaura o plano inicial, incluindo a composição de fábrica do Órgão 2 e as opções locais de idade aplicáveis ao cadastro de servidores.

- **Bypass** suspende o binding vigente de um slot substituível. Desligar o bypass do Studio reativa a composição que ele suspendeu, inclusive uma substituição anterior do órgão.
- **URN** mescla os valores enviados com os valores declarados da composição vigente e resolve o conjunto no template de produto. As faixas de RN‑013, dispensa de perícia, retroatividade e direito padrão de férias exigem mínimo menor ou igual ao máximo.
- **REPLACE_EXACT** admite o código de produto do slot, um código catalogado associado ao mesmo slot ou um código local com mensagem própria.
- **AUGMENT** adiciona ou atualiza restrições locais identificadas pelo seu código. Uma restrição inativa pode continuar registrada e ser removida pelo reset.

Uma proposta inválida retorna HTTP 422. Uma simulação concluída pode responder HTTP 200 com decisão `DENY`. As simulações recebem `facts` ou `presetScenarioKey`. Em `/simulate`, os órgãos de referência e alvo são 1 e 2 por padrão. A avaliação usa o plano ativo de cada órgão. O resultado informa o instante UTC e os digests dos fatos e dos planos. Nos diagramas, `DEPENDS_ON` identifica dependência de execução e `STAGE_SEQUENCE` identifica a transição visual.

## Escopo e linhagem

`CERG-06407` é a guarda protegida do estado do período aquisitivo. `CERG-06414` é a política substituível do showcase que exige o ato registrado na liquidação da venda avulsa. Para separar o que vem do legado do que é decisão do showcase, foram consultados `v7x/aps/ERG_VENDA_LICESP.tab` e `v7x/aps/fontes_ergon/pck_erg_venda_licesp.pkb`. A exigência do ato é uma política do showcase solicitada para o Policy Studio, sem atribuição a um código PL/SQL. O abono gerado a partir de gozo segue o fluxo financeiro vinculado já existente.

`ERG-01151` protege a cronologia da vaga e `ERG-01152` protege a consistência entre a situação da vaga e a designação do titular. Ambos os slots têm política `FORBIDDEN` para customização. `ERG-01154`, que exige função ativa para vaga vigente, ocupa um slot substituível. Os códigos `CERG-06304` e `CERG-07009` precisam de conciliação de domínio antes de qualquer adição. A matriz de parâmetros de férias usa a chave `ergon.administracao-pessoal.parametros-contagem-ferias.matriz`.

A ativação e o token são locais à instância. Um reinício restaura a configuração de fábrica. Para uso corporativo com réplicas, a trilha de evolução é integrar o Config Starter para snapshots persistentes, ativação coordenada, aprovação e histórico por autor.
## Contrato de admissão PS-3A

O Config possui autoria, aprovação, publicação e seleção. A fachada do Ergon
admite o snapshot verificado para o órgão e ambiente autorizados, compila com o
registry executável do host e devolve o candidato. A admissão preserva a seleção
vigente. O dono esperado do snapshot é `praxis-ergon-showcase` e o tenant Config
usa o código decimal canônico do órgão, por exemplo `2`.

O wiring recebe tenant, ambiente e ator do principal resolvido por
`DomainRuleGovernancePrincipalResolver` no modo corporativo. O host valida
acesso ao órgão e confronta seu código com o tenant autorizado. Cabeçalhos são
hints. A interface de admissão recebe escopo já autorizado e é usada depois da
verificação do Config. A autorização do Studio local permanece própria.

| Operação Config | Papel |
|---|---|
| Autoria de definição ou política de rollout | `RULE_DEFINITION_AUTHOR` |
| Aprovação de definição ou política de rollout | `RULE_DEFINITION_APPROVER` |
| Aprovação da composição exata | `RULE_COMPOSITION_APPROVER` |
| Preparação de manifesto e publicação | `RULE_SNAPSHOT_PUBLISHER` |
| Leitura de snapshot, head, política, readiness e histórico operacional | `RULE_SNAPSHOT_READER` |
| Observações de execução e probes de réplica | `RULE_EXECUTION_OBSERVER` |
| Operação de rollout, ativação de política, seleção e rollback | `RULE_SNAPSHOT_OPERATOR` |

Aplicar também as regras de autoria, segregação e aprovação dos serviços Config.
Os papéis e o contexto da conta autenticada permanecem independentes das
autoridades Ergon por órgão.

O adapter compara domínio, bounded context, chave e operação do RuleSet e
preserva slots, fatos, política de falha e bindings protegidos do produto. Para
compor uma publicação, consultar `DomainRuleSnapshotService.findMaximumRuleSetVersion`
no escopo autorizado e preparar uma versão inédita, seguindo o compositor do
Quickstart. Em caso de colisão, recompor e obter aprovação para o novo digest.
`RuleSetRef.version` deve corresponder à coluna persistida. Revisão da publicação
e revisão da seleção têm ciclos próprios.

O candidato governado transporta descritores do cliente em
`ruleSet.errorDescriptors`. A chave é o binding, que fixa o slot e código.
Usar códigos novos para descrições de cliente. O adapter recusa códigos
reservados do Ergon. Mensagens persistidas são lidas do snapshot verificado e
entram na projeção canônica de violações. O uso de descritores em memória pelo
Studio local pertence à implementação anterior ao wiring de PS-3B.

### Disponibilidade do contrato

A inspeção dos JARs do Maven Central em 03/11/2026 confirmou que
`praxis-config-contracts:0.1.0-beta.5` expõe `PublishedRuleSnapshotHeadReader.findActive`
e que `praxis-config-starter:0.1.0-rc.176` expõe `findMaximumRuleSetVersion`.
O Engine público `0.1.0-beta.17` ainda tem o construtor de `RuleSetDefinition`
sem descritores e não expõe `RuleSetRef.sameIdentity`. O verificador público
do Config antecede a vinculação dos descritores à composição aprovada.

A implementação em fonte de `ruleSet.errorDescriptors` permanece candidata.
Um JAR local com a mesma coordenada do Engine público tem conteúdo diferente.
A adoção corporativa requer versões públicas compatíveis do Engine e do Config,
com verificação de round-trip, dos hashes e da leitura downstream a partir
dessas versões. A evidência de um candidato local pertence ao gate de pré-release.

### Unidade Config no Ergon

O host usa `CONFIG_DATASOURCE_URL`, `CONFIG_DATASOURCE_USERNAME`,
`CONFIG_DATASOURCE_PASSWORD` e `CONFIG_DATASOURCE_SCHEMA` para a unidade
Config. Use um banco e um papel de runtime distintos dos do domínio. O pool
Config aplica o schema também às consultas JDBC e ao lock de snapshots.

Antes de iniciar o host, execute `InstallConfigStorage` com o caminho do JAR
Config selecionado e as dependências Flyway e PostgreSQL no classpath. O
instalador usa `CONFIG_MIGRATION_USERNAME` e `CONFIG_MIGRATION_PASSWORD`,
extrai somente as migrações oficiais daquele artefato e aplica a sequência
completa no datastore Config. Conceda ao runtime acesso ao schema, tabelas e
sequências de aplicação. Reserve a propriedade dos objetos e o histórico
Flyway ao migrador. O perfil do showcase mantém a ingestão automática de
catálogos e os provedores de IA desativados para este fluxo.

No Windows, a partir do diretório do host, gere o classpath com o mesmo
repositório Maven usado no pacote. Execute o instalador com Java 21 e informe
o JAR Config que será empacotado.

```powershell
mvn -B "-Dmaven.repo.local=$repo" dependency:build-classpath "-Dmdep.outputFile=target/config-classpath.txt"
java --class-path (Get-Content -Raw target/config-classpath.txt).Trim() `
  src/main/java/br/com/praxis/ergonshowcase/operations/InstallConfigStorage.java $configJar
```

No laboratório HTTP Basic, configure `showcase.security.config-principals`
como uma lista de contas distintas. Cada entrada declara `username`,
`password`, `organization`, `environment` e `roles`. Injete as senhas pela
configuração protegida do ambiente. A entrada concede acesso Config somente
ao órgão e ambiente declarados. Use contas distintas para os atos que exigem
segregação. As contas do domínio e do Bulk conservam suas autoridades próprias.
Configure `SHOWCASE_SECURITY_CONFIG_ORIGINS` com as origens autorizadas.
O ator vem da conta autenticada. Se o cliente
enviar hints de órgão, ator ou ambiente, eles precisam coincidir com o escopo
do servidor. A autorização de cada operação de regras permanece no Config.

A prova `Ps3bConfigNeonTest` exige `PS3B_NEON_PROOF=true` e os dois
datastores da branch de teste. No banco Config de teste, o migrador cria
`ps3b_tx_probe (id integer primary key)` e concede SELECT e INSERT ao runtime.
A prova usa a tabela apenas dentro de uma transação revertida.

### Preparação governada da composição

`POST /api/ergon/policy-studio/rule-sets/{ruleSetKey}/orgaos/{empCodigo}/composition-manifest`
recebe `sourceDefinitionIds`, `validFromUtc` e, quando aplicável,
`validUntilUtc`. O principal Config precisa de `RULE_SNAPSHOT_PUBLISHER` e
seu tenant deve coincidir com `empCodigo`.

Cada fonte Config aprovada ou ativa usa o tipo governado `form_rule` e
identifica o binding em `ruleKey`,
o RuleSet em `resourceKey`, o bounded context em `contextKey` e
`praxis-ergon-showcase` em `serviceKey`. A fonte declara
`governance.lifecycleBoundary=REFERENCE_DRAFT_ONLY`,
`parameters.hostContractVersion=ergon-policy-studio/1.0` e a customização
em `definition.ergonCustomization`. O binding produzido precisa corresponder
à condição aprovada. Nos slots parametrizados, o template do produto e
os valores declarados também precisam coincidir com a fonte.

| Modalidade | `ruleKey` esperado |
|---|---|
| `REPLACE_SLOT_EXPRESSION` | `customer.orgao{empCodigo}.replace.{slotKey}` |
| `UPDATE_PARAMETERS` | `customer.orgao{empCodigo}.parameters.{slotKey}` |
| `AUGMENT_SLOT` | `customer.orgao{empCodigo}.augment.{slotKey}.{ruleCode}` |
| `BYPASS_SLOT` habilitado | `customer.orgao{empCodigo}.bypass.{slotKey}` |

O conjunto de fontes precisa produzir bindings habilitados no candidato
resultante. Um pedido de desativação ou restauração por si só não fornece
uma fonte de binding.

A composição parte do perfil de fábrica do órgão e usa a maior versão
publicada no escopo para calcular a versão candidata. O Config valida as
fontes e devolve o manifesto e seu digest. O preparo pode ser repetido com
os mesmos inputs enquanto o escopo permanece igual. A versão candidata
só se torna ocupada na publicação canônica, que revalida o manifesto e a
unicidade. Os passos de aprovação da composição, publicação e seleção
ocorrem em operações próprias do Config.

