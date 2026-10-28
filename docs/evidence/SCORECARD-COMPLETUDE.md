# Scorecard — correções da revisão 2013442

**STATIC PASS nos seis recursos; aceite operacional pendente, sem liberação para merge.**

O `mvn test` integral executou 74 casos: 4 falhas, 0 erros e 0 ignorados. A repetição focal executou 5 casos: 3 passaram e 2 falharam. O teste de segregação foi corrigido e passou por HTTP real; schemas e analytics passaram na repetição. Em conjunto, 73 dos 74 casos distintos possuem prova positiva nesta rodada. Isso **não equivale a uma execução Maven integral verde**.

O fluxo positivo de lote de vínculos recebeu 503 de publicação durante a confirmação; a repetição focal foi bloqueada pelo timeout de reconciliação. O tratamento seguro evita 500 e não autoriza replay automático, mas a disponibilidade permanece uma lacuna operacional.

| Recurso | Completude estática | Prova de negócio/contrato e lacunas |
|---|---:|---|
| Órgãos | 12/12, 100% | Gates cadastrais e metadados; limites de identidade corporativa permanecem |
| Cargos | 17/17, 100% | Escopo antes do limite, aglutinação concorrente, SET/CLEAR nas duas modalidades de lote comprovados |
| Pessoas | 68/68, 100% | PATCH com presença/null e SET/CLEAR nas duas modalidades de lote comprovados |
| Vínculos | 59/59, 100% | PATCH, regras e preview acionável comprovados; ciclo positivo completo de lote pendente por 503 |
| Setores | 12/12, 100% | Contratos/diretório comprovados; sem aceitação visual de tabela |
| Documentos legais | 13/13, 100% | Contratos/diretório comprovados; sem aceitação visual de tabela |

SDK: 130 testes focais aprovados; auditor: 45 regressões aprovadas. Angular: 17 specs e builds Core/Table, pureza aprovada, alteração sob responsabilidade de agente separado no PR #661. O estado da prova HTTP ampliada consta da [campanha](PILLARS-CAMPAIGN.json).

Evidências: [execução integral](review-full-test-results.json), [repetição focal](review-focal-test-results.json), [auditoria operacional](review-operational-audit.md), [proveniência SDK](review-sdk-provenance.json), [schema preservado](review-neon-schema-comparison.json).

Limites: candidato privado `8.0.0-bulk.308-review-SNAPSHOT`; rc.179 não certificada; sem instalação npm pública, avaliação visual desktop/narrow ou prontidão corporativa. Config/rules-engine permanece fase futura aprovada. Não houve merge/release.

# Praxis Resource Completeness, Quick View & Editorial Quality Audit
- **Project**: `D:\Developer\praxis-plataform\praxis-ergon-showcase`
- **Mapped JPA Entities**: 16
- **Resource Controllers**: 6

- **Scope**: static source evidence; not HTTP, concurrency, parity or runtime approval. N+1 requires cold full-query/page/cursor/export measurements; this source audit cannot certify it.

## 1. Resource & Capability Scorecard
| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | `MicroViz` | `UiAnalytics` | `ReactiveDet` | BulkEditable | BulkOps | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
<!-- administracao-pessoal.cargos: functional-accountability=17/17; command-managed=dtExtincao -->
| `administracao-pessoal.cargos` | `Cargo` | 17 | 22 | 100.0% | 17/17 | 16 | 8 ops | 4 | 5 | YES | 5 (5) | 3 | 1 | 0 | 2 | 1 | 2 | 1 | 5 | 2 | 0 | **PASS** |
<!-- administracao-pessoal.documentos-legais: functional-accountability=13/13; command-managed=none -->
| `administracao-pessoal.documentos-legais` | `DocumentoLegal` | 13 | 13 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 2 (2) | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.empresas: functional-accountability=12/12; command-managed=ativa -->
| `administracao-pessoal.empresas` | `Orgao` | 12 | 12 | 100.0% | 12/12 | 11 | 7 ops | 2 | 5 | YES | 1 (1) | 2 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.funcionarios: functional-accountability=68/68; command-managed=agencia,bairroender,banco,cepender,cidadeender,complender,conta,eMail,nomelogender,numender,numtelCelular,telefone,tipologender,tipopag,ufender -->
| `administracao-pessoal.funcionarios` | `ServidorPessoa` | 68 | 71 | 100.0% | 68/68 | 25 | 9 ops | 2 | 5 | YES | 2 (2) | 3 | 0 | 1 | 2 | 1 | 2 | 1 | 7 | 2 | 0 | **PASS** |
<!-- administracao-pessoal.setores: functional-accountability=12/12; command-managed=none -->
| `administracao-pessoal.setores` | `Setor` | 12 | 12 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 4 (4) | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.vinculos: functional-accountability=59/59; command-managed=agencia,banco,cargoComissao,conta,correio,descontaIr,documentoLegalId,dtHomologEstagio,dtPgtoAte,dtaposent,dtfimCessao,dtiniCessao,dtvac,fatorProporcAposent,formavac,funcaoReq,jornadaSemanal,motivovac,orgaoReq,planoprev,regimeprev,setor,setorExercicio,situacao,tipoOnusReq,tipoReq,tipoRessarcReq,tipoapos,tipopag -->
| `administracao-pessoal.vinculos` | `Vinculo` | 59 | 74 | 100.0% | 59/59 | 33 | 12 ops | 6 | 6 | YES | 13 (13) | 3 | 9 | 2 | 4 | 1 | 2 | 1 | 10 | 4 | 0 | **PASS** |

## 4. Verdict: STATIC PASS (source gates satisfied; runtime and architectural review still required)
