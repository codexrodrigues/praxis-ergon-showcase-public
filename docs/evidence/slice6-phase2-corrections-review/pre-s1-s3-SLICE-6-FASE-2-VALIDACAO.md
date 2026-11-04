# Validação — Slice 6 / Fase 2 / Issue #10

01/11/2026. Implementação local no working tree existente, sem commit, push,
publicação ou alteração de banco remoto. Não é parecer de revisão independente.
As alterações locais anteriores foram preservadas; a suíte cobre o estado atual
completo do host, não apenas arquivos atribuíveis a esta tarefa.

| Gate | Resultado |
|---|---|
| `mvn -B test` com Java 21 e Metadata rc.186 | **329 testes, 0 falhas, 0 erros, 0 ignorados reportados; BUILD SUCCESS** |
| `python ../scripts/workspace/audit-java-resource-completeness.py . --strict` | **25/25 STATIC PASS**, 25 entidades e bindings resolvidos |
| `git diff --check` | Exit 0 |
| Gates nativos em `PraxisMetadataCompletenessTest` | 5 testes verdes: revisão semântica, expressões e inventário de option sources, entre os demais contratos |
| Testes específicos da Fase 2 | 39 casos anteriores + **33 regressões novas**: 23 de serviços/contratos e 10 com PostgreSQL local |
| Regressões dos sete achados | **33/33 verdes**, execução focal e suíte completa; rastreabilidade em `SLICE-6-FASE-2-CORRECOES.md` |

`mvn test` usa o profile padrão `offline-without-neon-branch`, que exclui classes
Neon sem `BULK_PROOF_DISPOSABLE_BRANCH`. A contagem não certifica testes de integração.

## Contabilidade e qualidade editorial

| Recurso | Colunas de negócio / campos response | Cobertura | Escrita contabilizada | Quick View | Identity rica | Referências / preview | MicroViz | EditorialIssues | Filtros / operadores | QuickFilters |
|---|---|---|---|---|---|---|---|---|---|---|
| Averbação | 30 / 33 | 100.0% | 30/30 | 4 | YES | 7 (7) | 1 | 0 | 18 / 12 ops | 3 |
| Período | 33 / 36 | 100.0% | 33/33 | 5 | YES | 8 (8) | 1 | 0 | 18 / 12 ops | 3 |
| Gozo | 22 / 25 | 100.0% | 22/22 | 5 | YES | 9 (9) | 1 | 0 | 18 / 11 ops | 3 |
| Venda | 20 / 23 | 100.0% | 20/20 | 5 | YES | 9 (9) | 1 | 0 | 19 / 10 ops | 3 |
| Radar | 19 / 21 | 100.0% | N/A (read-only) | 4 | YES | 8 (8) | 2 | 0 | 17 / 11 ops | 3 |

Os lookups ricos do Slice 6 conservam selection/detail/display/filtering,
retention de valores históricos, cascata e by-ids. A seleção de nova origem exige
averbação homologada; períodos quitados, suspensos, prescritos e cancelados são
bloqueados para novas referências. O inventário nativo é validado pela suíte.

## Capacidades e testes

| Recurso | Surfaces | Intents PATCH | WorkflowActions próprias | BulkEditable / BulkOps | Facts | Analytics | ReactiveDet | Invariantes / determinações testadas |
|---|---|---|---|---|---|---|---|---|
| Averbação | 4 | 2 | 5 | 17 / 3 | 3 | 2 | 1 | Homologação e geração do PA; desaverbação/reversão/cancelamento; criação protegida |
| Período | 5 | 2 | 8 | 19 / 3 | 4 | 2 | 1 | Saldo derivado; conversão/reversão; suspensão, prescrição e cancelamento |
| Gozo | 4 | 2 | 6 | 12 / 3 | 3 | 2 | 1 | Homologação e abono sem débito duplo; interrupção/encerramento/cancelamento |
| Venda | 2 | 1 | 5 | 8 / 3 | 3 | 2 | 1 | Autorização/pagamento/estorno; replay/conflito; campos restritos na exportação |
| Radar | 1 | 0 | 0 | 0 / 0 | 3 | 2 | 0 | Propriedades JPA reais em stats, projections e export; consulta somente leitura |

A contagem estática de actions é a declarada nos controllers concretos; os quatro
mutáveis também herdam duplicate-draft. A decisão exibida no inventário central
sobre duplicação é histórica e está distinguida da adoção solicitada nesta Issue.
O arquivo central fica fora do escopo permitido deste trabalho e não foi alterado.

Os testes executam o compilador BulkEditableFields sobre os schemas reais,
ambos os modos de edição, rejeição de tipos/coerções e estado protegido, candidato
sem mutação e aplicação de comando com sincronização do pai. Não certificam
publicação READY, captura/recibos do kernel em banco nem corrida entre workers.
A autorização de leitura foi adicionalmente executada com PostgreSQL 14.22 nos
quatro recursos e três modos, com permissão do alvo existente e rejeição de outro
órgão, outro solicitante e alvo inexistente. O nome físico vem de `@Table`.

As prévias de criação registradas atendem ao gate estrito de determinações e não
representam a Fase 3 completa. Regras legadas 1:1, governança por tenant e paridade
integrada continuam fora desta Fase 2.

A migração V6 foi aplicada **somente em PostgreSQL local isolado**, sobre o DDL V5
sem seed demonstrativo e com pais sintéticos. Os testes preservam pagamentos e
encerramentos históricos e verificam rollback diante de cinco históricos ambíguos.
Registros antigos ABONO sem FK de origem exigem reconciliação explícita; o preflight
da V6 impede inventar associações. Não houve aplicação em banco externo/Neon,
execução HTTP ou materialização Angular. Os testes de serviços usam armazenamento
simulado; a ordem de locks foi verificada localmente, sem prova de corrida JPA.

Os sete achados têm correção e regressões identificadas no relatório de correções.
A revisão original permanece como registro da árvore anterior reprovada; os
artefatos anteriores foram arquivados com prefixo `baseline-` em
`slice6-phase2-review/`. Esta execução não constitui nova aprovação independente.

Artefatos:

- [Scorecard integral](SLICE-6-FASE-2-SCORECARD.md)
- [Log da suíte](SLICE-6-FASE-2-TESTS.txt)
- [Manifesto de hashes](SLICE-6-FASE-2-VALIDACAO.json)
- [Contrato e decisões da Fase 2](../SLICE-6-FASE-2.md)
- [Correções e regressões dos sete achados](SLICE-6-FASE-2-CORRECOES.md)
- [Execução focal das regressões](SLICE-6-FASE-2-CORRECOES-REGRESSAO.txt)
