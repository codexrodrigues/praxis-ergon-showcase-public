# Scorecard — Bulking #308 (aceite pendente)

Fase 3A de `e6b9fa0`: PASS por aprovação explícita do responsável. Config/rules-engine é fase futura.

A campanha atual **não passou**. [Registro verificável](BULKING-308-CAMPAIGN.json) e [resultados dos testes](bulk-neon-test-results.txt). O JAR compila e responde health 200; reconciliação de publicação falha no prazo canônico e o consumidor Angular encontra 503. Os dois recibos CONFIRMED observados numa tentativa interrompida são evidência parcial, sem aprovação do protocolo inteiro.

O auditor abaixo é a versão vigente da raiz, incluindo evolução concorrente de governança editorial/analytics. As novas colunas BulkEditable/BulkOps estão presentes, mas o resultado global é FAIL: MicroVisualization/UiAnalytics ausentes em três recursos e EntityLookupDescriptor incompleto. Esses achados não foram apagados para obter PASS.

As 16 tabelas/392 colunas foram conferidas com hash estrutural idêntico; a branch descartável foi excluída com autorização, eliminando também as linhas técnicas. [Reconciliação dos campos ausentes](../RECONCILIACAO-CAMPOS-BULK-308.md): dois requisitos não têm coluna nem extensão governada existente; correio NOT NULL não admite CLEAR.

A prova usa candidato Maven privado do [PR Metadata #311](https://github.com/codexrodrigues/praxis-metadata-starter/pull/311), distinto da rc.179 ainda declarada no POM. Não há release ou aprovação de merge. O manifesto atual mantém os gates failed/pending; os recibos antigos permanecem históricos.

# Praxis Resource Completeness, Quick View & Editorial Quality Audit
- **Project**: `D:\Developer\praxis-plataform\praxis-ergon-showcase`
- **Mapped JPA Entities**: 16
- **Resource Controllers**: 6

- **Scope**: static source evidence; not HTTP, concurrency, parity or runtime approval. N+1 requires cold full-query/page/cursor/export measurements; this source audit cannot certify it.

## 1. Resource & Capability Scorecard
| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | `MicroViz` | `UiAnalytics` | BulkEditable | BulkOps | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
<!-- administracao-pessoal.cargos: functional-accountability=17/17; command-managed=dtExtincao -->
| `administracao-pessoal.cargos` | `Cargo` | 17 | 22 | 100.0% | 17/17 | 15 | 8 ops | 4 | 5 | YES | 5 (5) | 3 | 1 | 0 | 2 | 0 | 0 | 5 | 2 | 0 | **FAIL (Enterprise mutable resource (>=15 biz cols) missing @MicroVisualization; Enterprise mutable resource (>=15 biz cols) missing @UiAnalytics/@AnalyticsProjection)** |
<!-- administracao-pessoal.documentos-legais: functional-accountability=13/13; command-managed=none -->
| `administracao-pessoal.documentos-legais` | `DocumentoLegal` | 13 | 13 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 2 (2) | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.empresas: functional-accountability=12/12; command-managed=ativa -->
| `administracao-pessoal.empresas` | `Orgao` | 12 | 12 | 100.0% | 12/12 | 11 | 7 ops | 2 | 5 | YES | 1 (1) | 2 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.funcionarios: functional-accountability=68/68; command-managed=agencia,bairroender,banco,cepender,cidadeender,complender,conta,eMail,nomelogender,numender,numtelCelular,telefone,tipologender,tipopag,ufender -->
| `administracao-pessoal.funcionarios` | `ServidorPessoa` | 68 | 71 | 100.0% | 68/68 | 24 | 9 ops | 2 | 5 | YES | 2 (2) | 3 | 0 | 1 | 2 | 0 | 0 | 7 | 2 | 0 | **FAIL (Enterprise mutable resource (>=15 biz cols) missing @MicroVisualization; Enterprise mutable resource (>=15 biz cols) missing @UiAnalytics/@AnalyticsProjection)** |
<!-- administracao-pessoal.setores: functional-accountability=12/12; command-managed=none -->
| `administracao-pessoal.setores` | `Setor` | 12 | 12 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 4 (4) | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.vinculos: functional-accountability=59/59; command-managed=agencia,banco,cargoComissao,conta,correio,descontaIr,documentoLegalId,dtHomologEstagio,dtPgtoAte,dtaposent,dtfimCessao,dtiniCessao,dtvac,fatorProporcAposent,formavac,funcaoReq,jornadaSemanal,motivovac,orgaoReq,planoprev,regimeprev,setor,setorExercicio,situacao,tipoOnusReq,tipoReq,tipoRessarcReq,tipoapos,tipopag -->
| `administracao-pessoal.vinculos` | `Vinculo` | 59 | 74 | 100.0% | 59/59 | 32 | 12 ops | 6 | 6 | YES | 13 (13) | 3 | 9 | 2 | 4 | 0 | 0 | 10 | 4 | 0 | **FAIL (Enterprise mutable resource (>=15 biz cols) missing @MicroVisualization; Enterprise mutable resource (>=15 biz cols) missing @UiAnalytics/@AnalyticsProjection)** |

## 3. @UISchema Editorial & Metadata Quality Issues
- **`OptionSources / EntityLookupDescriptor`** (1 issues):
  - `OptionSourcesConfig: skeletal EntityLookupDescriptor (6 null arguments; populate LookupSelectionPolicy, LookupDetailDescriptor, LookupDisplayDescriptor, LookupFilteringDescriptor)`

## 4. Verdict: FAILED (4 structural or operational gate failure(s))
- [x] administracao-pessoal.cargos: FAIL (Enterprise mutable resource (>=15 biz cols) missing @MicroVisualization; Enterprise mutable resource (>=15 biz cols) missing @UiAnalytics/@AnalyticsProjection)
- [x] administracao-pessoal.funcionarios: FAIL (Enterprise mutable resource (>=15 biz cols) missing @MicroVisualization; Enterprise mutable resource (>=15 biz cols) missing @UiAnalytics/@AnalyticsProjection)
- [x] administracao-pessoal.vinculos: FAIL (Enterprise mutable resource (>=15 biz cols) missing @MicroVisualization; Enterprise mutable resource (>=15 biz cols) missing @UiAnalytics/@AnalyticsProjection)
- [x] OptionSources: FAIL (OptionSourcesConfig: skeletal EntityLookupDescriptor (6 null arguments; populate LookupSelectionPolicy, LookupDetailDescriptor, LookupDisplayDescriptor, LookupFilteringDescriptor))
