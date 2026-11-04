# Issue #10 — Correção dos sete achados

01/11/2026. Execução autorizada sobre a árvore local revisada. Classificação:
`contrato-publico`, `suportado-parcialmente`. O proprietário das correções é o
host `praxis-ergon-showcase`; não houve mudança de vocabulário do starter,
rotas, operationIds, bibliotecas universais ou política remota.

## Rastreabilidade e regressões

| Achado | Correção | Regressão |
|---|---|---|
| R1 — tabelas inválidas na autorização de lote | `Resource.table` vem de `@Table` da entidade, validado como identificador físico; autorização não deriva tabela da rota | `r1AllSlice6ResourcesAuthorizeReadsForEveryBulkMode`: quatro recursos × três modos em PostgreSQL real; alvos ausentes, outro órgão e outro solicitante continuam negados. `r1EveryBulkResourceUsesTheJpaPhysicalTableRatherThanItsPublicRoute` cobre todos os recursos de lote |
| R2 — V6 reabre histórico | Preserva `PAGA`, `GOZADA`, cancelamentos e destinações existentes. Preflight interrompe históricos ambíguos antes do DDL. Ações também rejeitam nova liquidação quando já existe `dtPagamento` | `r2MigrationPreservesPaidClosedAndCanceledHistoryAndInitializesOnlyPendingRecords`; cinco entradas ambíguas em `r2MigrationFailsBeforeReclassifyingAmbiguousHistory`; `r2HistoricalPaymentCannotBeAuthorizedOrRecordedAgainEvenIfStateIsInconsistent` |
| R3 — abono autorizado não liquidável | PATCH financeiro apura valor/observações do abono autorizado; folha/documentos/origem/dias continuam no gozo; cadastro e lote não alteram a origem. Locks seguem gozo → período → venda | `r3GeneratedAbonoCanBeAppraisedAndPaidThroughPublishedPatchAndAction`, `r3LinkedFinancialPatchDoesNotPermitOriginPayrollOrPaidValueEditsAndChecksVersion`, `r3FinancialPatchLocksGozoAndPeriodBeforeLinkedSale` |
| R4 — gozo reescreve folha de abono pago | Guarda comum ao PUT/PATCH/lote/transições antes do merge; sincronização não reescreve venda paga e lê o espelho sob lock | Quatro PATCHs de mudança/limpeza de competência/folha e os dois modos estruturais de lote. Manutenção documental legítima continua aceita |
| R5 — teto de licença atinge aposentadoria | Teto de 999 dias restrito à finalidade `LICENCA_ESPECIAL`; guardas de dependências permanecem comuns | Aposentadoria e tempo de serviço com 5.000 dias permitem desaverbar, reverter e cancelar; direito de licença acima de 999 continua rejeitado |
| R6 — interrupção invalida abono proporcional | Recalcula pela base `D`/`G`, preserva previsão, invalida valor não pago quando muda a quantidade e estorna espelho não pago quando o truncamento resulta em zero. Não modifica liquidação existente implicitamente | Encerramento e interrupção de 10+5 para 5+2, saldo 83; truncamento para zero; rejeição anterior ao merge de redução de abono pago; abono pago sobre direito preservado quando muda apenas o descanso |
| R7 — PUT apaga data de conversão | Guarda de `dtCntDobro` junto ao contador; proteção também do lastro de desaverbação/remoção | JSON com data nula/substituída rejeitado após conversão; manutenção documental preserva dez dias convertidos e saldo 80. Datas/motivos de averbação não podem ser trocados por PUT |

As regressões estão em `TempoLicencaIssue10RegressionTest` (23 casos) e
`TempoLicencaIssue10PostgresTest` (10 casos). Os **33 novos casos passaram** na
execução focal registrada em `SLICE-6-FASE-2-CORRECOES-REGRESSAO.txt`.
Esses números identificam a primeira rodada. O fechamento posterior S1–S3 está
em `SLICE-6-FASE-2-S1-S3.md`: a classe PostgreSQL passou a conter 17 casos e foram
adicionados doze testes de serviços/contratos, totalizando 19 regressões adicionais.

## Decisões de domínio

Foram consultados diretamente os arquivos de
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/`:

- `pck_lic_esp.pkb`, `MAIN_POS`, linhas 305–333: abono pela base direito ou pelo
  intervalo efetivo, com truncamento. A implementação mantém essa distinção.
- `pck_erg_venda_licesp.pkb`, `MAIN_PRE`, linhas 90–103: ERG-03387 protege a
  manutenção avulsa do abono. O PATCH financeiro novo em comportamento não
  altera seu lastro estrutural, dias ou origem. Não simula folha ou package Oracle.

O valor do abono não é inventado pelo servidor nem inferido de dias sem tabela
de remuneração. Um operador autorizado informa o valor pelo PATCH existente;
somente a ação de pagamento registra a liquidação. If-Match continua obrigatório,
e a ação mantém o gateway de Idempotency-Key já existente.

Uma interrupção não reinterpreta pagamento concluído. Se o recálculo da base `G`
reduziria o abono pago, a operação é rejeitada antes do merge; a redução não é
tratada como estorno financeiro implícito. Base `D` pode conservar o abono pago
quando a quantidade indenizada não se altera. A regressão verifica ambos os casos.
O estorno explícito e a reemissão necessários para essa jornada agora existem,
preservando o registro liquidado conforme o relatório S1–S3.

A V6 pode identificar encerramento pela `dtfim`, mas não inferir sua causa;
por isso usa `GOZADA`, preservando intervalo e quantidade. Históricos com fatos
incompatíveis exigem reconciliação. A V6 não tinha sido aplicada a banco externo
nas evidências anteriores; esta correção altera essa migração local ainda não
publicada, sem criar trilha de compatibilidade paralela.

## Evidência consolidada e limites

A suíte completa, o auditor estrito, o scorecard e os hashes finais estão em
`SLICE-6-FASE-2-VALIDACAO.md`, `SLICE-6-FASE-2-TESTS.txt`,
`SLICE-6-FASE-2-SCORECARD.md` e `SLICE-6-FASE-2-VALIDACAO.json`.

Os testes SQL iniciam PostgreSQL 14.22 em loopback, com diretório temporário
exclusivo e encerramento/limpeza ao final. Aplicam tanto o DDL V5 sem seed quanto
a V5 integral com seed, com tabelas pai sintéticas, seguida da V6 integral. Não acessam Neon, Oracle ou
dados reais. Demonstram migração e autorização de leitura do lote; não certificam
o protocolo completo de avaliação/confirmação/worker, HTTP, concorrência JPA dos
serviços nem Angular. A ordem de locks tem teste local, sem alegação de prova de corrida.

Resultado consolidado atual: **348 testes, 0 falhas, 0 erros, 0 skipped; BUILD SUCCESS**.
O auditor `--strict` encerrou com exit 0 e **25/25 STATIC PASS**; a cobertura do
período permanece em **33/33 escritas contabilizadas**. O scorecard integral
continua distinguindo presença estática de capacidade operacional certificada.

A suíte padrão mantém o profile `offline-without-neon-branch`, que exclui classes
Neon quando `BULK_PROOF_DISPOSABLE_BRANCH` não está definido. A ausência dessas
classes não aparece como skipped e não deve ser apresentada como execução delas.

O relatório original `slice6-phase2-review/REVISAO.md` continua sendo evidência
histórica da árvore reprovada. Os quatro artefatos canônicos anteriores foram
arquivados com prefixo `baseline-` nesse diretório antes de sua atualização. As
mudanças não receberam aprovação independente nesta execução; não houve commit,
push, publicação ou alteração de banco remoto.
