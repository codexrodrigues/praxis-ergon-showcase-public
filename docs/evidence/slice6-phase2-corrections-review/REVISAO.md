# Revisão criteriosa das correções — Issue #10

**Parecer: CHANGES REQUESTED. A Issue #10 ainda não pode ser aprovada.**

01/11/2026. Revisão da árvore corrigida, sem alterações de produção, testes da
suíte, migrations ou evidências canônicas da implementação. Os 397 arquivos do
manifesto conferiram, sem divergência. As provas adicionais e este relatório
ficam exclusivamente neste diretório.

As correções resolvem os contraexemplos originais nos recortes cobertos, mas a
revisão encontrou três lacunas funcionais que impedem o fechamento: duas na
transição V5→V6 e uma no ciclo financeiro do abono pago. A conclusão não decorre
apenas da ausência de testes externos.

## Achados acionáveis

### S1 — [P1] A cadeia de instalação V5→V6 falha com o seed efetivamente entregue

Local: `src/main/resources/db/migration/V6__tempo_licenca_ciclo_de_vida.sql:5-7`.
Origem do dado: `V5__averbacoes_e_licenca_especial.sql:165-169`.

A V5 integral cria um registro de modalidade `ABONO` para cada titular admitido
pela carga demonstrativa. A primeira guarda da V6 rejeita exatamente esses
registros. Portanto, uma instalação com um vínculo elegível já presente não
completa a sequência de migrations entregue.

O novo teste SQL executa `v5.substring(0, v5.indexOf("-- Seed demonstrativo"))`
em `TempoLicencaIssue10PostgresTest.java:47`, eliminando a origem do problema.
Além disso, `docs/SLICE-6-FASE-2.md:103` afirma que a carga usa vendas avulsas
`PECUNIA`, o que não corresponde ao SQL: ela cria uma `PECUNIA` e um `ABONO`.

**Prova:** `MigrationChainProbe` criou pais sintéticos mínimos com um vínculo,
executou a V5 inteira, verificou um `ABONO` e tentou a V6 inteira. O PostgreSQL
rejeitou a V6 com `reconcilie os registros ABONO sem origem de gozo`. Ver
`migration-chain.log`, marcador `complete V5 demo seed creates ABONO`.

O preflight conservador para registros históricos ambíguos é correto e não deve
ser simplesmente removido. Falta tornar coerentes a instalação nova, os dados
demonstrativos e o procedimento explícito de reconciliação, sem inventar uma FK
a partir de descrições ou heurísticas. A regressão deve executar a cadeia com
o seed real, além do DDL sem seed.

### S2 — [P1] V6 aceita origem EM_ANALISE com período já consumido e a deixa sem avanço possível

Local: `src/main/resources/db/migration/V6__tempo_licenca_ciclo_de_vida.sql:25-29`.
Guardas envolvidas: `TempoLicencaLifecycle.java:50-53,72-74` e
`TempoLicencaValidation.validarImpactoOrigem`.

Quando uma averbação V5 `REGISTRADO` não tem remoção/desaverbação, a migração
escolhe `EM_ANALISE`, mesmo que ela já seja a origem de um período com consumo.
Para esse mesmo grafo, o período é migrado para `PARCIALMENTE_GOZADO`.

A origem não consegue então ser homologada: a própria homologação chama
`validarImpactoOrigem`, que proíbe consumo no período ligado. Cancelar também
falha pela mesma guarda. Desaverbar/reverter não são admitidos em `EM_ANALISE`.
Excluir a origem é impedido pela existência dos períodos. O registro histórico
aceito pela migração fica operacionalmente bloqueado.

**Prova SQL:** após remover somente o `ABONO` sintético para isolar este segundo
defeito, a V6 foi aplicada e produziu `EM_ANALISE` na origem e
`PARCIALMENTE_GOZADO` no período, que conserva 30 dias gozados. Ver segundo
marcador `CONFIRMED` em `migration-chain.log`.

**Prova de serviço complementar:** uma fixture separada com serviços/mappers
reais, origem `EM_ANALISE` e período ligado com dez dias efetivamente gozados
rejeita tanto homologar quanto cancelar com `Regularize o consumo`. Essa prova
usa armazenamento simulado; não se apresenta como execução integrada do mesmo
grafo SQL. A condição impeditiva é qualquer consumo maior que zero. Ver segundo
marcador `CONFIRMED` em `behavior.log`.

É necessário reconciliar o grafo de origem/direito/consumo na migração e nas
transições. Quando os dados não permitem inferir o estado com segurança, a V6
deve exigir reconciliação antes de aceitar esse grafo, como já faz em outros
históricos ambíguos. Não basta migrar corretamente o estado isolado da parcela.

### S3 — [P2] O abono agora pode ser pago, mas seu estorno não tem caminho funcional

Local: `src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaLifecycle.java:152-155`.
Contrato publicado: `VendaLicencaEspecialController.java:97-106`.

A correção R3 tornou alcançável, pelas operações publicadas, o abono vinculado
`PAGA`. A action de estorno declara `PAGA` entre os estados admitidos, mas o
lifecycle sempre rejeita modalidade diferente de `PECUNIA` com ERG-03387 e
orienta cancelar o abono na parcela de gozo.

Essa orientação não é executável para um abono pago: o cancelamento do gozo
é rejeitado por `preservarLiquidacao`; a reconciliação existente conserva `PAGA`;
e a redução proporcional do abono pago na interrupção também é bloqueada.
Não há um ato compensatório publicado que resolva essa situação. O snapshot
de estado do Slice 6 contém somente `situacao`, não a modalidade/origem que
provoca essa recusa invariável da action.

**Prova:** `CorrectionBehaviorProbe` criou e homologou gozo, apurou o abono pelo
PATCH financeiro e registrou o pagamento pela action. Em seguida verificou:
estorno da venda → ERG-03387; cancelamento do gozo → `Abono liquidado`;
reconciliação → continua `PAGA`. Ver primeiro marcador em `behavior.log`.

A imutabilidade da liquidação é uma proteção correta; removê-la não resolve
o ciclo. É preciso um ato financeiro compensatório governado, coerente com a
origem e a contabilidade, além de discovery/availability compatíveis. Essa
lacuna aparece justamente porque o pagamento do abono passou a funcionar.

## Reavaliação dos sete achados originais

| Achado original | Resultado da correção no recorte revisado | Fechamento |
|---|---|---|
| R1 — nomes físicos das tabelas | Usa `@Table`, com identificador validado; testes PostgreSQL cobrem quatro recursos e três modos do callback, incluindo negativas de escopo/alvo | Aceito para o defeito original; não certifica o protocolo completo do kernel |
| R2 — reabertura de pagamentos e gozos | Venda paga e gozo encerrado são preservados nas fixtures novas; pagamentos repetidos são rejeitados; cinco ambiguidades são recusadas | **Parcial**: S1 e S2 impedem aprovar a migração como um todo |
| R3 — abono não liquidável | PATCH de valor/observações permite apuração e pagamento, preservando origem, dias, folha e documentos | Caminho positivo corrigido; **ciclo incompleto por S3** |
| R4 — folha de abono pago sobrescrita pelo gozo | Guarda anterior ao merge em PATCH/PUT/candidato de lote; sincronização conserva venda paga; negativas de mudança e limpeza passam | Aceito para os cenários originais; concorrência JPA completa não foi provada |
| R5 — teto de licença em averbações para aposentadoria | Teto restrito a LICENCA_ESPECIAL; aposentadoria/tempo de serviço com 5.000 dias podem desaverbar, reverter e cancelar | Aceito no recorte testado |
| R6 — interrupção proporcional inválida | Recalcula `D`/`G`, preserva previsão, invalida apuração não paga quando muda quantidade e trata truncamento para zero | Cenário original corrigido; proteção de pago é correta, mas falta a saída compensatória de S3 |
| R7 — data de conversão alterada por PUT | Data protegida junto ao contador; JSON nulo/substituído é rejeitado; manutenção documental legítima conserva o ato; lastro de averbação também protegido | Aceito no recorte testado |

## Verificações executadas nesta revisão

| Verificação | Resultado | Alcance |
|---|---|---|
| Manifesto da implementação | 397 fontes, sem divergência | Árvore corrigida identificada; hashes completos preservados na implementação |
| `mvn -B test` | **329 testes, 0 falhas, 0 erros, 0 skipped; BUILD SUCCESS** | Suíte padrão, com as 33 regressões; classes Neon excluídas pelo profile |
| Auditor `--strict` | **25/25 STATIC PASS**, exit 0 | Presença/contabilidade estática; não valida cadeia de migração nem fechamento de todos os estados |
| Gateway real + Hibernate + JpaTransactionManager + PostgreSQL 14.22 | **PASS:** gravação e replay, efeito executado uma vez | Prova adicional do recibo e sua transação, com resposta sintética; não é teste concorrente nem HTTP |
| V5 completa com seed → V6 completa | **FAIL funcional reproduzido:** S1 | Pais e dados sintéticos em PostgreSQL local |
| Migração do grafo de origem com consumo | **FAIL funcional reproduzido:** S2 | SQL real mais prova complementar de serviços com armazenamento simulado |
| Ciclo de abono pago | **FAIL funcional reproduzido:** S3 | Serviços/mappers reais, armazenamento simulado; sem pagamento externo |

Não foi promovida a defeito a hipótese de incompatibilidade da query de advisory
lock com o Hibernate: a prova real do gateway passou. Tampouco foram afirmados
deadlocks ou falhas de isolamento sem reprodução conclusiva.

## Scorecard obrigatório

Valores do auditor reexecutado. **P** significa presença no contrato/inventário,
não certificação HTTP ou Angular. O arquivo `scorecard.md` conserva o resultado
integral dos 25 recursos e os Checks 13 a 16.

| Recurso | Colunas entidade / Response | Cobertura | Escritas | QuickViewCols | RichIdentity | ResourceReference / preview | MicroViz | RichEntityLookup | EditorialIssues | Filterable / variedade | QuickFilter |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Averbação | 30 / 33 | 100% | 30/30 | 4 | YES | 7 / 7 | 1 | P | 0 | 18 / 12 ops | 3 |
| Período | 33 / 36 | 100% | 33/33 | 5 | YES | 8 / 8 | 1 | P | 0 | 18 / 12 ops | 3 |
| Gozo | 22 / 25 | 100% | 22/22 | 5 | YES | 9 / 9 | 1 | P | 0 | 18 / 11 ops | 3 |
| Venda | 20 / 23 | 100% | 20/20 | 5 | YES | 9 / 9 | 1 | P | 0 | 19 / 10 ops | 3 |
| Radar | 19 / 21 | 100% | read-only | 4 | YES | 8 / 8 | 2 | P | 0 | 17 / 11 ops | 3 |

| Recurso | UiSurface | Intent PATCH | WorkflowAction | BulkEditable / BulkOps | ResourceFact | UiAnalytics | ReactiveDet | Invariantes/determinações testadas |
|---|---|---|---|---|---|---|---|---|
| Averbação | 4 | 2 | 5 | 17 / 3 | 3 | 2 | 1 | Homologação, desaverbação, reversão, origem e teto por finalidade; S2 permanece |
| Período | 5 | 2 | 8 | 19 / 3 | 4 | 2 | 1 | Contabilidade, conversão, data de efeito, suspensão e prescrição; grafo histórico ainda afetado por S2 |
| Gozo | 4 | 2 | 6 | 12 / 3 | 3 | 2 | 1 | Abono, pagamento protegido e interrupção D/G; saída compensatória ausente em S3 |
| Venda | 2 | 1 | 5 | 8 / 3 | 3 | 2 | 1 | Apuração, pagamento, replay, estorno avulso e export; abono pago afetado por S3 |
| Radar | 1 | 0 | 0 | 0 / 0 | 3 | 2 | 0 | Registry com propriedades JPA reais, metadados e export locais; sem prova HTTP |

## Níveis de conformidade e aprovação

| Camada | Situação |
|---|---|
| Inventário estático | PASS do script; não afasta os defeitos operacionais |
| Schema público | Evidência HTTP incompleta; annotations/testes nativos não provam todos os documentos publicados |
| HTTP | Não exercitado nesta revisão; gateway real não equivale à pilha HTTP |
| Availability | Coerência incompleta no estorno de abono pago: estado publicado admitido, modalidade invariavelmente recusada |
| Persistência/ciclo de vida | FAIL por S1 e S2; prova positiva adicional do gateway; S3 falha na lógica dos serviços |
| Angular readiness | Não certificada; não houve execução do consumidor |

Os três achados concretos bastam para reprovar o fechamento, independentemente
dos limites de evidência HTTP/Angular. Não se exige aprovação de release, acesso
a dados reais ou campanha externa para demonstrá-los.

Para reconsiderar a aprovação, corrigir S1–S3 na fonte do host, incorporar as
reproduções como regressões, repetir a cadeia real de instalação e a jornada
completa de abono pago/compensação, e então reexecutar os gates. A revisão atual
não altera nem aprova a implementação.

## Artefatos

- `mvn-test.log`, `scorecard.md`: gates reexecutados.
- `GatewayPostgresProbe.java`, `gateway-postgres.log`: prova positiva de SQL,
  transação e replay com gateway de produção, sem entidades de negócio no ORM.
- `MigrationChainProbe.java`, `migration-chain.log`: cadeia com seed completo,
  falha da V6 e isolamento do segundo caso em dados exclusivamente sintéticos.
- `CorrectionBehaviorProbe.java`, `behavior.log`: contraexemplos de serviços.
- `review-manifest.json`: hashes da revisão, estado das fontes e resultados.

As classes de prova usam Java 21 e o classpath Surefire registrado em
`classpath.txt`. São compiladas fora de `src` e `target` com `javac -proc:none
-encoding UTF-8 -cp <classpath> -d <diretorio> <fontes>` e executadas a partir da
raiz do host, no pacote `br.com.praxis.ergonshowcase`. O PostgreSQL local foi
encerrado; seu diretório de dados temporário foi removido após a coleta.
