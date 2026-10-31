# Praxis Resource Completeness, Quick View & Editorial Quality Audit
- **Project**: `D:\Developer\praxis-plataform\praxis-ergon-showcase`
- **Mapped JPA Entities**: 16
- **Resource Controllers**: 6
- **Resolved controller bindings**: 6
- **Metadata source**: `.local\m2-bulk308\io\github\codexrodrigues\praxis-metadata-starter\8.0.0-bulk.308-consolidation.5-SNAPSHOT\praxis-metadata-starter-8.0.0-bulk.308-consolidation.5-SNAPSHOT-sources.jar`; declaration SHA-256 `2596238abdd845ad90146cabdfd9e8c349462e5eff6b14eb99dac24496a93c21`
- **Endpoint source universe**: 703 host/selected-metadata sources; SHA-256 `0f93a2859823be3c7781405db82ff10312b503e8ec12e852260cc917906a9827`
- **Expected inventory**: `D:\Developer\praxis-plataform\scripts\workspace\resource-audit-inventories\praxis-ergon-showcase.json`

## Inventory and duplicate-draft adoption
Adoption is an attributed review decision, separate from static binding. Code, annotations, interfaces and boolean getters do not prove non-persistence or runtime behavior.
| Resource | Controller | Binding | Duplicate-draft decision | Reason |
|---|---|---|---|---|
| administracao-pessoal.cargos | CargoController | CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO | pendente | Aplicável e integrada no host com SDK privado; aceite bloqueado por 503 recorrente no discovery: integral 112/113 e focal reforçada 2/3. Exige diagnóstico canônico e nova prova revisada. |
| administracao-pessoal.documentos-legais | DocumentoLegalController | DocumentoLegalResponseDTO, Long, DocumentoLegalFilterDTO | não aplicável | Consulta de atos existentes sem create; referência jurídica não constitui modelo de novo ato. |
| administracao-pessoal.empresas | OrgaoController | OrgaoResponseDTO, Integer, OrgaoFilterDTO, CreateOrgaoDTO, UpdateOrgaoDTO | não aplicável | Instituição com identidade e CNPJ próprios; fluxo adotado é criação explícita, sem modelo interinstitucional aprovado. |
| administracao-pessoal.funcionarios | ServidorPessoaController | ServidorPessoaResponseDTO, Long, ServidorPessoaFilterDTO, CreateServidorPessoaDTO, UpdateServidorPessoaDTO | não aplicável | Cadastro de pessoa física única; copiar identificação, dados sensíveis e bancários não é jornada adotada. Novo vínculo referencia a pessoa existente. |
| administracao-pessoal.setores | SetorController | SetorResponseDTO, String, SetorFilterDTO | não aplicável | Recurso exclusivamente de consulta, sem create; manutenção de organograma está fora desta fronteira. |
| administracao-pessoal.vinculos | VinculoController | VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO | não aplicável | Novo provimento exige ato, vaga, órgão, datas e validação de acumulação; cópia genérica de assentamento histórico não é a jornada adotada. |
- **Scope**: static source evidence; not HTTP, concurrency, parity or runtime approval. N+1 requires cold full-query/page/cursor/export measurements; this source audit cannot certify it.

## 1. Resource & Capability Scorecard
| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | `MicroViz` | `UiAnalytics` | `ReactiveDet` | BulkEditable | BulkOps | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
<!-- administracao-pessoal.cargos: functional-accountability=17/17; command-managed=dtExtincao -->
| `administracao-pessoal.cargos` | `Cargo` | 17 | 22 | 100.0% | 17/17 | 16 | 8 ops | 4 | 5 | YES | 5 (5) | 3 | 2 | 0 | 2 | 1 | 2 | 1 | 5 | 2 | 0 | **PASS** |
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
- angularReadiness: not-run
- availability: failed
- http: failed
- reactiveDeterminations: partial

## 4. Verdict: FAILED (5 structural or operational gate failure(s))
- [x] administracao-pessoal.cargos: duplicate-draft adoption decision pending (not a requirement to implement copying)
- [x] angularReadiness: not-run
- [x] availability: failed
- [x] http: failed
- [x] reactiveDeterminations: partial
