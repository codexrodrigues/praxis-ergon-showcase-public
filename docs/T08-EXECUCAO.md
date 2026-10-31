# T08 — duplicate-draft em Cargos: execução e bloqueio

**Resultado: CHANGES REQUESTED. Adoção de Cargo pendente; gate T08 reprovado; merge e avanço a T09 não liberados.** A implementação e os testes foram reconciliados com ApiPaths. O bloqueio é um 503 recorrente na leitura governada de metadados, observado na integral e na única focal complementar. Não houve repetição até verde, aumento de timeout ou relaxamento do guard.

## Resultados executados

| Prova | Resultado | Evidência |
|---|---|---|
| Integral reconciliada | 113 testes; 112 aprovados; 1 falha; 0 erros; 0 skips | `evidence/t08-final-test-results.json` |
| Focal única com oráculos reforçados | 3 métodos completos; 2 aprovados; 1 falha; 0 erros; 0 skips | `evidence/t08-negative-contract-focal-results.json` |
| Regressão geral Neon | 63/63 | Incluída na integral |
| Lote / pilares / completude / contexto | 6/6, 6/6, 4/4, 9/9 | Incluídos na integral |
| Rotas / contrato draft / apresentação 3.0 e 3.1 | 2/2, 5/5, 4/4 | Incluídos na integral |
| SDK — tratamento de constraint | 60/60 focais | `evidence/t08-sdk-constraint-tests.json` |
| Auditor canônico | 114/114 regressões; seis recursos estruturais PASS, 100%, zero problemas editoriais | `evidence/t08-auditor-regression.json`, `evidence/t08-static-audit.md` |
| Auditor com recibos runtime | FAILED; fonte atual e hashes válidos, sem stale evidence | `evidence/t08-runtime-audit.md` |
| Consumidor HTTP / browser | Não executados neste corte; 676 inputs oficiais conferidos não equivalem a execução | `evidence/t08-http-preparation.json`, `evidence/t08-angular-inputs.json` |

A integral inclui Cargo Neon 13/14. As asserções negativas dessa versão tinham uma lacuna de shape posteriormente corrigida; o número 13/14 não certifica sozinho todas as fronteiras de negação. Zero skips também não significa que as asserções posteriores a uma falha tenham executado.

## Fontes e dependência efetivamente usadas

- Baseline do host: `1769b0958bbb3499e02843619415b7c373a95745`.
- Delta isolado anterior: `e0b1a5d6460c274d6e0d03d8e24b84db691fa77a`; o candidato foi reconciliado no checkout compartilhado `praxis-ergon-showcase`.
- Fonte da integral: `891ee0799cd95beeb496e4249fe10a029b503044814784247ee0f295eb183f0d`.
- Fonte atual/focal: `8f4d93e777f3338ca7af7f4b32f4ba1815cffc4265c9a1170fc4d03fc484f723`.
- Entre esses hashes mudou somente `CargoDuplicateDraftNeonTest.java`; produção, configuração, scripts do host e SDK ficaram iguais. A revisão independente conferiu 129 dos 130 arquivos src/pom inalterados. Recibo: `evidence/t08-negative-oracle-change.json`.
- Fingerprint independente src/pom atual: `914df16fb108898af3653ce195ecb5d941f9e710b092b73e66a51bb7bcda07e5`; anterior arquivado: `73cfa8a23ed5fef03c7a34a0d08df68fd0479aa0474cc66bf3a1818d829e119d`.
- SDK privado: `8.0.0-bulk.308-consolidation.5-SNAPSHOT`, commit `e53a13534b7b475ba6383e4802dd1e6b176ed6d0` em `.worktrees/bulk-runtime-consolidation`.
- SHA-256 do JAR SDK, confirmado no classpath e no BOOT-INF: `11194e06878268a184fbeb476f7b1a4fbd09a287c91badf36cb86e3e7072abd0`.
- SHA-256 do host empacotado: `0ebb019e7126eccea0014caa561c65157ffdaf4f7d792840ac6991562d8d86ce`.

O POM ainda declara rc.179, insuficiente para esta extensão. As provas usam override explícito e cache privado; não há certificação do artefato público ou do build padrão. Proveniência completa em `evidence/t08-sdk-provenance.json` e `evidence/t08-packaged-artifact.json`.

## Delta implementado

`CargoDuplicateDraftDTO` é independente do CREATE: sete sugestões (`nome`, `categoria`, `subcategoria`, `tipoCargo`, `cargoFuncao`, `escolaridade`, `cbo`), nove campos nulos e `customFields` vazio. Não copia identidade, versão, auditoria, extinção, ato, vagas, datas, agregação ou atributos livres. As três flags começam null. O CREATE exige escolha explícita e aceita false; o UPDATE conserva seus defaults anteriores.

O contrato neutro do Starter usa o DTO parcial como sexto parâmetro genérico. O host seleciona os sete campos por allowlist explícita. Endpoint e serviço exigem READ e WRITE; a consulta inclui órgão e identidade. Somente origem VIGENTE é aceita: qualquer extinção não nula, inclusive futura, nega. A transação própria de leitura evita flush do contexto do chamador. Preparar não reserva código nem congela a origem; criar revalida as regras atuais.

`TenantRequestValidation` valida contexto antes da descoberta do recurso, mantendo 400 para header malformado e 401 para anônimo. Cargo novo mantém versão Java nula para o Spring Data escolher persist; o SDK trata a colisão nativa Hibernate como 409 sanitizado. A prova concorrente confirmou vencedor preservado.

ApiPaths foi preservado e reconciliado. Os FKs do draft usam os mesmos lookups do CREATE, e as flags usam seleção booleana sem default. Nenhuma biblioteca Angular foi alterada nesta frente.

A revisão fortaleceu cinco comparações negativas com `BooleanNode.FALSE`, exigiu status 200 e envelopes válidos e conferiu metadata de estado no contexto de outro órgão. `allowedStates` documental permanece permitido. Uma expectativa proposta de GET capabilities para registro inexistente foi descartada antes de aplicar: esse contrato retorna 404. A equivalência de origem ausente/fora do órgão é exigida no POST duplicate-draft.

Fonte de domínio: `docs-legado/v7x/aps/cargos_.tab:119–295,1146–1169`, entidades/invariantes atuais e decisões humanas `T07-decisoes-mantenedor.md`. O booleano do host não representa integralmente os códigos L/Q/N ou POR_REFER do legado. Não foi acrescentado veto a pai aglutinador extinto nem regra jurídica inferida.

## Matriz A01–A20

| ID | Estado e evidência delimitada |
|---|---|
| A01 | PASS na integral: allowlist, contagens/hashes de domínio e ledger, ETag/versão e descarte sem escrita |
| A02 | PASS: função F, opcionais nulos e CBO textual; contrato 3.0/3.1 |
| A03 | PASS: preparo repetido e ausência de flush/DML do contexto sujo do chamador, com observação committed independente |
| A04 | PARCIAL: CREATE e regras revalidados; roteiro HTTP complementar de readback dos 17 campos não executado |
| A05 | Funcional PASS na focal reforçada: READ-only negado, capability/action false explícito e link ausente; integral teve 503 na consulta de ações |
| A06 | PASS na focal: POST ausente/fora do órgão indistinguível; availability explícita, motivo correto e metadata sem estado efetivo do outro órgão |
| A07 | PASS na integral: contexto ausente/malformado/divergente, anônimo e validação do catálogo real do órgão 2 |
| A08 | BLOQUEADO: negação do preparo exercitada; focal de origem extinta parou no GET capabilities 503. Não certificar as asserções seguintes nem a segunda iteração dessa focal |
| A09 | PASS: identidade atribuída com versão null é nova; concorrência real 201/409 e readback do vencedor |
| A10 | PASS: identidade/classificação/flags explícitas, false válido e prévia não confiável rejeitada |
| A11 | PASS nas regras exercitadas: vagas/flags e invariantes da regressão geral; sem obrigação jurídica nova de data/ato |
| A12 | PARCIAL: integridade/concorrência de agregador passaram; cenário HTTP complementar específico de pai extinto não executado |
| A13 | PASS: quatro propriedades desconhecidas rejeitadas individualmente; id ignorado não controla identidade, versão ou auditoria |
| A14 | PASS no escopo testado: catálogo atual, chave desconhecida, enum e required após preparo; readOnly é prova controlada do validator, não ACL publicada pelo provider real |
| A15 | Backend PASS para contexto/referências; limpeza visual do formulário ao trocar órgão permanece fora deste corte |
| A16 | PASS em HTTP OpenAPI 3.0 e resolver oficial 3.0/3.1; não houve host HTTP em modo 3.1 |
| A17 | PARCIAL: schema/links/action positivos passaram; disponibilidade operacional bloqueada pelos dois 503 |
| A18 | FAIL: integral 112/113; focal única 2/3. Nenhuma falha foi removida ou convertida em skip |
| A19 | NÃO EXECUTADO: browser desktop/narrow, route/overlay, foco, descarte e submit único. Não avançar a T09 por este pacote |
| A20 | Estrutura PASS; gate integrado FAIL. Inventário atribuído mantém Cargo pendente; cinco decisões N/A preservadas |

## Duas ocorrências e reprodução delimitada

1. Integral, `readWithoutCreatePermissionCannotPrepareOrDiscoverExecutableAction:299`: POST negado, capability negada e GET do item sem link passaram; GET `/api/ergon/cargos/{id}/actions` recebeu 503 em vez de 200. Não chegaram a executar a asserção da ação nem a igualdade final do snapshot desse método. Log: `2026-10-28T22:08:05.731-03:00`. Recibo `evidence/t08-cargo-integrated-results.json`.
2. Focal única, `extinctOriginIncludingFutureDateIsDeniedInEveryBoundary:223`: GET `/api/ergon/cargos/{id}/capabilities` recebeu 503 em vez de 200. A focal READ-only e a de contexto estrangeiro passaram. Log: `2026-10-28T23:34:38.860-03:00`. Recibos `evidence/t08-negative-contract-focal-results.json` e `evidence/t08-second-timeout-diagnostic.json`.

Ambos registram `GovernedOpenApiPublicationUnavailableException` causado por `TransactionTimedOutException`. O JFR aponta a leitura LIVE de catálogo em `BulkCatalogPhase.Reader.finish` dentro de `readPublicationForDocumentGuard`; a primeira pilha envolve ações, a segunda superfícies/capabilities. As maiores leituras amostradas citadas foram 584 ms e 667 ms. Isso não estabelece a causa física nem atribui uma execução multiresult a uma SQL específica. O segundo JFR não contém eventos JavaExceptionThrow: as pilhas de SocketRead não são a cadeia Throwable completa. A revisão não demonstrou defeito canônico específico que justifique patch heurístico. Os prazos de 2 s/3 s e a guarda conservadora permanecem intactos.

As oito operações estavam READY antes/depois da focal e após a reconciliação HTTP normal, com gerações idênticas. Não houve recuperação administrativa especial para os três testes. A reincidência mantém o gate reprovado; não houve nova integral/focal após ela.

Comandos usados, com credenciais somente no ambiente privado:

```powershell
mvn -o '-Dmaven.repo.local=.local/m2-bulk308' '-Dpraxis.core.version=8.0.0-bulk.308-consolidation.5-SNAPSHOT' clean test
mvn -o '-Dmaven.repo.local=.local/m2-bulk308' '-Dpraxis.core.version=8.0.0-bulk.308-consolidation.5-SNAPSHOT' '-Dtest=CargoDuplicateDraftNeonTest#extinctOriginIncludingFutureDateIsDeniedInEveryBoundary+readWithoutCreatePermissionCannotPrepareOrDiscoverExecutableAction+missingAndOtherOrganizationOriginsAreIndistinguishableAndDoNotLeakState' test
```

Ambos também usaram JFR default, dumponexit, maxsize 128m, com arquivos privados distintos; não alteraram budgets do SDK. A falha é intermitente, não uma reprodução determinística garantida. A branch usada foi excluída: uma futura prova exige nova cópia descartável, preservando schema e o oráculo, e hipótese verificável ou correção concreta antes de repetir campanhas. Este pacote encerra captura e diagnóstico delimitado, não toda a infraestrutura.

## Preservação, limpeza e limites

Os 16 conjuntos de dados de domínio ficaram idênticos por contagem e MD5 canônico de linhas, comparando original, cópia e baseline anterior. Colunas (618), constraints (332), índices (115) e comentários efetivos (408) dos schemas public/praxis_bulk também coincidem. Não houve DDL, migrations ou grants; `ddl-auto=validate` e `flyway.enabled=false` permanecem. Sequências e conteúdo do ledger técnico não foram comparados; propostas/recibos/publicação são efeitos técnicos esperados das provas.

Branch descartável `br-orange-smoke-b7ma4hie` excluída após captura, conforme autorização permanente do usuário. Listagem completa confirmou preservação de `br-super-sound-b7urizmd` e main. Maven/JFR encerrados, host HTTP próprio parado e porta 8080 livre. Worktrees locais permanecem disponíveis para revisão. `.local/` é privado e ignorado.

O host HTTP foi empacotado e reconciliado normalmente, mas o harness consumidor e o roteiro HTTP adicional não foram executados depois da falha recorrente. Não usar preparação ou hashes de inputs como prova de consumo. O acesso demonstrativo global continua vigente: X-Tenant é contexto, não entitlement individual. Não há certificação de identidade federada, SLO corporativo, UX/browser ou versão pública. Config/rules-engine permanece fase futura aprovada pelo usuário.

README, contrato, relatório, scorecard e recibos foram atualizados. Landing, corpus público e recipes não foram publicados neste corte. A mudança do CREATE exige clientes com três flags explícitas; UPDATE conserva defaults. Nenhum push, mensagem GitHub, merge, tag, release, deploy ou avanço a T09 foi feito. A revisão focal do coordenador ocorrerá após este pacote e é distinta da revisão de reconciliação já recebida.

## Histórico e material de revisão

As falhas anteriores permanecem atribuídas: integral isolada 95/97, preparação integrada com timeout e correções de fixtures para required do órgão 2 e rejeição estrita de propriedades desconhecidas. Os respectivos recibos `t08-pre-context-full-results.json`, `t08-integrated-focal-publication-timeout.json`, `t08-reconciliation-focal-results.json`, `t08-context-unknown-input-results.json` e `t08-context-final-results.json` não foram reclassificados.

Raw XML/log/JFR ficam privados em `.local/t08-final-integrated-campaign`, `.local/t08-negative-focal-campaign` e arquivos JFR adjacentes; recibos sanitizados ficam em `docs/evidence/`. A decisão independente está copiada em `evidence/t08-adoption-review.md`, com inventário em `evidence/t08-adoption-inventory.json`. O pacote de handoff contém deltas host/SDK, manifestos de hash e estes resultados, sem credenciais.

## Identidade Git e recorte de autoria

O checkout compartilhado permanece na branch `feat/ergon-showcase-scaffold-pilots`, HEAD `1769b095`. Não foi criado commit misto nem alterado o staging para absorver mudanças preexistentes de ApiPaths. Os commits isolados host/SDK estão identificados acima; os resultados integrados pertencem ao snapshot pelos hashes, não ao HEAD sozinho.

O pacote separa `host-preexisting-shared.patch` (HEAD → snapshot anterior à reconciliação) de `host-t08-reconciled.patch` (snapshot → estado final deste corte), acompanhado de atribuição por caminho e snapshot completo dos 277 arquivos de fonte/docs no escopo. A reconstrução deve aplicar ambas as camadas sobre o baseline. O diff posterior ao snapshot está restrito aos 37 caminhos atribuídos no plano de integração e às evidências posteriores. Mudanças de outras frentes e os arquivos privados permanecem preservados fora de qualquer staging global.
