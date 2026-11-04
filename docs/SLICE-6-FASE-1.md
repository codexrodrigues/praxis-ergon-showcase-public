# Slice 6 — Fase 1: tempo de serviço e licença especial

Atualizado em 01/11/2026. Classificação: `contrato-publico`.
Baseline: `praxis-metadata-starter:8.0.0-rc.186`, Java 21.

**Aceitação global ainda pendente.** Os sete achados da
[revisão do CRUD](REVISAO-CRUD-SLICE-6-2026-11-01.md) receberam correções e
regressões em [CORRECOES-CRUD-SLICE-6-R9-R15.md](CORRECOES-CRUD-SLICE-6-R9-R15.md).
As provas são locais; não certificam aplicação da V5, execução HTTP,
concorrência PostgreSQL ou materialização visual. Consulte o scorecard e as
limitações atuais no relatório de correção, preservando os relatórios anteriores
como snapshots históricos.

## Estado atual

Os quatro recursos de averbação, período aquisitivo de licença especial, parcela
de gozo e venda executam CRUD real. A contenção `TempoLicencaWriteAvailability`
foi removida, inclusive sua delegação no provider de disponibilidade do host.
O radar continua somente leitura. O gate estrito global permanece reprovado;
a aprovação integral não é inferida da passagem dos testes locais.

| Caminho sob `/api/ergon` | Comportamento |
|---|---|
| `averbacoes-tempo-servico` | Cadastro do tempo certificado, finalidade e posição consolidada de desaverbação |
| `periodos-aquisitivos-licenca-especial` | Ciclo de direito, parâmetros individuais e contabilidade de dias |
| `licencas-especiais-gozo` | Parcelas vinculadas ao ciclo, com gozo e abono |
| `vendas-licenca-especial` | Conversões pecuniárias vinculadas ao ciclo |
| `radar-tempo-servico-licenca-especial` | Projeção de saldo, tempo reconhecido, programação e prescrição |

ResourceKeys: `administracao-pessoal.` seguido do nome correspondente acima.
ApiPaths mantém os caminhos centralizados. Cada recurso publica identidade rica,
Quick View, referências com preview, microvisualização e três Quick Filters.
As classes concretas dos cinco filtros declaram os 17 predicados executáveis no
rc.186. Os lookups normalizam o órgão em filter/GET by-ids/POST by-ids; as entidades
publicam contêineres CustomFields. X-Tenant continua sendo contexto demonstrativo,
não autorização por identidade autenticada; sem header, usa o órgão 1.

## CRUD e precondições

Create e delete individuais usam os fluxos herdados de ErgonResourceService.
O PUT exige ResourceVersionUpdatePrecondition: ausente retorna 428 e ETag
incompatível retorna 412. A entidade é carregada no escopo do órgão e bloqueada
com PESSIMISTIC_WRITE; é recarregada sob o lock antes da comparação de versão.
CustomFields read-only são preservados em torno do mapper. Após save/hooks,
flush/refresh capturam corpo e revisão persistida da mesma representação.

A exclusão em lote chama o fluxo individual para cada identidade em ordem,
dentro da mesma transação, preservando guardas e sincronização. Período com
qualquer parcela de gozo/venda, inclusive cancelada, não pode ser excluído (409).
Averbação referenciada por período aquisitivo também não pode ser excluída (409).

As parcelas são criadas selecionando o período e os dados próprios do evento;
órgão, pessoa, vínculo e chave espelhada podem ser omitidos e são hidratados pelo
pai. Divergências explícitas são rejeitadas. A transferência de parcela para
outro período via PUT é rejeitada com 422: evita mudar o lastro do direito por
um merge cadastral. A movimentação entre ciclos exige operação de domínio própria.

## Invariantes e contabilidade

Antes de alterar uma parcela, o serviço bloqueia e recarrega o período pai.
Rejeita novas parcelas/reativações em pai cancelado, consumo acima do saldo e
inconsistências de identificação. O estorno por CANCELADO libera consumo mesmo
se a política mudou ou o pai já estava cancelado. Cancelar um pai novo exige
cancelar antes suas parcelas ativas; registros legados com pai cancelado podem
ser regularizados sem excluir a história.

Os dias de gozo correspondem à duração inclusiva entre dtini e dtfim, quando
encerrado. Enquanto dtfim está vazio, correspondem a dtini..dtprevfim e reservam
saldo. A previsão original permanece no encerramento: gozo 01/01..10/01,
encerrado em 05/01, informa diasGozo=5 e libera cinco dias de reserva.
Sem abono, diasAbono é zero e iniFimAbono fica vazio. Com abono, a quantidade é
positiva e a posição I/F é obrigatória.

Se o início do gozo antecede ou coincide com o fim da aquisição, VEDAR rejeita
antecipação. HERDAR consulta a matriz ativa pelo resolver existente, com órgão,
regime, tipo de vínculo, categoria, subcategoria e vigência; ausência de permissão
explícita não libera gozo. PERMITIR é o override individual positivo.
Venda usa a política efetiva: N veda, S permite, I ou ausência exige permissão
da matriz ativa. Falta de decisão não autoriza venda. Mínimo e teto individuais
prevalecem; quando ausentes, são herdados da matriz. O mínimo é por venda avulsa;
o teto soma vendas e abonos, independentemente da ordem em que foram criados.

O abono também exige permissão pecuniária e base D/G válida. D aplica a fração
ao direito e admite um único abono ativo; G aplica a fração aos dias de gozo.
A quantidade deve ser o inteiro truncado de base × numerador / denominador.
Base N, decisão ausente ou proporção incompleta/inválida rejeitam o abono.

A resolução temporal segue os packages consultados: venda usa a maior data
entre fim da aquisição e exercício (início da venda quando ambas ausentes).
Gozo usa fim da aquisição, ou término efetivo/previsto quando a aquisição
antecede o exercício, e procura no início do gozo se não encontrar matriz.
Override explícito de antecipação continua prevalecendo sobre a matriz.

Após create/update/delete de gozo e venda, as parcelas REGISTRADO são somadas:

- diasGozados = soma de diasGozo;
- diasVendidos = soma das vendas + soma dos diasAbono das parcelas de gozo;
- saldoDias = diasDireito - diasGozados - diasVendidos - diasConvertidosDobro.

Na edição, o consumo anterior do próprio item é liberado na comparação para
evitar dupla contagem. Os cálculos usam long e conversão exata, recusando saldo
negativo. Os totais diasGozados/diasVendidos/saldoDias são derivados no servidor,
exclusivos do ResponseDTO e publicados com readOnly. Não são campos de
CreateDTO/UpdateDTO do período. Para criar direito de 30 dias, informe
diasDireito=30; o servidor retorna saldoDias=30 sem cálculo manual do cliente.
Gozo total inclui a reserva de parcelas abertas e o consumo efetivo das fechadas.

Nova averbação de origem (em create ou troca no PUT) deve pertencer ao mesmo
órgão/vínculo, estar REGISTRADO, não removida e ter finalidade LICENCA_ESPECIAL.
Uma referência histórica já existente pode ser mantida mesmo após cancelamento
ou remoção da origem, sem autorizar novas associações a ela.

## Metadata, UX e limites de domínio

O override individual permGozoLicEspAntFimPe usa HERDAR/PERMITIR/VEDAR no wire;
o converter mantém NULL/TRUE/FALSE na coluna existente. O parâmetro geral de
ParametroContagemFerias continua Boolean, default false. Não existem aliases
boolean/enum paralelos. A mudança beta requer os três valores textuais nos
consumidores do override individual.

Datas distinguem serviço, aquisição, gozo, previsão, venda e pagamento. O valor
indenizatório fica fora da exportação cadastral e das medidas estatísticas.
A governança usa os atributos efetivamente disponíveis no JAR selecionado.
O radar não anuncia revisão forte baseada só em pa.version: suas dependências e
a data corrente podem mudar sem alterar essa versão.

A desaverbação ainda é posição consolidada. A Fase 2/3 deverá ampliar atos,
trilha histórica, workflow, idempotência e determinações reativas. CRUD cadastral
não é prova de equivalência integral aos packages Oracle nem de liquidação de
folha, e a abertura do CRUD não encerra esses escopos futuros.

## Fontes e migração

Fontes primárias consultadas em
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps`:
AVERBACOES_CONTA.tab, AVERB_OQUE_CONTA.tab, DESAVERBACOES_CONTA.tab,
PERAQLICESP.tab, ERG_CONTDOBRO_LICESP.tab, LIC_ESP.tab, ERG_VENDA_LICESP.tab e
respectivos packages/views sob fontes_ergon. A revisão anterior detalha essas
referências e as Tarefas 39127/58337/58924/82601/103339.

A V5 cria as quatro tabelas, FKs, índices, domínios e seed demonstrativo.
O checkout não contém V1–V4: a V5 depende do schema anterior admitido, não é
bootstrap de banco vazio. Flyway automático continua desativado. A V5 não foi
aplicada a banco nesta execução.

## Decisão de duplicate-draft

As decisões dos Slices 5 e 6 estão formalizadas como não aplicáveis no documento
`ergon-duplicate-draft-review-20261028.md`, no diretório de inventários autorizado.
O inventário esperado referencia o hash atualizado desse documento em todas as
entradas pertinentes. Não há pendências de duplicate-draft. Cargos preserva sua
decisão implementada; a formalização não declara revisão independente do CRUD.

## Evidência de aceitação

Consulte [as correções e os gates atuais](CORRECOES-CRUD-SLICE-6-R9-R15.md).
As [provas da entrega anterior](evidence/SLICE-6-CRUD-VALIDACAO.md) e
[seu scorecard](evidence/SLICE-6-CRUD-SCORECARD.md) permanecem históricos.
Os relatórios de revisão e contenção anteriores são históricos; a contenção de
escrita descrita neles foi removida por este pedido explícito.

O teste de fluxo de serviço cobre os quatro creates, PUT sem precondição,
precondição divergente e correta, deletes, saldo após create/update/cancel/delete,
VEDAR/HERDAR/PERMITIR, pais cancelados, abono inconsistente e vínculos impeditivos
de exclusão. Esses testes usam EntityManager/repositórios simulados. Não provam
serialização PostgreSQL, aplicação Flyway, integração HTTP nem UX em navegador.

As correções R9–R15 acrescentam quinze regressões, incluindo negações de
herança/abono, teto combinado, estorno, origem histórica, fechamento efetivo e
schema de entrada sem totais derivados. A suíte sem Neon tem 248 testes verdes;
os resultados e a reprovação estrita estão no relatório de correção atual.
