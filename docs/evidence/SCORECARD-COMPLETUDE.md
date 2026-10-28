# Scorecard — 503 e decisões de adoção

**80/80 testes Neon; 77/77 testes focais SDK; 75/75 regressões do auditor.**
O consumidor HTTP oficial passou com o mesmo SDK empacotado e verificado por SHA-256.
Gates operacionais PASS, sem hash de fonte obsoleto. **Veredito global FAILED:
uma pendência de adoção em Cargos; não pronto para merge.**

| Recurso | Cobertura de campos | Decisão e lacuna vigente |
|---|---:|---|
| Órgãos | 12/12 (100%) | Duplicate-draft não aplicável ao cadastro institucional atual; criação explícita |
| Cargos | 17/17 (100%) | Duplicate-draft aplicável e pendente: contrato neutro integrado, allowlist e provas de autorização/não persistência/criação final |
| Pessoas | 68/68 (100%) | Lote aprovado na campanha final; cópia da ficha de outra pessoa não aplicável |
| Vínculos | 59/59 (100%) | Ciclo de lote aprovado; novo provimento exige criação explícita, não cópia genérica do histórico |
| Setores | 12/12 (100%) | Consulta sem create; duplicate-draft não aplicável nesta fronteira |
| Documentos legais | 13/13 (100%) | Consulta de atos existentes sem create; duplicate-draft não aplicável |

As decisões estão fundamentadas em [avaliação de adoção](../DUPLICATE-DRAFT-ADOCAO.md)
e [inventário atribuído](503-adoption-inventory.json). N/A não equivale a proibição
universal nem a aprovação humana. Cobertura estática não certifica toda a riqueza
estatutária ou a experiência corporativa.

Evidências: [80 casos](503-final-test-results.json), [SDK](503-sdk-provenance.json),
[consumidor HTTP](503-angular-http-results.json), [preservação](503-neon-preservation.json),
[limpeza](503-neon-cleanup.json), [campanha](PILLARS-CAMPAIGN.json).
A falha histórica 73/74 permanece registrada; sua causa exata não foi reproduzida.
O defeito de composição multigrupo foi reproduzido por duas regressões e corrigido.

Limites: candidato privado .3-SNAPSHOT, rc.179 não certificada, sem release,
aceite visual desktop/narrow, identidade federada/política por órgão ou SLO.
Config/rules-engine permanece fase futura aprovada. As libs Angular pertencem à
frente separada do PR #661; esta rodada apenas executou o harness existente.

## Saída do auditor com fontes do candidato e evidências atuais

# Praxis Resource Completeness, Quick View & Editorial Quality Audit
- **Project**: `D:\Developer\praxis-plataform\praxis-ergon-showcase`
- **Mapped JPA Entities**: 16
- **Resource Controllers**: 6
- **Resolved controller bindings**: 6
- **Metadata source**: `.local\m2-bulk308\io\github\codexrodrigues\praxis-metadata-starter\8.0.0-bulk.308-consolidation.3-SNAPSHOT\praxis-metadata-starter-8.0.0-bulk.308-consolidation.3-SNAPSHOT-sources.jar`; declaration SHA-256 `2792bd94656a6c76913fbf77e06719f92af6573ca97d795a63efe3c4afb1aa01`
- **Expected inventory**: `D:\Developer\praxis-plataform\scripts\workspace\resource-audit-inventories\praxis-ergon-showcase.json`

## Inventory and duplicate-draft adoption
Adoption is an attributed review decision, separate from static binding. Code, annotations, interfaces and boolean getters do not prove non-persistence or runtime behavior.
| Resource | Controller | Binding | Duplicate-draft decision | Reason |
|---|---|---|---|---|
| administracao-pessoal.cargos | CargoController | CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO | pendente | Aplic�vel: preparo por allowlist de classifica��o exige contrato can�nico integrado e provas de n�o persist�ncia; ado��o pendente. |
| administracao-pessoal.documentos-legais | DocumentoLegalController | DocumentoLegalResponseDTO, Long, DocumentoLegalFilterDTO | n�o aplic�vel | Consulta de atos existentes sem create; refer�ncia jur�dica n�o constitui modelo de novo ato. |
| administracao-pessoal.empresas | OrgaoController | OrgaoResponseDTO, Integer, OrgaoFilterDTO, CreateOrgaoDTO, UpdateOrgaoDTO | n�o aplic�vel | Institui��o com identidade e CNPJ pr�prios; fluxo adotado � cria��o expl�cita, sem modelo interinstitucional aprovado. |
| administracao-pessoal.funcionarios | ServidorPessoaController | ServidorPessoaResponseDTO, Long, ServidorPessoaFilterDTO, CreateServidorPessoaDTO, UpdateServidorPessoaDTO | n�o aplic�vel | Cadastro de pessoa f�sica �nica; copiar identifica��o, dados sens�veis e banc�rios n�o � jornada adotada. Novo v�nculo referencia a pessoa existente. |
| administracao-pessoal.setores | SetorController | SetorResponseDTO, String, SetorFilterDTO | n�o aplic�vel | Recurso exclusivamente de consulta, sem create; manuten��o de organograma est� fora desta fronteira. |
| administracao-pessoal.vinculos | VinculoController | VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO | n�o aplic�vel | Novo provimento exige ato, vaga, �rg�o, datas e valida��o de acumula��o; c�pia gen�rica de assentamento hist�rico n�o � a jornada adotada. |
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

## Operational Evidence Receipts
Hashed receipts are checked for freshness; independent review remains required.

## 4. Verdict: FAILED (1 structural or operational gate failure(s))
- [x] administracao-pessoal.cargos: duplicate-draft adoption decision pending (not a requirement to implement copying)
