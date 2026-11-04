# Slice 6 — Fase 2 / Issue #10

01/11/2026. Classificação: `contrato-publico`. Escopo: somente este host.
Baseline consumido: Metadata `8.0.0-rc.186`, Rules `0.1.0-beta.17`, Java 21.
Não há release ou alteração de biblioteca universal nesta entrega.

## Fonte e impacto

O starter é o dono de surfaces, intents, actions, precondições, envelopes de comando,
bulking, stats, export e determinações. O showcase fornece a política de domínio,
os candidatos, as transações e os adapters. As novas operações são consumíveis pelo
discovery oficial e pelos contratos existentes do Angular; não houve alteração do
runtime Angular, do Config ou de seu vocabulário.

Fontes legadas consultadas em
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/`:

- `pck_averb_oque_conta.pkb`, `INSERE_PERAQ` (134–163) e `REMOVE_PERAQ`
  (201–249): geração do direito e impedimento de remoção com consumo/contagem em dobro;
- `pck_erg_venda_licesp.pkb`, `MAIN_PRE` (90–103): ERG-03387 impede manutenção
  avulsa do abono vinculado ao gozo;
- `pck_erg_contdobro_licesp.pkb`: separação entre dias originais debitados e
  aproveitamento em dobro no tempo funcional.
- `pck_lic_esp.pkb`, `MAIN_POS` (305–333): abono proporcional ao direito (`D`)
  ou ao intervalo efetivo de gozo (`G`), com truncamento dos dias fracionários.

Não se afirma paridade integral de regras legadas: catálogo atômico, RuleSets,
lineage e testes de regras continuam sendo o trabalho dedicado da Fase 3.

## Operações

Todos os cinco recursos publicam `detail`, três ou quatro facts no ResponseDTO,
duas projections analíticas e os quatro modos de stats. O ranking nominal usa
bucket `id` e `recordOpen.sourceIdentityField=key`, apontando para a surface
`detail` do próprio recurso. Somente propriedades reais das entidades são
registradas em StatsFieldRegistry.

As coleções relacionadas usam as propriedades reais `averbacaoOrigemId`,
`periodoAquisitivoLicencaId` e `licencaEspecialGozoId`. A navegação a partir de
Vínculo usa a identidade composta `vinculoId`, com pessoa e número do vínculo;
um `numvinc` isolado seria ambíguo. Cada endpoint confere o pai, fixa o filtro de
parentesco e limita a página a 200 itens.

Os sete PATCHs usam DTOs próprios, presença explícita por PartialUpdateInput,
merge sobre o snapshot completo e validação do agregado. Ausência preserva o valor;
nulo explícito precisa ser aceito pelo contrato e pelo domínio. If-Match é
revalidado sob lock antes da escrita.

| Recurso | Ações de item | Ato coletivo |
|---|---|---|
| Averbação | Homologar, desaverbar, reverter desaverbação, cancelar | Homologar averbações |
| Período | Homologar, suspender, cancelar, converter em dobro, reverter conversão, prescrever, reconciliar saldo | Reconciliar saldos |
| Gozo | Homologar, iniciar, interromper, encerrar, cancelar | Homologar parcelas |
| Venda | Autorizar, pagar, estornar, reconciliar abono vinculado | Autorizar vendas |

As ações não aceitam um estado de destino arbitrário. O cadastro inicia em
`EM_ANALISE`, `EM_AQUISICAO`, `PROGRAMADA` ou `SOLICITADA`, respectivamente.
PUT/PATCH não substituem ações de ciclo de vida. Exclusão preserva o histórico
de registros que já avançaram no ciclo.

O preparo de duplicate-draft usa a base canônica e uma transação própria somente
de leitura. Copia apenas sugestões descritivas explicitamente selecionadas, sem
identidade, vínculo, chave única, datas, documentos, aprovações, origem de direito,
parcelas ou atributos livres. Preparar não persiste; a criação subsequente revalida
todo o contrato. A availability verifica leitura, criação, origem e órgão.

## Concorrência e contabilidade

Cada ação de item passa pelo GovernedResourceCommandExecutor. O recibo persistido
e o efeito pertencem à mesma transação JPA/PostgreSQL. A chave inclui solicitante,
órgão, recurso, item e ação. O fingerprint inclui o conteúdo tipado e If-Match.
Um advisory transaction lock serializa também duas primeiras requisições sem recibo.
Replay exige a autoridade atual e conserva corpo e revisão originais; outra carga
ou precondição com a mesma chave retorna conflito. Recibos não expiram automaticamente.

O período é bloqueado antes de conferir consumo. `recalcularPeriodo` e `sincronizar`
continuam sendo a única contabilidade: gozo reservado/efetivo + vendas avulsas +
abonos + dias originais convertidos. O cliente não escreve `diasGozados`,
`diasVendidos` ou `saldoDias`. Encerramento/interrupção conservam a previsão e
liberam a parte não usufruída. Suspensão, prescrição e cancelamento impedem
novo consumo, sem impedir a liberação de um débito existente por estorno.

Homologar gozo com abono gera um espelho de venda ligado à parcela. Esse espelho
não entra novamente na soma de vendas avulsas. Sua manutenção se faz pelo gozo;
uma venda vinculada não pode ser estornada/excluída como venda avulsa. Valor de
indenização não é fact público nem medida analítica e fica fora da allowlist de export.

Os três modos de lote usam ErgonBulkOperations e o kernel durável existente:
avaliação → proposta → confirmação → execução/recibos. Há descriptors e modelos
OpenAPI concretos, seleção EXPLICIT/QUERY conforme o modo, versões individuais,
resultados por item e sete endpoints de lifecycle. Disponibilidade operacional
continua exigindo instalação e publicação explícitas pelo control plane existente;
anotações e testes locais não tornam uma operação READY no banco.

## Migração e validação

`V6__tempo_licenca_ciclo_de_vida.sql` sucede a V5, migra os estados cadastrais,
adiciona a origem do abono/substituto e instala a tabela de recibos. É uma migração
beta do contrato: consumidores precisam abandonar REGISTRADO/CANCELADO como
vocabulário universal e usar o estado publicado por cada recurso.
Se houver registros antigos `ABONO` sem origem de gozo, a migração interrompe
antes de alterar dados: a V5 não fornecia lastro suficiente para inventar uma FK.
A carga de demonstração da V5 usa vendas avulsas `PECUNIA`.
O abono do seed permanece na própria parcela programada, com política `G` e
folha explícita; sua homologação cria o financeiro com FK. A correção do seed
foi feita na V5 local ainda não aplicada externamente, conforme as evidências
da Fase 1. Não se remove o preflight para abonos históricos órfãos.

Após a revisão da Issue #10, a V6 preserva como `PAGA` a venda com pagamento
registrado e como `GOZADA` a parcela com término efetivo. Não presume que um
término antecipado tenha sido uma interrupção, pois a V5 não registrava esse
motivo. Preserva cancelamentos e dias já destinados pelo período. Impede a
migração de pagamentos sem valor/folha, intervalos com dias incompatíveis e
conversões sem data coerente, exigindo reconciliação explícita antes do DDL.
A origem que já materializou período ativo na V5 permanece `HOMOLOGADA`, em vez
de voltar a `EM_ANALISE`. Períodos ativos com origem removida, totalmente
desaverbada ou incompatível interrompem a migração para reconciliação.

CSV, JSON e EXCEL reutilizam o executor e os engines do starter. Allowlists
curadas excluem observações, customFields e o valor restrito da indenização.
No rc.186, DomainGovernance não tem membro `exportable`; a exclusão é representada
pela política suportada `NON_EXPORTABLE` e aplicada por governedExportFields,
além da allowlist do recurso. Não foi inventado atributo de annotation.

Para atender também ao gate estrito de determinações, existem quatro prévias
não persistentes de criação: tempo líquido, saldo inicial, duração inclusiva e
saldo após venda. As definições apontam para operações reais e outputs dos DTOs.
Essas prévias não reservam saldo nem substituem a validação na confirmação.

Resultados finais: consultar `evidence/SLICE-6-FASE-2-SCORECARD.md` e
`evidence/SLICE-6-FASE-2-TESTS.txt`. O gate estático não aprova runtime.
Os testes de correção incluem aplicação da V6 no PostgreSQL local isolado sobre
o DDL V5 com pais sintéticos, além da autorização de leitura de lote nos três modos.
Esta execução não certifica migração externa, corrida transacional dos serviços, HTTP ou
materialização Angular. Não há publicação, aplicação de política remota ou
alteração de banco compartilhado autorizada/efetuada neste pacote local.
As decisões de duplicate-draft exibidas pelo inventário central são históricas
da Fase 1; o escopo desta Issue impede alterar aquele arquivo fora do host. A
adoção atual foi solicitada expressamente na Issue #10 e está implementada/testada
localmente, sem se atribuir aprovação independente ou certificação de runtime.

Os contratos são específicos deste showcase. Não há corpus HTTP, playground
ou documentação pública externa que espelhe estas novas operações; os artefatos
derivados alterados são os modelos OpenAPI de lote, configuração de lookups,
descriptors/instalador e esta documentação do host.

## Correções dos sete achados de revisão

R1: o adaptador de lote usa o nome físico de `@Table`, sem inferi-lo da rota.
R2: a migração preserva fatos históricos e as ações rejeitam novo pagamento
quando já existe data de liquidação, mesmo em um registro com estado incoerente.

R3: o PATCH financeiro existente pode apurar valor e observações do abono
autorizado. Origem, quantidade, documentos e folha permanecem controlados pelo
gozo; PUT, lote e outros PATCHs não contornam essa separação. Não há novo endpoint
nem execução de folha Oracle. Essa apuração financeira controlada estende o
cadastro legado sem afastar a guarda estrutural ERG-03387.

R4: a validação de gozo rejeita alteração dos dias/folha e cancelamento de abono
pago antes de aplicar o merge, e a sincronização conserva integralmente a venda
liquidada. Os escritores de abono vinculado seguem a ordem de locks gozo →
período → venda. A ordem é verificada localmente; não se afirma prova de corrida.

Na manutenção de averbações para licença especial, o produto limita a finalidade
a 999 dias. O Órgão 2 configura 180 dias no template de `CERG-06109`.
As guardas de dependências se aplicam a todas as averbações.

R6: encerrar/interromper recalcula o abono pelos dias efetivos segundo a política
`D`/`G`; a previsão original é preservada. Uma redução do abono não pago invalida
o valor anteriormente apurado; truncamento para zero encerra o espelho sem débito
pecuniário. Abono pago não é reduzido ou estornado implicitamente: se a redução
afetaria sua quantidade, o ato é rejeitado sem modificar os registros.

R7: manutenção cadastral não altera a data da conversão em dobro nem o lastro de
desaverbação/remoção, além dos contadores e estados já protegidos.

A revisão original permanece em `evidence/slice6-phase2-review/REVISAO.md` como
registro da árvore anterior. O fechamento das correções e os limites das provas
estão em `evidence/SLICE-6-FASE-2-CORRECOES.md`.

## Fechamento dos achados S1–S3

O teste de instalação executa agora a V5 **integral, incluindo o seed**, seguida
da V6 integral, com pré-requisitos sintéticos no PostgreSQL local. Também
confere que reservas, abono e venda avulsa não duplicam o débito do período.

`estornar-venda-licenca-especial` admite venda e abono. Preserva valor/data do
pagamento original, registra `dtEstorno` e conserva a identidade do histórico.
No abono, o estorno financeiro não libera por si os dias reservados pelo gozo;
cancelamento ou interrupção da parcela realizam o ajuste funcional pertinente.
O estorno e a reconciliação mantêm If-Match, Idempotency-Key e os locks existentes.

Após estorno, manutenção comum do gozo não reativa a autorização. O ato explícito
`reconciliar-abono-vinculado` pode emitir uma nova venda vinculada, com outra
identidade, sem copiar valor/pagamento anterior e sem alterar o registro
estornado. A resposta mantém a identidade e a revisão da origem, acrescentando
`autorizacaoAtualId`, referência 360° para a nova apuração. O replay conserva
essa referência. Trata-se de uma projeção do resultado do ato, não de coluna
JPA ou chave de stats. O banco permite histórico 1:N, mas somente um abono ativo
por gozo, mediante índice único parcial. Restrições adicionais impedem reativar
um registro com data de estorno ou estornar antes do pagamento original.

O campo `dtEstorno` tem documentação e governança próprias, filtros por janela
e existência, apresentação de exportação e proteção em create/PUT/lote. Datas
desconhecidas de estornos legados continuam nulas. A pesquisa de modalidade e o
quick filter usam `ABONO_JUNTO_GOZO`; não há alias para a modalidade antiga.

Rastreabilidade dos três achados e provas em
`evidence/SLICE-6-FASE-2-S1-S3.md`. As revisões anteriores permanecem como
snapshots históricos, sem atribuir aprovação independente à implementação atual.
