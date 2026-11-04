# Validação local — Issue #9, Slice 6, Fase 1

> Registro histórico da implementação inicial. Para os bytes após as correções,
> consulte [o relatório atual](../CORRECOES-SLICE-6-2026-11-01.md) e
> [o scorecard atual](SLICE-6-CORRECOES-SCORECARD.md).

> Este é o registro dos gates executados na implementação. A
> [revisão posterior](../REVISAO-SLICE-6-FASE-1-2026-11-01.md) reproduziu falhas
> funcionais que esses testes não detectavam. Os resultados verdes não aprovam
> filtros, lookups, escrita ou concorrência para uso operacional.

01/11/2026. Estado: **CHANGES REQUESTED para aceite integral**.

Dependência efetivamente compilada: `praxis-metadata-starter:8.0.0-rc.186`.
SHA-256 do JAR selecionado:
`99da8ce5bc0a10405b3f93de9356015e964562b72c61ae4d8b0a59f58b71508b`.
Toolchain: `D:/Developer/JAVA/graalvm-jdk-21.0.2+13.1` e Maven 3.9.6.

## Comandos e resultados

| Gate | Resultado |
|---|---|
| `mvn test "-Dtest=!*Neon*"` | **BUILD SUCCESS**, 220 testes, 0 falhas, 0 erros, 0 ignorados na execução selecionada; 47,596 s |
| `python scripts/workspace/audit-java-resource-completeness.py praxis-ergon-showcase --strict` a partir do workspace | **Exit 1**, 25 entidades e 25 bindings resolvidos, 21 linhas PASS, 4 linhas FAIL e 5 decisões de duplicação pendentes |
| Qualidade editorial do auditor | **0 issues** nos 25 recursos |
| `SemanticMetadataReviewer` | Sem erros para todos os DTOs e facts dos 25 recursos |
| `PraxisExpressionStaticValidator` | Cinco novos ResponseDTOs resolvidos pelo CustomOpenApiResolver; expressões válidas e microvisualizações efetivamente emitidas |
| Inventário independente de lookups | 23 fontes esperadas publicadas, incluindo as quatro novas; sem mismatch |

O [scorecard completo](SLICE-6-FASE-1-SCORECARD.md) é a saída real do gate estrito.

## Recorte de completude dos cinco recursos novos

| Recurso | Colunas de negócio | Cobertura DTO | Escrita contabilizada | Filterable / operadores | Quick View | Identity rica | Referências com preview | MicroViz | Editorial |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|
| Averbações | 30 | 100% | 30/30 | 17 / 12 | 5 | Sim | 7/7 | 1 | 0 |
| Períodos de licença | 33 | 100% | 33/33 | 17 / 12 | 5 | Sim | 8/8 | 1 | 0 |
| Gozo de licença | 21 | 100% | 21/21 | 17 / 11 | 5 | Sim | 8/8 | 1 | 0 |
| Vendas de licença | 19 | 100% | 19/19 | 17 / 10 | 5 | Sim | 8/8 | 1 | 0 |
| Radar | 19 | 100% | Somente leitura | 17 / 11 | 5 | Sim | 8/8 | 1 | 0 |

Todos publicam três Quick Filters, uma superfície `detail` e um ResourceFact com
ícone, tom e thresholds. WorkflowAction, BulkEditable/BulkOperation,
ResourceIntent, UiAnalytics e ReactiveDeterminations não foram implementados
para estes recursos na Fase 1. Os testes novos verificam integridade cadastral e
hidratação; não certificam invariantes transacionais de concessão/venda/conversão.
O scorecard apresenta os contadores completos, inclusive zeros e reprovações.

## Provas e limites

`TempoLicencaSlice6Phase1ContractTest` executa oito testes locais: round-trip de
todas as propriedades de negócio das quatro entidades, identidade/versão
preservadas no merge, hidratação de gozo e venda, rejeições de pai ausente,
chaves incompatíveis e órgão divergente, coerência QuickFilter/FilterDTO,
cascata/reload declarados, novo parâmetro false/true/false, proteção de consulta e
exclusões por órgão e ausência do valor indenizatório na exportação cadastral.

A validação não acessou banco, não executou a V5, não iniciou servidor ou navegador,
não provou consultas reais de lookup, reload contextual, orçamento de queries,
transações ou adoção Angular. O checkout não contém V1–V4; a V5 depende do schema
anterior. A configuração mantém Flyway automático desabilitado.

O bloqueio de `25/25 STATIC PASS`, os atributos ausentes em DomainGovernance do
rc.186 e a formalização de decisões no diretório de inventários estão detalhados
em [SLICE-6-FASE-1.md](../SLICE-6-FASE-1.md). Nenhum gate foi enfraquecido e nenhuma
aprovação independente foi inferida de testes verdes.
