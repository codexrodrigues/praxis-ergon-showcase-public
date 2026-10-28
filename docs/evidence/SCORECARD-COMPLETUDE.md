# Praxis Resource Completeness, Quick View & Editorial Quality Audit
- **Project**: `D:\Developer\praxis-plataform\praxis-ergon-showcase`
- **Mapped JPA Entities**: 16
- **Resource Controllers**: 6

- **Scope**: static source evidence; not HTTP, concurrency, parity or runtime approval. N+1 requires cold full-query/page/cursor/export measurements; this source audit cannot certify it.

## 1. Resource & Capability Scorecard
| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---|
<!-- administracao-pessoal.cargos: functional-accountability=17/17; command-managed=dtExtincao -->
| `administracao-pessoal.cargos` | `Cargo` | 17 | 22 | 100.0% | 17/17 | 15 | 8 ops | 4 | 5 | YES | 5 (5) | 3 | 1 | 0 | 2 | 0 | **PASS** |
<!-- administracao-pessoal.documentos-legais: functional-accountability=13/13; command-managed=none -->
| `administracao-pessoal.documentos-legais` | `DocumentoLegal` | 13 | 13 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 2 (2) | 1 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.empresas: functional-accountability=12/12; command-managed=ativa -->
| `administracao-pessoal.empresas` | `Orgao` | 12 | 12 | 100.0% | 12/12 | 11 | 7 ops | 2 | 5 | YES | 1 (1) | 2 | 2 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.funcionarios: functional-accountability=68/68; command-managed=agencia,bairroender,banco,cepender,cidadeender,complender,conta,eMail,nomelogender,numender,numtelCelular,telefone,tipologender,tipopag,ufender -->
| `administracao-pessoal.funcionarios` | `ServidorPessoa` | 68 | 71 | 100.0% | 68/68 | 24 | 9 ops | 2 | 5 | YES | 2 (2) | 3 | 0 | 1 | 2 | 0 | **PASS** |
<!-- administracao-pessoal.setores: functional-accountability=12/12; command-managed=none -->
| `administracao-pessoal.setores` | `Setor` | 12 | 12 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 4 (4) | 1 | 0 | 0 | 0 | 0 | **PASS** |
<!-- administracao-pessoal.vinculos: functional-accountability=59/59; command-managed=agencia,banco,cargoComissao,conta,correio,descontaIr,documentoLegalId,dtHomologEstagio,dtPgtoAte,dtaposent,dtfimCessao,dtiniCessao,dtvac,fatorProporcAposent,formavac,funcaoReq,jornadaSemanal,motivovac,orgaoReq,planoprev,regimeprev,setor,setorExercicio,situacao,tipoOnusReq,tipoReq,tipoRessarcReq,tipoapos,tipopag -->
| `administracao-pessoal.vinculos` | `Vinculo` | 59 | 74 | 100.0% | 59/59 | 32 | 12 ops | 6 | 6 | YES | 13 (13) | 3 | 7 | 2 | 4 | 0 | **PASS** |

## 4. Verdict: STATIC PASS (source gates satisfied; runtime and architectural review still required)

## 5. Aceitação por recurso e fase

Contagens acima são resultados do auditor canônico, não uma aprovação arquitetural. As 392 colunas pertencem às 16 entidades; o denominador por piloto exclui IDs, versão, auditoria e associações espelhadas. Auxiliares somente de leitura não exigem escrita.

| Recurso | Estrutura / coluna de negócio | Fase 2 / backend | Fase 3 / determinações e consumo | Lacunas antes do aceite integral |
|---|---|---|---|---|
| orgaos | PASS 12/12, Quick View 5, referências reais | CNPJ/estado/ETag e quadro de cargos paginado comprovados | Sem cálculo derivado aplicável ao cadastro; navegação relacionada publicada | Composição visual da ficha/coleção não exercitada |
| cargos | PASS 17/17, Quick View 5, comissão/aglutinação/documento resolvíveis | Saldo/extinção/órgão/aglutinador, titulares e comissão comprovados | Prévia de aglutinador CREATE/EDIT: duas execuções oficiais | Disputa concorrente da última vaga e regras estatutárias integrais não certificadas |
| servidor-pessoas | PASS 68/68, Quick View 5, documento resolvível | PATCH com limite real/400, CPF, identificação profissional e vínculos relacionados comprovados | Prévia PcD CREATE/EDIT: duas execuções, resposta obsoleta protegida, submit recusa resultado forjado | Validação visual e fluxos jurídicos de concessão por PcD não certificados |
| vinculos | PASS 59/59, Quick View 6, 13 referências/preview | Contexto frio página/cursor/exportação 4/4; cronologia; histórico; três formulários especializados no conjunto; ações e documentos por operação | Prévia nome/saldo CREATE/EDIT: duas execuções e rejeição visível | Decisões governadas aguardam snapshot aprovado; paridade estatutária integral não certificada |
| setores (auxiliar) | PASS 12/12, somente leitura | Ficha, filtros e opções HTTP comprovados | Destino real de referências de lotação/exercício | Não é um módulo completo de manutenção de unidades |
| documentos-legais (auxiliar) | PASS 13/13, somente leitura | Ficha, filtros, opções e política de referência por operação comprovados | Destino real de referências normativas | Existência não certifica aplicabilidade jurídica do diploma |

## 6. Provas e gates

- 52 testes de integração, zero falhas/erros/ignorados: neon-integration-tests.txt. mvn package executou a suíte completa contra Neon isolado e empacotou o JAR.
- 29 regressões do auditor e transporte isolado aprovado: auditor-tests.txt, ../tooling/auditor-canonico-hashes.json.
- Schema por operação: oito capturas request/response dos pilotos e seis capturas de determinações CREATE/EDIT.
- Caminho completo frio: cold-query-counts.json; uma e três linhas executam quatro statements em página, cursor e exportação.
- Runtime oficial de fonte Angular: angular-consumer-proof.json, seis schemas/seis execuções; sem fórmula local, com proteção de geração e erro exposto. Não equivale a pacote npm publicado, tela host ou verificação visual.
- Paridade deliberadamente delimitada por fonte/capacidade: ../MATRIZ-PARIDADE-305.md. Asserções de teste no NeonIntegrationTest possuem nomes phaseOne, phaseTwo e phaseThree, além das regressões anteriores revision/review.

**Resultado estrutural: STATIC PASS, seis recursos. Resultado operacional global: PENDENTE.** O manifesto runtime-gates.json mantém governedDecisions=awaiting-input e --require-runtime-evidence reprova esse gate. Não há N/A presumido. Snapshot aprovado do Config e nova revisão independente são necessários para encerrar o aceite integral. A paridade admitida cobre somente o cadastro e as operações listadas na matriz, não todo o legado. Nenhum merge executado.
