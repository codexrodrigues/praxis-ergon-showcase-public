# Validação do CRUD — Slice 6, Fase 1

01/11/2026. Java 21, Maven 3.9.6, `praxis-metadata-starter:8.0.0-rc.186`.

**Snapshot histórico da entrega.** A
[revisão posterior](../REVISAO-CRUD-SLICE-6-2026-11-01.md) manteve os 233 testes
verdes, reproduziu contraexemplos adicionais e registrou 51 entradas no auditor
atual. Os 36 itens abaixo pertencem à execução original; não são a contagem
atual nem uma aprovação funcional das regras de consumo.

## Resultado executado

| Gate | Resultado |
|---|---|
| `mvn test "-Dtest=!*Neon*"` | **233 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS** |
| Auditor de completude `--strict` | **Exit 1**, 36 reprovações: quatro de Fases 2/3 no Slice 6, onze de outros recursos e 21 de RuleCatalogParity |
| Decisões de duplicate-draft | **Zero pendências, zero divergências de hash** no inventário |
| Editorial dos cinco recursos Slice 6 | **Zero issues** |
| Cobertura Entity→DTO dos cinco recursos Slice 6 | **100%** |
| Conteúdo desta rodada | CRUD versionado, invariantes, saldo sincronizado, guardas de exclusão, formalização do inventário e documentação |

O [scorecard completo](SLICE-6-CRUD-SCORECARD.md) é a saída atual do auditor, sem
alterações no script. Seus 10 PASS globais não são comparáveis a um aceite de
runtime. Os cinco recursos do Slice 6 mantêm zero problemas editoriais; os quatro
mutáveis ainda são reprovados somente pelas capacidades de Fases 2/3:
BulkEditable/BulkOperation, UiAnalytics, ReactiveDeterminations e, para as duas
entidades extensas, ResourceIntent. O radar passa os gates estáticos.

O requisito de restarem **apenas quatro reprovações no host inteiro** não foi
atingido. O auditor atual acusa também guardas de exclusão/editorial em onze
recursos anteriores e 21 vínculos ausentes ou sem teste no catálogo de regras.
Essas áreas não foram alteradas nesta implementação. O pedido não foi usado para
reduzir os requisitos do auditor, nem para alterar o trabalho local dos outros
recursos. A Issue não recebe aceite global a partir de um recorte verde.

## Provas de serviço

`TempoLicencaCrudContractTest` contém seis testes que exercitam os serviços reais
com mappers reais e EntityManager/repositórios simulados:

1. Create, PUT sem precondição (428), token divergente (412), token correto e delete nos quatro serviços;
2. Pai cancelado/sem saldo, venda vedada, limites mínimo/máximo, VEDAR/HERDAR negativo, abono incompatível e duração divergente (422);
3. Sincronização conjunta de gozo, abono e venda, alteração, cancelamento e exclusão em lote;
4. Edição do próprio gozo sem dupla contagem e rejeição de uma segunda venda que excederia o saldo;
5. PERMITIR e HERDAR positivo, e rejeição de totais de consumo inventados no cadastro do período;
6. Exclusão impedida de período com filhos e averbação com períodos vinculados (409).

O teste de contenção foi substituído e a disponibilidade do host não tem mais a
negação temporária. Os demais testes de filtros, cascata, JSONB, tri-state,
Quick View, identidade, microvisualização e revisionamento do radar continuam
na suíte. Nenhum teste finge execução de workflow, liquidação de folha ou prova
de serialização PostgreSQL.

## Consistência do inventário

Documento: `ergon-duplicate-draft-review-20261028.md`.
SHA-256: `a2d112659769c96cf2b0eb7bf145b5da57197ffcabc5a829b32e67843e3d94cc`.

As nove identidades dos Slices 5 e 6 foram formalizadas como não aplicáveis e
todas as referências ao documento receberam o hash atual. Cargos preserva a
decisão implementada existente. O hash prova integridade do documento; não
substitui revisão independente ou aceitação funcional.

## Limites

Não houve deploy, release, alteração dos starters ou execução de banco. A V5
continua não aplicada. Locks pessimistas e flushes são invocados pelos serviços,
mas os testes locais não provam concorrência, rollback e locks efetivos em
PostgreSQL. Não houve servidor HTTP ou navegador: o status das precondições foi
verificado nas exceções canônicas dos serviços, não por tráfego autenticado.
Os resultados históricos de 220/228 testes e da contenção são anteriores a esta
rodada. O contrato atual é descrito em [SLICE-6-FASE-1.md](../SLICE-6-FASE-1.md).
