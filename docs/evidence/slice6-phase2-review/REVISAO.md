# Revisão da Issue #10 — Slice 6 / Fase 2

**Parecer: CHANGES REQUESTED.** Revisão executada em 01/11/2026 sobre a árvore local entregue. Sete defeitos reproduzidos impedem a aprovação funcional, apesar de 296 testes verdes e do `STATIC PASS` do auditor estrito.

Não houve correção de código de produção, alteração de testes da suíte, commit, publicação ou acesso ao banco externo. Os arquivos de reprodução e os resultados desta revisão estão neste diretório. A árvore contém trabalho local anterior; esta revisão não atribui todo o diff à Issue #10.

## Resultado dos gates

| Verificação executada nesta revisão | Resultado | Limite da evidência |
|---|---|---|
| Comparação com `SLICE-6-FASE-2-VALIDACAO.json` | 395 fontes conferidas, sem divergência | Identifica a árvore revisada; não certifica comportamento |
| `python ../scripts/workspace/audit-java-resource-completeness.py . --strict` | Exit 0, 25/25 recursos, STATIC PASS | Análise estática; não executa SQL nem combinações do ciclo de vida |
| `mvn -B test`, Java 21 | 296 testes, 0 falhas, 0 erros, 0 skipped | Profile `offline-without-neon-branch`; classes Neon excluídas, não contabilizadas como skipped |
| `Phase2ReviewProbe` | 6/6 contraexemplos confirmados | Serviços e mappers reais; armazenamento, locks e versão simulados pela fixture existente |
| `PostgresBulkReadProbe` | Falha da autorização de lote nos quatro recursos e reclassificação incorreta da V6 confirmadas | PostgreSQL 14.22 isolado em loopback; callback real e SQL real, sem aplicação HTTP/kernel completo |

O exit 0 das reproduções significa que o defeito esperado foi observado. Não significa que o comportamento de negócio passou. Os testes existentes continuam verdes precisamente porque não cobrem esses cenários.

## Achados

### R1 — [P1] Autorização de leitura impede os três modos de lote nos quatro recursos

Local: `src/main/java/br/com/praxis/ergonshowcase/bulk/ErgonBulkOperations.java:267-276`; novos recursos em `ErgonBulkDomain.java:35-38`.

O fallback `resource.name` contém nomes de rota (`averbacoes-tempo-servico`, por exemplo). O SQL exige os nomes físicos com underscore (`averbacoes_tempo_servico`). A consulta gera erro de sintaxe PostgreSQL `42601`, capturado como `AUTHORITY_UNAVAILABLE`, inclusive quando o usuário e o alvo pertencem ao órgão correto e o registro existe.

A mesma autorização atende às leituras de proposta, resultados de proposta, execução e resultados de execução. A avaliação devolve a proposta pelo leitor autorizado. Portanto, publicar as annotations dos modos `UNIFORM_UPDATE`, `PER_ITEM_UPDATE` e `DOMAIN_COMMAND` não torna o protocolo utilizável.

Prova: callback de produção executado com conexão PostgreSQL real vinculada ao datasource da infraestrutura, contra as quatro tabelas reais criadas pelo DDL V5. Nos quatro casos a consulta com nome físico encontra um registro e o callback retorna `AUTHORITY_UNAVAILABLE` por SQL inválido. Ver `postgres-bulk-read.log`, linhas `CONFIRMED A`.

Correção necessária: representar os nomes físicos explicitamente no proprietário canônico do recurso usado pelo host e validar o protocolo completo de proposta/confirmação/leitura nos três modos. Trocar hífen por underscore por convenção genérica seria uma nova inferência implícita de persistência.

### R2 — [P1] V6 reabre pagamentos e gozos históricos

Local: `src/main/resources/db/migration/V6__tempo_licenca_ciclo_de_vida.sql:20-25`.

A V5 permitia `dt_pagamento` preenchida em vendas `REGISTRADO`. A V6 transforma toda venda não cancelada em `SOLICITADA`, preservando a data e o valor da liquidação anterior. Também transforma gozos com `dtfim` efetivo em `PROGRAMADA`. Esses estados contradizem os fatos já registrados.

Prova SQL: DDL V5 sem seed demonstrativo, pais mínimos sintéticos, uma venda com pagamento em 05/01/2026 e valor de R$ 1.234,56, e um gozo encerrado em 10/01/2026. A V6 integral e sem alterações executa com sucesso e produz respectivamente `SOLICITADA` e `PROGRAMADA`. Não há preflight para esses casos, ao contrário do preflight para `ABONO` sem origem.

Prova de serviço complementar: a combinação produzida pela migração permite `autorizar-venda-licenca-especial` e depois `pagar-venda-licenca-especial` em 05/02/2026; a nova data substitui 05/01/2026. Isso demonstra repetição do registro de liquidação, não uma segunda transferência monetária externa. Ver `CONFIRMED G` nos dois logs.

Correção necessária: reconciliar o estado com os fatos históricos de cada recurso ou bloquear a migração dos registros ambíguos até reconciliação explícita. Testar dados existentes, além de tabela vazia.

### R3 — [P1] Abono autorizado gerado pelo gozo não consegue alcançar liquidação

Local: `src/main/java/br/com/praxis/ergonshowcase/service/VendaLicencaEspecialService.java:80-86`, em conjunto com `TempoLicencaValidation.java:202-212` e `TempoLicencaLifecycle.java:140-144`.

Homologar um gozo com abono cria `VendaLicencaEspecial` em `AUTORIZADA` com `valorIndenizacao = null`. A ação de pagamento exige valor positivo. O PATCH de liquidação e o PUT passam por `beforeUpdate`, que rejeita qualquer venda vinculada com `ERG-03387`. A edição em lote também rejeita vínculos em `validateBulkCandidate`. A instrução de manter o abono na parcela não resolve: o contrato do gozo não possui o valor indenizatório, e a sincronização não o apura.

Prova: criar e homologar gozo de dez dias com abono de cinco; o abono gerado fica autorizado sem valor; pagamento falha com 422 e PATCH somente do valor falha com `ERG-03387`. Ver `CONFIRMED E`.

Correção necessária: disponibilizar uma manutenção financeira governada para o abono vinculado, preservando no gozo a autoridade sobre origem e dias, ou implementar uma apuração determinística server-side que forneça os dados exigidos pelo pagamento. Cobrir a sequência completa até `PAGA` usando apenas operações publicadas.

### R4 — [P1] PATCH do gozo altera referência de folha de um abono já pago

Local: `src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java:203-210`; chamada por `LicencaEspecialGozoService.afterUpdate`.

A sincronização protege somente a quantidade de dias quando o abono está `PAGA`. Depois sobrescreve incondicionalmente `mesAnoFolha` e `numFolha` a partir do gozo. O PATCH de reagendamento aceita esses campos e a guarda do gozo não os restringe após homologação. A guarda financeira da própria venda é contornada por essa atualização do recurso relacionado.

Prova: dado um registro vinculado já `PAGA`, com valor e data de pagamento preenchidos, o PATCH do gozo de competência janeiro/folha 1 para fevereiro/folha 2 é aceito. O abono permanece `PAGA`, conserva a data da liquidação original e passa a apontar para outra folha. Ver `CONFIRMED B`.

Este contraexemplo parte explicitamente de um estado pago existente. A reprodução não afirma que o fluxo atualmente bloqueado em R3 consegue criá-lo. A própria action publica `PAGA` como estado de destino e a sincronização contém tratamento para esse estado; ele precisa permanecer protegido quando existir.

Correção necessária: validar campos financeiros e efeitos no registro vinculado sob a transação e os locks apropriados, exigindo o procedimento de estorno/reconciliação para alteração de uma liquidação existente. Testar PATCH e lote a partir de `PAGA`.

### R5 — [P2] Limite de licença de 999 dias bloqueia cancelamento de averbação para aposentadoria

Local: `src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java:227-238`; chamadas em `TempoLicencaLifecycle.averbacao`.

A homologação aplica `validarImpactoOrigem` somente a `LICENCA_ESPECIAL`, mas cancelamento, desaverbação e reversão chamam a função sem esse recorte. A função impõe o teto de 999 dias de direito de licença mesmo quando a finalidade é aposentadoria/tempo de serviço e não existe período aquisitivo vinculado.

Prova: averbação de aposentadoria com `totalDias = diasFinalidade = 5000` é criada e homologada; não gera períodos; cancelar resulta em 422 com a mensagem de limite do direito de licença. Ver `CONFIRMED C`. Tempos superiores a três anos são admitidos pelo contrato dessa averbação.

Correção necessária: restringir a validação do direito de licença à finalidade e aos relacionamentos pertinentes, preservando as guardas comuns de dependências. Cobrir finalidades e quantidades distintas nas transições.

### R6 — [P2] Interrupção de gozo com abono proporcional rejeita uma parcela válida

Local: `src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaLifecycle.java:121-125`, em conjunto com `TempoLicencaValidation.java:122-125`.

Interromper ou encerrar antecipadamente reduz `diasGozo` para os dias efetivos, mas mantém `diasAbono`. Com política de abono sobre gozo (`G`, proporção 1/2), a validação passa a exigir outro valor de abono e rejeita a própria transição. A guarda de manutenção cadastral também impede alterar os parâmetros de abono depois de homologado.

Prova: parcela válida de 01 a 10/01/2026, dez dias de gozo e cinco de abono, homologada e iniciada; interromper em 05/01 falha com 422, `Dias de abono divergem da proporção da política efetiva`. O teste existente de interrupção cobre parcela sem abono. Ver `CONFIRMED D`.

Correção necessária: definir e executar atomicamente o tratamento do abono e de seu eventual pagamento na interrupção/encerramento, conciliando o débito efetivo e a liberação do restante. Não apenas suprimir a validação proporcional.

### R7 — [P2] PUT genérico apaga a data de efeito de conversão já realizada

Local: `src/main/java/br/com/praxis/ergonshowcase/service/PeriodoAquisitivoLicencaEspecialService.java:80-88`; mapeamento de `dtCntDobro` em `PeriodoAquisitivoLicencaEspecialMapper`.

O serviço protege `situacao` e `diasConvertidosDobro`, mas não protege `dtCntDobro`, que continua desserializável no UpdateDTO e mapeado para a entidade. `@UISchema(readOnly = true)` orienta apresentação, sem substituir a guarda transacional. A lista de campos protegidos do lote já reconhece a propriedade como controlada pelo workflow.

Prova: homologar período, converter dez dias por action e enviar manutenção cadastral com a mesma situação/quantidade, mas `dtCntDobro = null`; a atualização é aceita. O resultado permanece `CONVERTIDO_EM_DOBRO`, dez dias debitados, saldo 80 e data de efeito nula. Ver `CONFIRMED F`.

Correção necessária: preservar/proteger também a data e demais atributos de lastro pertencentes ao workflow na manutenção comum. Cobrir a coerência do ato completo, além do valor do contador.

## Scorecard obrigatório de completude

Os números abaixo são do auditor estrito reexecutado; **P** representa presença estática, não aprovação operacional. O arquivo `scorecard.md` contém as 25 linhas integrais, inclusive os Checks 13 a 16.

| Recurso | Colunas entidade / Response | Cobertura | Escritas contabilizadas | QuickViewCols | RichIdentity | ResourceReference / preview | MicroViz | RichEntityLookup | EditorialIssues | Filterable / variedade | QuickFilter |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Averbação | 30 / 33 | 100% | 30/30 | 4 | YES | 7 / 7 | 1 | P | 0 | 18 / 12 ops | 3 |
| Período | 33 / 36 | 100% | 33/33 | 5 | YES | 8 / 8 | 1 | P | 0 | 18 / 12 ops | 3 |
| Gozo | 22 / 25 | 100% | 22/22 | 5 | YES | 9 / 9 | 1 | P | 0 | 18 / 11 ops | 3 |
| Venda | 20 / 23 | 100% | 20/20 | 5 | YES | 9 / 9 | 1 | P | 0 | 19 / 10 ops | 3 |
| Radar | 19 / 21 | 100% | read-only | 4 | YES | 8 / 8 | 2 | P | 0 | 17 / 11 ops | 3 |

| Recurso | UiSurface | Intent PATCH | WorkflowAction | BulkEditable / BulkOps | ResourceFact | UiAnalytics | ReactiveDet | Invariantes/determinações testadas e resultado desta revisão |
|---|---|---|---|---|---|---|---|---|
| Averbação | 4 | 2 | 5 | 17 / 3 | 3 | 2 | 1 | Homologação, origem, desaverbação e recálculo presentes; R1 e R5 reprovam o comportamento |
| Período | 5 | 2 | 8 | 19 / 3 | 4 | 2 | 1 | Saldo, conversão, suspensão e prescrição presentes; R1 e R7 reprovam o comportamento |
| Gozo | 4 | 2 | 6 | 12 / 3 | 3 | 2 | 1 | Homologação, abono e interrupção presentes; R1, R2, R4 e R6 reprovam o comportamento |
| Venda | 2 | 1 | 5 | 8 / 3 | 3 | 2 | 1 | Autorização, pagamento, estorno e export presentes; R1 a R4 reprovam o comportamento |
| Radar | 1 | 0 | 0 | 0 / 0 | 3 | 2 | 0 | Propriedades JPA, analytics e export verificadas estaticamente; sem prova HTTP nesta revisão |

As annotations e os testes de inventário comprovam presença de detail/master-detail, intents, snapshot provider, metadados de versão/idempotência, facts, registry e analytics nos cinco controllers. O Check 13 e os testes verificam propriedades reais da entidade. Os formatos CSV/JSON/EXCEL e a exclusão de campos não exportáveis têm testes locais dos engines. Isso não prova que um usuário consiga completar o protocolo de lote ou todo o ciclo financeiro.

## Alcance e validação restante

| Camada | Parecer |
|---|---|
| Inventário estático e gates do script | PASS estático, 25/25 |
| Comportamento dos serviços | FAIL: contraexemplos R3 a R7, mais repetição de registro em R2 |
| SQL local das leituras de lote e migração | FAIL: R1 e R2 reproduzidos |
| Schema HTTP, capabilities e links em aplicação iniciada | Não executados; testes de annotations não substituem essa evidência |
| Protocolo completo de lote, filas/workers e concorrência | Não certificados; o callback real já falha antes |
| Idempotência e concorrência transacionais de item em PostgreSQL | Testes locais existentes com mocks; não certificados nesta revisão |
| Integração Neon e paridade Oracle | Não executadas |
| Materialização Angular | Não executada |

Não foram promovidas a achado hipóteses de deadlock ou de comportamento do driver sem reprodução conclusiva. A correção exige regressões dos cenários acima e nova execução dos gates pertinentes; os testes verdes atuais não autorizam encerrar a Issue.

## Reprodução e artefatos

- `Phase2ReviewProbe.java`: seis cenários de serviço, usando as fixtures já existentes em `target/test-classes`.
- `counterexamples.log`: resultado final dos seis cenários, exit 0.
- `PostgresBulkReadProbe.java`: cria banco local descartável, aplica o DDL V5 sem seed com pais sintéticos, executa o callback de lote nos quatro recursos e aplica a V6 integral; encerra o PostgreSQL ao final.
- `postgres-bulk-read.log`: erros `42601`, decisões do callback e resultados da migração. Apenas dados sintéticos.
- `mvn-test.log`: reexecução da suíte padrão.
- `scorecard.md`: auditoria estrita reexecutada.
- `review-manifest.json`: hashes e resumo dos resultados desta revisão.

Para repetir as provas Java, usar Java 21, o classpath registrado em `classpath.txt` (extraído do relatório Surefire), compilar com `javac -proc:none -encoding UTF-8 -cp <classpath> -d <diretorio-de-classes> <fonte>` e executar a classe qualificada `br.com.praxis.ergonshowcase.Phase2ReviewProbe`. Para a prova SQL, usar `postgres-classpath.txt`, que acrescenta os jars locais do embedded-postgres 2.2.2, binário PostgreSQL 14.22 e suas dependências, e executar `br.com.praxis.ergonshowcase.PostgresBulkReadProbe` a partir da raiz do host.

O banco local foi encerrado e seus arquivos de dados foram removidos após coleta dos resultados. Não há processo de banco deixado por esta revisão.
