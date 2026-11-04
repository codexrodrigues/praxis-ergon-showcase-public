# Issue #10 — Correções dos achados restantes S1–S3

01/11/2026. Implementação local autorizada, classificação `contrato-publico`,
proprietário `praxis-ergon-showcase`. Sem mudança de starter, release, commit,
push ou aplicação de migration em banco remoto. Não constitui aprovação
independente da Issue.

## S1 — Cadeia V5→V6 com seed integral

A V5 local ainda não aplicada externamente foi corrigida na fonte. Essa condição
está registrada em `SLICE-6-CRUD-VALIDACAO.md:66-67` e
`SLICE-6-FASE-1-VALIDACAO.md:58-61`; não se efetuou repair de checksum remoto.
O seed cria somente a venda avulsa `PECUNIA`. O abono permanece na parcela
programada, com folha e política `G` coerentes com os cinco dias associados aos
quinze dias de gozo. A homologação posterior cria o financeiro com origem explícita.

Os contadores do primeiro ciclo demonstrativo consideram as reservas existentes:
45 dias de gozo, 15 destinados a pecúnia/abono e saldo 30. Não se mantém uma venda
órfã para representar novamente o mesmo abono.

O teste `s1CompleteV5SeedAndV6InstallWithoutOrphanAbonoOrDoubleDebit` executa a
V5 **integral, com seu seed**, seguida da V6 integral, em PostgreSQL 14.22 com
pré-requisitos sintéticos. Confere estados, pagamento histórico e saldo contra
as parcelas/vendas efetivamente presentes. Os testes anteriores sem seed
continuam úteis para os demais cenários isolados. ABONO histórico sem origem
comprovada continua recusado pelo preflight; não há associação por descrição,
datas aproximadas ou outra heurística.

## S2 — Origem com direito já materializado

A V6 mantém `HOMOLOGADA` a averbação de licença que já possui período ativo
explicitamente vinculado, respeitando primeiro remoção e desaverbação. A
existência desse vínculo representa direito materializado no contrato V5;
a origem não volta a uma análise que exigiria apagar consumo para ser concluída.

Um período ativo ligado a origem cancelada/removida, totalmente desaverbada ou
de finalidade incompatível exige reconciliação **antes** do DDL. Não se fabrica
homologação para um grafo contraditório.

Regressões verificam a migração do grafo consumido, quatro origens incompatíveis
e a manutenção documental legítima de uma origem migrada como homologada, com
parcela efetivamente encerrada. A guarda contra desaverbar direito já consumido
permanece ativa; a correção não libera redução retroativa indevida.

## S3 — Compensação e reemissão do abono

A action existente `estornar-venda-licenca-especial` agora atende também ao abono
vinculado. Conserva valor e data do pagamento, registra `dtEstorno` e exige data
de efeito que não anteceda a liquidação. O financeiro estornado permanece
`ESTORNADA`; sua identidade nunca é reciclada para uma nova autorização.

O estorno financeiro não libera sozinho os dias do gozo. Depois dele é possível
cancelar a parcela ou interrompê-la segundo as regras funcionais já existentes.
Reservas e saldo continuam sendo calculados a partir do gozo, sem somar o
histórico financeiro vinculado como venda avulsa.

Manutenção comum do gozo não reativa o registro estornado. A action existente
`reconciliar-abono-vinculado` pode criar nova autorização com outra identidade,
sem copiar valor/pagamento anteriores. Uma autorização já ativa é reutilizada.
O índice único parcial garante um único financeiro ativo por gozo, conservando
o histórico 1:N. Checks SQL proíbem reativar um registro com data de estorno ou
estornar antes do pagamento original.

A resposta da reconciliação conserva o corpo e a revisão da origem e acrescenta
`autorizacaoAtualId`, referência semântica para a surface `detail` da autorização
encontrada/emitida. É uma projeção do resultado do ato, não coluna JPA, filtro ou
campo de stats. O recibo de idempotência conserva essa referência no replay.

`dtEstorno` foi incluído na entidade, DTOs, governança, apresentação de exportação,
filtros por janela/existência e proteção de create/PUT/lote. Datas de compensações
legadas desconhecidas continuam nulas. Os filtros de estado/modalidade e o quick
filter de abono refletem o vocabulário operacional atual.

Foram consultados os artefatos legados em
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/`,
especialmente `pck_erg_venda_licesp.pkb`, MAIN_PRE, ERG-03387, e
`pck_lic_esp.pkb`, MAIN_POS, linhas 305–333. A compensação financeira é capacidade
nova do host: preserva a proibição de manutenção avulsa da origem/dias, em vez
de remover essa guarda estrutural. Não executa transferência bancária ou folha
Oracle. Apuração e pagamento da nova autorização continuam exigindo seus atos.

## Validação e rastreabilidade

| Gate | Resultado |
|---|---|
| Suíte completa padrão `mvn -B test` | **348 testes, 0 falhas, 0 erros, 0 skipped; BUILD SUCCESS** |
| Auditor `--strict` | **25/25 STATIC PASS**; venda com 21/21 escritas contabilizadas |
| Novas regressões desta rodada | **19**: sete casos adicionais em PostgreSQL e doze em `TempoLicencaRemainingFindingsTest` |
| PostgreSQL da classe de regressões | 17 casos totais, incluindo cadeia completa, preflight, histórico financeiro, unicidade e checks de data/estado |
| Regressões de serviços | Estorno pago, cancelamento funcional posterior, interrupção/reemissão, datas, replay, proteção de histórico, folha nova, filtros e origem com consumo |

As matrizes anteriores de filtros foram atualizadas de 19 para 21 campos, com
teste do predicado real para `dtEstorno`; as verificações de contagem não foram
removidas. O writer compartilhado está explícito nas operações de sincronização
e reconciliação, com implementação privada comum, sem supressores do auditor.
Os três filtros de estado (`situacao`, inclusão e exclusão) dos cinco recursos
também refletem o lifecycle atual, com regressão que compara seus enums ao
modelo operacional e impede a volta do vocabulário cadastral antigo.

As provas de serviços usam os mappers reais e armazenamento simulado. Os testes
de migração, índice único e constraints usam PostgreSQL real em loopback, com
encerramento e limpeza ao final. O teste de replay usa o gateway de produção com
armazenamento simulado de recibos; a prova positiva anterior de gateway com
Hibernate/JpaTransactionManager/PostgreSQL permanece no relatório da revisão.
Não se afirma corrida JPA, fluxo HTTP completo, kernel de lote integral, Neon,
Oracle ou materialização Angular. O profile padrão continua excluindo classes
Neon; elas não são contabilizadas como skipped.

As evidências canônicas atualizadas estão em `SLICE-6-FASE-2-VALIDACAO.md`,
`SLICE-6-FASE-2-VALIDACAO.json`, `SLICE-6-FASE-2-TESTS.txt` e
`SLICE-6-FASE-2-SCORECARD.md`. Os artefatos imediatamente anteriores foram
arquivados em `slice6-phase2-corrections-review/` com prefixo `pre-s1-s3-`.
O parecer anterior CHANGES REQUESTED continua sendo registro da árvore que
reprovou, sem ser reescrito como aprovação da implementação atual.
