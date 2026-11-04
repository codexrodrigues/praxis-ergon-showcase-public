# Inventário local de fatos — Fase 1

Data: 04/11/2026. **Censo técnico e classificação candidatos; não é schema de runtime nem aceite de G1.**

Inventário dos pontos de escrita encontrados nas superfícies examinadas, com expressão, tipo Java, classificação, destino e hash de fonte no [JSON](fact-source-inventory.json). As limitações constam no próprio artefato. Nenhuma descrição de negócio foi preenchida por heurística.

**Evolução da curadoria:** o [pacote de chefias](curated/README.md) materializa 39 schemas/bindings de avaliação para quatro famílias, 55 paths de produto/fábrica com migração explícita e 19 casos semânticos executáveis. O censo abaixo permanece uma classificação candidata; no escopo curado, as decisões detalhadas do novo pacote prevalecem sobre sugestões genéricas de destino. G1 global continua aberto.

| Superfície | Pontos de escrita |
| --- | ---: |
| DependenteElegibilidadeFacts.java | 77 |
| DesignacaoFuncaoFacts.java | 14 |
| ErgonRuleEngineFacade.java | 251 |
| FeriasFacts.java | 276 |
| FrequenciaAfastamentoFacts.java | 180 |
| OperationalRuleSet.java | 1 |
| ProvaHorariosAcumulacao.java | 7 |
| Rn013RegrasFrequenciaFacts.java | 37 |
| SubstituicaoFuncaoFacts.java | 25 |
| VinculoProvimentoFacts.java | 77 |
| AverbacaoTempoServicoService.java | 0 |
| ErgonDeterminationService.java | 0 |
| ErgonInvariants.java | 0 |
| LicencaEspecialGozoService.java | 0 |
| PeriodoAquisitivoLicencaEspecialService.java | 0 |
| TempoLicencaLifecycle.java | 21 |
| TempoLicencaPreviewService.java | 0 |
| TempoLicencaRuleFacts.java | 42 |
| TempoLicencaValidation.java | 47 |
| VendaLicencaEspecialService.java | 1 |

| Classificação | Ocorrências |
| --- | ---: |
| BUSINESS_PREDICATE | 37 |
| CONTEXT | 20 |
| CUSTOM_FIELD_ADAPTER | 147 |
| DECLARED_DYNAMIC_SHAPE | 3 |
| DETERMINISTIC_CALCULATION | 34 |
| NEIGHBORHOOD_AGGREGATE | 33 |
| OUTSIDE_FACT_SNAPSHOT | 28 |
| POLICY_RESOLUTION | 84 |
| POSITIONAL_FACTORY | 1 |
| PRIMITIVE | 628 |
| STRUCTURAL_PRESENCE | 27 |
| STRUCTURE | 14 |

## Famílias governadas existentes

O construtor real do host foi instanciado sem contexto Spring/banco, após `mvn -DskipTests compile`. O censo inclui o produto e a composição de fábrica do órgão 2; não é leitura de planos/head persistidos.

| RuleSet | Bindings produto | Paths produto | Customizações de fábrica órgão 2 |
| --- | ---: | ---: | ---: |
| `ergon.administracao-pessoal.cargos.cadastro` | 8 | 11 | 1 |
| `ergon.administracao-pessoal.funcionarios.cadastro` | 8 | 11 | 1 |
| `ergon.administracao-pessoal.dependentes.elegibilidade` | 12 | 35 | 3 |
| `ergon.administracao-pessoal.vinculos.provimento` | 8 | 55 | 3 |
| `ergon.administracao-pessoal.regras-frequencia.rn013` | 15 | 15 | 3 |
| `ergon.administracao-pessoal.frequencias.lancamento` | 17 | 47 | 3 |
| `ergon.administracao-pessoal.afastamentos.concessao` | 17 | 61 | 3 |
| `ergon.administracao-pessoal.parametros-contagem-ferias.matriz` | 32 | 40 | 3 |
| `ergon.administracao-pessoal.periodos-aquisitivos-ferias.aquisicao` | 24 | 51 | 3 |
| `ergon.administracao-pessoal.ferias-gozo.concessao` | 34 | 65 | 3 |
| `ergon.administracao-pessoal.averbacoes-tempo-servico.cadastro` | 15 | 16 | 2 |
| `ergon.administracao-pessoal.periodos-aquisitivos-licenca-especial.cadastro` | 22 | 32 | 2 |
| `ergon.administracao-pessoal.licencas-especiais-gozo.cadastro` | 19 | 42 | 2 |
| `ergon.administracao-pessoal.vendas-licenca-especial.cadastro` | 13 | 22 | 2 |
| `ergon.administracao-pessoal.funcoes.chefia` | 3 | 6 | 1 |
| `ergon.administracao-pessoal.vagas-funcao.chefia` | 4 | 7 | 1 |
| `ergon.administracao-pessoal.designacoes.chefia` | 8 | 13 | 1 |
| `ergon.administracao-pessoal.substituicoes.chefia` | 15 | 24 | 2 |

## Regras operacionais posicionais

As 38 definições e 50 chamadas foram extraídas da AST. Para cada chamada, o JSON conserva o enum, os argumentos e tipos; datas hoje convertidas para epochDay devem migrar com o consumidor. Os argumentos não autorizam inferir proveniência ou nulabilidade por rótulo.

| Enum / código | Paths atuais |
| --- | --- |
| `CAMPO_SELECT` / CERG-09051 | `draft.fieldType`, `draft.optionTabela` |
| `CAMPO_ORDEM` / CERG-09002 | `draft.ordem` |
| `FERIAS_VINCULO` / CERG-09003 | `periodoAquisitivo.pessoa`, `draft.pessoa`, `periodoAquisitivo.vinculo`, `draft.vinculo` |
| `PREVIA_FRACAO_ABONO` / CERG-09009 | `draft.numerador`, `draft.denominador` |
| `LICENCA_ESTADO_EDICAO` / CERG-09012 | `current.situacao`, `draft.situacao` |
| `LICENCA_PERIODO_EDICAO` / CERG-09013 | `current.periodo`, `draft.periodo` |
| `AVERBACAO_CRIAR` / CERG-09014 | `draft.dias`, `draft.desaverbacao`, `draft.remocao`, `draft.motivoRemocao`, `draft.motivoDesaverbacao` |
| `AVERBACAO_ATO_EDICAO` / CERG-09015 | `current.diasDesaverbados`, `draft.diasDesaverbados`, `current.dataRemocao`, `draft.dataRemocao`, `current.dataDesaverbacao`, `draft.dataDesaverbacao`, `current.motivoDesaverbacao`, `draft.motivoDesaverbacao`, `current.motivoRemocao`, `draft.motivoRemocao` |
| `AVERBACAO_RETIFICACAO` / CERG-09016 | `current.situacao`, `current.totalDias`, `draft.totalDias`, `current.diasFinalidade`, `draft.diasFinalidade`, `current.finalidade`, `draft.finalidade`, `current.tipoTempo`, `draft.tipoTempo`, `current.instituicao`, `draft.instituicao`, `current.dtini`, `draft.dtini`, `current.dtfim`, `draft.dtfim` |
| `GOZO_CRIAR` / CERG-09017 | `draft.fim` |
| `GOZO_ENCERRAMENTO_EDICAO` / CERG-09018 | `current.fim`, `draft.fim` |
| `GOZO_RETIFICACAO` / CERG-09019 | `current.situacao`, `current.dtini`, `draft.dtini`, `current.dtprevfim`, `draft.dtprevfim`, `current.diasGozo`, `draft.diasGozo`, `current.abonoPecuniario`, `draft.abonoPecuniario`, `current.diasAbono`, `draft.diasAbono`, `current.iniFimAbono`, `draft.iniFimAbono` |
| `PERIODO_CRIAR` / CERG-09020 | `draft.dias`, `draft.data` |
| `PERIODO_CONVERSAO_EDICAO` / CERG-09021 | `current.diasConvertidosDobro`, `draft.diasConvertidosDobro`, `current.dtCntDobro`, `draft.dtCntDobro` |
| `PERIODO_VINCULO_ORIGEM` / CERG-09022 | `averbacaoOrigem.pessoa`, `draft.pessoa`, `averbacaoOrigem.vinculo`, `draft.vinculo` |
| `PERIODO_NOVA_ORIGEM` / CERG-09023 | `current.origem`, `draft.origem`, `averbacaoOrigem.situacao`, `averbacaoOrigem.dataRemocao`, `averbacaoOrigem.finalidade` |
| `VENDA_CRIAR_DATAS` / CERG-09024 | `draft.pagamento`, `draft.estorno` |
| `VENDA_CRIAR_MODALIDADE` / CERG-09025 | `draft.gozo`, `draft.modalidade` |
| `VENDA_DATAS_EDICAO` / CERG-09026 | `current.dtPagamento`, `draft.dtPagamento`, `current.dtEstorno`, `draft.dtEstorno` |
| `VENDA_ABONO_ORIGEM` / ERG-03387 | `draft.gozo`, `context.operacao`, `current.mesAnoFolha`, `draft.mesAnoFolha`, `current.numFolha`, `draft.numFolha`, `current.documentoLegalId`, `draft.documentoLegalId`, `current.atoPublicacaoId`, `draft.atoPublicacaoId` |
| `VENDA_PERIODO_ORIGEM` / CERG-09027 | `gozo.periodo`, `draft.periodo` |
| `VENDA_MODALIDADE_ORIGEM` / CERG-09028 | `draft.modalidade` |
| `VENDA_DIAS_VALOR` / CERG-09029 | `draft.gozo`, `draft.situacao`, `draft.dias`, `draft.valor` |
| `VENDA_PATCH_ESTORNO` / CERG-09030 | `current.situacao` |
| `VENDA_RECONCILIAR_ORIGEM` / CERG-09031 | `current.gozo` |
| `VENDA_RECONCILIAR_DATA` / CERG-09032 | `current.estorno`, `draft.data` |
| `VENDA_RECONCILIAR_GOZO` / CERG-09033 | `gozo.situacao`, `gozo.abono` |
| `VENDA_RETIFICACAO` / CERG-09034 | `current.situacao`, `current.diasVendidos`, `draft.diasVendidos`, `current.valorIndenizacao`, `draft.valorIndenizacao`, `current.mesAnoFolha`, `draft.mesAnoFolha`, `current.numFolha`, `draft.numFolha` |
| `PERIODO_SALDO_CONTABIL` / CERG-09035 | `draft.direito`, `draft.gozo`, `draft.vendido`, `draft.convertido` |
| `GOZO_ABONO_UNICO` / CERG-09036 | `neighborhood.abonosAtivos` |
| `AVERBACAO_REDISTRIBUICAO` / CERG-09037 | `neighborhood.periodos` |
| `PREVIA_AVERBACAO` / CERG-09038 | `draft.total`, `draft.dias` |
| `PREVIA_CONVERSAO` / CERG-09039 | `draft.direito`, `draft.dias` |
| `PREVIA_GOZO_INTERVALO` / CERG-09040 | `draft.inicio`, `draft.fim` |
| `PREVIA_GOZO_LIMITE` / CERG-09041 | `draft.dias` |
| `PREVIA_VENDA_SALDO_APURADO` / CERG-09042 | `periodoAquisitivo.saldo` |
| `PREVIA_VENDA_CONSUMO` / CERG-09043 | `current.dias` |
| `PREVIA_VENDA_LIMITE` / CERG-09044 | `draft.dias`, `periodoAquisitivo.saldo` |

## Destinos que exigem atenção na implementação

- `effectiveParametroConta`, `effectiveStatutoryRule` e `politica` misturam seleção de políticas, defaults e overrides. A migração deve publicar candidatos primitivos e preservar a precedência no dono da decisão; mover o objeto para `context` não resolve essa mistura.
- `current` precisa ser construído a partir do persistido em UPDATE/DELETE. `current.criacao` e `current.edicaoManual` são contexto de execução; mapas incompletos não demonstram OLD.
- `DesignacaoFuncaoFacts` e `SubstituicaoFuncaoFacts` projetam só `numfunc` em partes de chaves compostas. Preservar também `numero`/`numvinc` e o tenant na identidade completa.
- `ProvaHorariosAcumulacao` publica objetos e arrays aninhados, datas civis, instantes, duração e fuso. Não transformar instantes em datas civis nem supor que há fonte de horários autorizada para todo par.
- `Rn013RegrasFrequenciaFacts.ruleParams` é mapa dinâmico: o schema deve declarar chaves/tipos admitidos; reflexão ou `additionalProperties: true` não satisfazem o contrato.
- O adapter `toSafeEntityNode` para CustomFieldValidator é outra superfície, com chaves dinâmicas e OLD próprio. Não contar seus campos como cobertura automática de `current` do RuleSet.
- Referências a `existingOverlaps` nas raízes declaradas e `existingRules` no provider de parâmetros precisam ser reconciliadas pelo consumo real dos executores; raízes disponíveis não equivalem a folhas usadas.

## Reprodutibilidade e limites

O extrator de investigação `OntologyFactInventoryProbe.java`, o censo das famílias e os logs ficam junto ao blueprint na pasta de investigação original. A análise retornou zero erros de javac. As fontes são fixadas por SHA-256 no JSON; qualquer alteração exige reextração e revisão das classificações.

A cobertura dos pontos de escrita não fecha sozinha G1: ainda são necessários os schemas completos por operação, decisões finais dos caminhos/políticas e o inventário do ambiente-alvo quando escolhido. Não avançar providers nem heads com base neste censo.

## Ambiente operacional, conexões Neon, escopo de governança e estado de snapshots

1. **Conexões efetivas (Projetos Neon, branches, databases e schemas):**
   - `ErgonPersistenceConfiguration.ConfigStore.configDataSource` exige que `CONFIG_DATASOURCE_URL` e `CONFIG_DATASOURCE_USERNAME` apontem para um database (`current_database()`) e usuário (`current_user`) distintos de `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME`.
   - O projeto Neon `praxis-config` (`ancient-leaf-72055027`, branch `production` / `br-aged-lake-a4eb715l`, database `neondb`) pertence ao `praxis-api-quickstart` (onde estão as 14 definições `desenv`/`local` do Quickstart) e **não** é usado pelo Ergon.
   - O `praxis-ergon-showcase` usa o projeto Neon **`holy-mouse-40140108`**:
     - `main` (`br-bitter-bonus-b7i0vfko`): database `neondb` (`public`: 16 tabelas base).
     - `issue-305-host-proof-20261027` (`br-super-sound-b7urizmd`): database `neondb` (`public`: 16 tabelas + `praxis_bulk`: 32 tabelas; branch atribuída sem TTL, protegida contra mutações diretas de suíte).
     - `ps3b-gate-20261103` (`br-purple-dust-b70ss3dj`): database de domínio **`ps3b_ergon`** (`public`: 30 tabelas até V10 + `praxis_bulk`: 32 tabelas) e database Config **`ps3b_config`** (`public`: 66 tabelas Config + `ps3b_tx_probe`).
2. **Escopo de governança (`tenantId`, `environment`, `serviceKey`):**
   - `tenantId` Config = código decimal do órgão (`empCodigo`) como String (`"1"`, `"2"`, `"3"`, `"7"`).
   - `serviceKey` / `ownerServiceKey` = `"praxis-ergon-showcase"`; `hostContractVersion` ativo = `"ergon-policy-studio/1.0"` (candidato da migração ontológica: `"ergon-policy-studio/2.0"`).
   - A autoridade `ERGON_CONFIG_SCOPE:<organization>:<environment>` é emitida a partir de `showcase.security.config-principals` e validada por `ErgonConfigAccess`.
3. **Estado atual no Config e consumidores reais:**
   - Em `ps3b_config` (`br-purple-dust-b70ss3dj`), existem **`0` snapshots** (`domain_rule_snapshot`), **`0` heads ativos** (`domain_rule_snapshot_head`) e **3 definições aprovadas de prova** (`tenant_id='2'`, `environment='stage'`, `service_key='praxis-ergon-showcase'`, incluindo `customer.orgao2.replace.chefia.erg-01154` de `Ps3bConfigNeonTest`).
   - A ausência de snapshots/heads ativos persistidos é esperada porque o host opera hoje com as composições de fábrica em memória (`productRuleSet` + `orgao2FactoryRuleSet` em `ErgonRuleEngineFacade`), `src/main/resources/policy-studio/scenarios.json` e o fluxo de preparação/admissão (`ErgonPolicyComposition.prepare` / `admitGovernedSnapshot`).
4. **Migrações, execução local e branches de prova:**
   - Startup HTTP usa `spring.flyway.enabled=false` e `ddl-auto=validate` (porta padrão `8080`; UI Ergon em `http://127.0.0.1:4303`; Policy Studio em `http://localhost:4302`).
   - Migrações de domínio: `src/main/resources/db/migration/V5__*.sql` a `V10__*.sql`.
   - Instalação de lote (`praxis_bulk`): `InstallBulkStorage.java`.
   - Instalação Config (`public` no banco Config separado): `InstallConfigStorage.java` (com `CONFIG_MIGRATION_USERNAME` distinto de `CONFIG_DATASOURCE_USERNAME`).
   - Provas Neon do Ergon usam apenas o projeto `holy-mouse-40140108` em branch descartável filha com TTL ≤ 24h, excluída após registrar as evidências em `docs/evidence/`.

