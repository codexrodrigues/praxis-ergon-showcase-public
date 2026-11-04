# Correções das sete inconsistências do CRUD Slice 6

01/11/2026. Java 21; `praxis-metadata-starter:8.0.0-rc.186`.
Classe: `contrato-publico`. [Plano e impacto](PLANO-CORRECOES-CRUD-SLICE-6.md).

Os achados R9–R15 foram corrigidos no domínio do showcase e cobertos por
regressões locais. **Isso não encerra o aceite corporativo integrado nem torna
verde o auditor estrito.** A revisão anterior e seus probes permanecem evidência
histórica; os testes abaixo passam a exigir os comportamentos corrigidos.

## Correções e provas de regressão

| Achado | Comportamento corrigido | Prova em TempoLicencaSevenFixesRegressionTest |
|---|---|---|
| R9 / P1 | I herda a permissão e limites da matriz efetiva; ausência/negação não autoriza venda; S/N explícitos prevalecem | `c1InheritedPermissionDeniesFalseMissingInactiveAndForeignMatrixBeforeSave`, `c1InheritedLimitsAndExplicitOverridesRespectAcquisitionAndExerciseDates` |
| R10 / P1 | Abono exige permissão pecuniária, base D/G e quantidade conforme fração; D admite um único abono ativo | `c2AbonoCannotBypassItsOwnPolicySalePermissionOrRatio`, `c2InheritedAbonoAndRightsBaseAreAppliedAndOnlyOneRightsAbonoIsAllowed` |
| R11 / P1 | Teto soma abono e venda nas duas ordens de criação e nos updates, excluindo o próprio débito anterior | `c3BothCreationOrdersAndUpdatesShareOnePecuniaryLimit` |
| R12 / P2 | CANCELADO estorna sem depender da política atual; pai legado cancelado permite estorno do filho; reativação volta a exigir elegibilidade; pai com filhos ativos não pode ser cancelado | `c4SaleCanBeCancelledAfterPermissionAndLimitsChangeWithoutDeletingHistory`, `c5LegacyCancelledParentStillAllowsChildEstornoButNotReactivation`, `c5NewParentCancellationRequiresChildrenToBeCancelledFirst` |
| R13 / P2 | Nova origem e troca por PUT exigem averbação registrada, não removida e destinada à licença; referência histórica existente pode ser retida | `c6NewOriginRejectsCancellationRemovalAndWrongPurposeSeparately`, `c6ExistingHistoricalOriginCanBeRetainedButNewAssociationCannotUseIt`, `c6UpdateCannotIntroduceAnIneligibleOrigin` |
| R14 / P2 | Gozo aberto reserva pela previsão; encerrado usa término efetivo e preserva a previsão histórica | `c7ActualClosureReleasesReservationWhileOriginalForecastIsRetained`, `c7ClosedCreateUsesActualDurationInsteadOfForecast` |
| R15 / P2 | Três totais saem de create/update e são calculados no servidor; response permanece completo, read-only e com microvisualização | `c8NonDefaultRightAndSubsequentEditsDeriveTotalsWithoutClientArithmetic`, `c8RequestSchemasOmitDerivedTotalsAndResponsePublishesReadonlyMicroVisualization` |

Os quinze testes novos executam serviços e mappers reais com armazenamento
Mockito. A fixture passou a distinguir os períodos nas consultas de parcelas e
a gerar identidades sucessivas dentro de cada recurso. Isso evita que a soma de
outro período ou IDs repetidos ocultem um erro contábil.

O teste anterior de round-trip continua verificando todas as colunas: os três
totais são preenchidos como valores do servidor, não podem ser escritos pelos
DTOs de entrada, não são sobrescritos por applyUpdate e aparecem na resposta.
Os testes anteriores de CRUD, 428/412, hidratação, escopo, filtros, JSONB,
tri-state, Quick View e referências continuam na suíte.

## Fonte e responsabilidade canônica

`EffectiveSpecialLeavePolicy` centraliza a seleção da matriz e a precedência dos
overrides. Reutiliza a seleção existente de parâmetros por órgão, vínculo,
categoria/subcategoria e vigência. `TempoLicencaValidation` aplica a política e
mantém uma base contábil comum para validação e recálculo do período.

As datas de referência e fórmulas foram conferidas nas fontes em
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon`:

- `pck_erg_venda_licesp.pkb:174–274`: matriz pela maior data de aquisição/exercício,
  precedência individual, venda, mínimo e teto, incluindo o abono;
- `pck_lic_esp.pkb:213–325`: seleção temporal e fallback no início do gozo,
  antecipação, abono vedado, bases D/G, proporção e unicidade da base D.

O host possui as regras específicas do domínio. O starter continua dono de
schema, metadata, resource version e precondições. Não houve alteração dos
starters, rotas, resourceKeys ou das bibliotecas universais Angular.

## Migração de contrato beta e UX

`diasGozados`, `diasVendidos` e `saldoDias` deixam de existir em
CreatePeriodoAquisitivoLicencaEspecialDTO e UpdatePeriodoAquisitivoLicencaEspecialDTO.
Permanecem no ResponseDTO, com `@Schema(accessMode=READ_ONLY)`, `@UISchema(readOnly=true)`
e a apresentação de saldo existente. Não há alias ou trilha de compatibilidade.
Clientes e agentes devem deixar de enviar esses totais e usar a resposta do
servidor. O schema efetivo de entrada foi verificado com o resolver do rc.186.

Exemplo de entrada de um período com direito não padrão:

```json
{
  "descricao": "Ciclo de licença com direito de 30 dias",
  "empCodigo": 1,
  "numfunc": 1001,
  "numvinc": 1,
  "chave": 2,
  "dtini": "2020-01-01",
  "dtfim": "2024-12-31",
  "diasDireito": 30,
  "diasConvertidosDobro": 0
}
```

Sem filhos, a resposta tem `diasGozados=0`, `diasVendidos=0`, `saldoDias=30`.
Editar o direito para valor inferior ao já consumido é recusado antes de salvar.
Para encerrar um gozo reservado de dez dias após cinco dias de fruição, mantenha
a previsão original, informe o término efetivo e diasGozo=5. Abono na base G
também deve respeitar a quantidade decorrente da duração efetiva.

## Gates executados

| Gate | Resultado |
|---|---|
| Testes focais antes das duas últimas ampliações | 34 testes, zero falhas/erros; BUILD SUCCESS |
| `mvn test "-Dtest=!*Neon*"` com Java 21 | **248 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS** |
| Novas regressões R9–R15 na suíte final | **15 testes, todos verdes** |
| Auditor `--strict` | **Exit 1; 51 entradas globais de reprovação** |
| Editorial / cobertura response no Slice 6 | Zero issues / 100% das colunas de negócio |
| duplicate-draft | Zero pendências ou divergências no inventário |

[Scorecard bruto final](evidence/SLICE-6-CRUD-R9-R15-SCORECARD.md).
[Resumo sanitizado da suíte](evidence/SLICE-6-CRUD-R9-R15-TESTS.txt).
Os 51 itens são 15 recursos reprovados e 36 achados de paridade de regras.
Quatro recursos pertencem ao Slice 6; os outros onze são anteriores. Capacidades
de Fases 2/3 permanecem ausentes no Slice 6: bulk, analytics, determinações e
intents para as entidades extensas. O radar continua com PASS estático.

O auditor também acrescenta, na mesma linha do período, `Unaccounted business
writes: diasGozados, diasVendidos, saldoDias` (30/33). Seu algoritmo só contabiliza
entrada mutável ou campo read-only escrito por WorkflowAction/ResourceIntent;
não percorre o recálculo dos hooks de CRUD. **O resultado não foi suprimido.**
A responsabilidade desses três campos está explicitamente fechada no serviço,
mapper, ResponseDTO e nos testes C8, mas não recebe aceite automático do auditor.
Uma evolução desse gate deve provar campos derivados pelo fluxo de escrita e
testar negações, sem criar uma allowlist de nomes nem comandos artificiais de
"editar saldo" para satisfazer a contagem.

| Recurso | Colunas / response | Cobertura | Escrita segundo auditor | QV | Identidade rica | Refs preview | MicroViz | Lookup rico declarado | Editorial | Filtros / operadores | QuickFilters |
|---|---|---|---|---:|---|---:|---:|---|---:|---|---:|
| Averbação | 30 / 32 | 100% | 30/30 | 4 | Sim | 7 | 1 | Sim | 0 | 17 / 12 | 3 |
| Período licença | 33 / 35 | 100% | 30/33 + 3 derivados provados localmente | 5 | Sim | 8 | 1 | Sim | 0 | 17 / 12 | 3 |
| Gozo licença | 21 / 23 | 100% | 21/21 | 5 | Sim | 8 | 1 | Sim | 0 | 17 / 11 | 3 |
| Venda licença | 19 / 21 | 100% | 19/19 | 5 | Sim | 8 | 1 | Sim | 0 | 17 / 10 | 3 |
| Radar | 19 / 21 | 100% | Somente leitura | 4 | Sim | 8 | 1 | Referências reutilizadas | 0 | 17 / 11 | 3 |

Nos cinco recursos: 1 surface, 1 fact, 0 intents, 0 workflow actions,
0 BulkEditable/BulkOps, 0 UiAnalytics e 0 ReactiveDet. Invariantes testadas:
CRUD/versionamento/dependências nos quatro mutáveis e as quinze regressões
acima; determinações reativas ainda não implementadas. As contagens de lookup
e UX são declarações/schema local, não prova visual ou de consumo HTTP.

## Preservação, limites e próximo gate

A rodada manteve o trabalho local, sem reset, clean, stash, commit ou alteração
de banco. O código alterado ficou nos arquivos do Slice 6; houve ainda atualização
de seus testes e documentação. O auditor e os inventários não foram modificados.
Manifestos, diferenças incrementais e logs ficam em `.local/slice6-seven-fixes/`;
a evidência resumida versionável fica em `docs/evidence/SLICE-6-CRUD-R9-R15-*`.

Não foi aplicada a V5 nem iniciado um host HTTP ligado a banco. Os mocks não
provam constraints, transações, concorrência, rollback, incremento real de versão
ou autorização multiórgão. Não houve navegador: a mudança de UX foi provada na
remoção dos inputs e no schema read-only, sem prova de renderização desktop/narrow.
O próximo gate integrado exige instalação admitida da V5 em banco descartável,
HTTP autenticado e validação visual por consumidor oficial.

Permanecem os riscos corporativos registrados na revisão: ordem de locks entre
update/delete do agregado, custo de sincronização em lotes grandes, identidade
autorizada por órgão e trilha administrativa completa. Eles não foram declarados
resolvidos pelos testes locais nem incorporados silenciosamente à correção dos
sete achados. O aceite global permanece **CHANGES REQUESTED**.
