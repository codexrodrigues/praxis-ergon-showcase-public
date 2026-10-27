# Praxis Resource Completeness, Quick View & Editorial Quality Audit
- **Project**: `D:\Developer\praxis-plataform\praxis-ergon-showcase`
- **Mapped JPA Entities**: 16
- **Resource Controllers**: 4

- **Scope**: static source evidence; not HTTP, concurrency, parity or runtime approval.

## 1. Resource & Capability Scorecard
| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---|
<!-- administracao-pessoal.cargos: functional-accountability=17/17; command-managed=dtExtincao -->
| `administracao-pessoal.cargos` | `Cargo` | 17 | 20 | 100.0% | 17/17 | 15 | 8 ops | 4 | 5 | YES | 2 (2) | 1 | 1 | 0 | 2 | 0 | **PASS** |
<!-- administracao-pessoal.empresas: functional-accountability=12/12; command-managed=ativa -->
| `administracao-pessoal.empresas` | `Orgao` | 12 | 12 | 100.0% | 12/12 | 11 | 7 ops | 2 | 5 | YES | 1 (1) | 1 | 2 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.funcionarios: functional-accountability=68/68; command-managed=agencia,bairroender,banco,cepender,cidadeender,complender,conta,eMail,nomelogender,numender,numtelCelular,telefone,tipologender,tipopag,ufender -->
| `administracao-pessoal.funcionarios` | `ServidorPessoa` | 68 | 70 | 100.0% | 68/68 | 24 | 9 ops | 2 | 5 | YES | 1 (1) | 2 | 0 | 1 | 2 | 0 | **PASS** |
<!-- administracao-pessoal.vinculos: functional-accountability=59/59; command-managed=agencia,banco,cargoComissao,conta,correio,descontaIr,documentoLegalId,dtHomologEstagio,dtPgtoAte,dtaposent,dtfimCessao,dtiniCessao,dtvac,fatorProporcAposent,formavac,funcaoReq,jornadaSemanal,motivovac,orgaoReq,planoprev,regimeprev,setor,setorExercicio,situacao,tipoOnusReq,tipoReq,tipoRessarcReq,tipoapos,tipopag -->
| `administracao-pessoal.vinculos` | `Vinculo` | 59 | 71 | 100.0% | 59/59 | 32 | 12 ops | 6 | 6 | YES | 4 (4) | 3 | 7 | 2 | 4 | 0 | **PASS** |

## 4. Verdict: STATIC PASS (source gates satisfied; runtime and architectural review still required)

## 5. Evidência funcional complementar por recurso

Esta seção é avaliação manual apoiada nos testes; não é uma inferência do contador estático. Fonte: `src/test/java/br/com/praxis/ergonshowcase/NeonIntegrationTest.java`.

| Recurso | Invariantes e operações exercitadas | Evidência de teste |
|---|---|---|
| orgaos | CNPJ único; estado por ações; ETag atual, ausente e obsoleto; PUT não altera ativa; novas admissões recusadas em órgão inativo; ficha institucional | revisionProfessionalRegistrationAndUniqueness; reviewInstitutionalActionsProtectStateAndETag; reviewOrgaoStateIsNotWritableByCadastralPut; reviewInactiveOrgaoRejectsNewAdmission; reviewReadSurfacesPublishRealETagAndReferences |
| cargos | Saldo inclui comissão e titular, sem dupla contagem; cargo extinto e vagas insuficientes rejeitados; redução de vagas e aglutinador validados; extinção com ocupantes bloqueada; ficha e enriquecimento em lote | revisionBusinessRulesRejectInvalidVacanciesAndAggregation; reviewCommissionOccupancyPreventsExtinctionAndDoubleCounting; reviewCommissionCannotUseExtinctOrFullCargo; reviewCommissionCannotExceedVacancies; reviewDerivedFactsUseOneBatchQueryRegardlessOfPageSize |
| servidor-pessoas | CPF único, par de registro profissional, PcD pelos cinco indicadores, governança no schema; manutenção parcial preserva identidade e demais dados; ficha cadastral | revisionProfessionalRegistrationAndUniqueness; revisionDiscoveryAndRichSchemas; revisionPartialFormsPreserveIdentityAndUntouchedValues; reviewReadSurfacesPublishRealETagAndReferences |
| vinculos | Jornada 1..168 no HTTP e serviço; ingresso e contrato coerentes; unidade/cargo no mesmo órgão; acumulação operacional; cessão, aposentadoria, homologação e vacância; dtPgtoAte; afastamento/retorno, código, documento, sobreposição, proprietário, estado e versão | reviewPatchRejectsBothInvalidJornadaBounds; reviewServiceAlsoRejectsInvalidJornada; revisionBusinessRulesRejectChronologyAndAccumulation; revisionBusinessRulesRejectCrossOrgAndExtinctCargo; revisionCessaoAndRetirementTransitions; revisionHomologacaoAndCargoExtinction; reviewVacancyMaintainsPaymentLimit; reviewVacancyRejectsPaymentBeforeDeparture; reviewAfastamentoRoundTripPersistsLicenseAndVersion; reviewAfastamentoRejectsOverlappingPeriodAndEarlyReturn; reviewRetornoRejectsForeignLicense |

Filtros e respectivos predicados são provados por revisionFiltersExecuteTheirDeclaredPredicates. CRUD, chaves compostas, cursor, locate, opções, estatísticas, custom fields, exportação, autenticação, discovery e governança permanecem na suíte de integração. Os 18 testes Python do auditor incluem falsos positivos de escrita, joins espelhados/graváveis, limiar, projeto vazio, referências inexistentes e recursos somente de leitura.

## 6. Limites do scorecard

100% significa cobertura das colunas de negócio e destino de escrita identificável na fonte dos quatro pilotos. Não significa paridade completa com todas as telas/rotinas do Ergon. Reactive Determinations, decisões governadas por motor de regras, master-detail das outras 12 entidades, Quick View de setores/documentos não publicados, execução Angular e carga concorrente não foram provados nesta rodada. O novo AGENTS.md exige avaliar essas fases separadamente; este scorecard não as marca como concluídas nem substitui nova revisão arquitetural independente.

| Recurso | Lacuna de aceitação que o PASS estático não encerra |
|---|---|
| orgaos | Composição Angular da ficha e master-detail institucional não exercitados; sem determinações reativas publicadas. |
| cargos | Prova visual de quadro de vagas e navegação entre fichas não executada; regras estatutárias além das colunas reais não certificadas. |
| servidor-pessoas | Visão 360° com documentos/outros registros auxiliares e fluxos reativos não publicados; governança de schema não equivale a autorização de campo em runtime. |
| vinculos | Setores e documentos não têm APIs próprias; elegibilidade integral de licenças/previdência/acumulação, decisões governadas e paridade completa de todos os ciclos legados não certificadas. |
