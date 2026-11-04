# Slice 8 — Onda 2: levantamento inicial, não concluída

Veredito: NÃO APROVADA. Este registro não é entrega nem aceite da Onda 2. Nenhum código de produto foi alterado nesta rodada. O baseline do worktree e o HEAD estão em baseline-status.txt e baseline-head.txt; o worktree já estava extensamente alterado. Não houve commit, deploy ou operação Neon.

## Contratos encontrados

- `SetorController` publica somente a ficha temporal no trecho examinado; a referência temporal em `SetorService` codifica id do período, data e hierarquia e valida órgão e vigência. Não quebrar esse caminho.
- `GenericCrudService.filterResponse` do Angular executa GET de surface COLLECTION com filtros escalares na query e aceita lista ou página. A surface precisa de schema de resposta e escopo real.
- `DesignacaoFuncaoFilterDTO.setorId` existe. `RadarOrganogramaChefiaOcupacaoFilterDTO.setorPai` é igualdade sobre o pai imediato: não comprova uma subárvore. O mapping de filhos deve ser provado ponta a ponta sem confundir estes conceitos.
- `OrganogramaPhaseOneAvailability` bloqueia as operações cadastradas dos cinco recursos. Preserve o bloqueio e revise todas as novas entradas, inclusive bulk e intents, em discovery e execução.
- O plano diretor distingue provimento, exercício e folha, bem como as regras hierárquicas de quadro. Não inferir ocupação temporal a partir de dados não integrados.

## Pendências integrais

Árvore, filhas e filtros reais, intents, comandos e simulação, locks/receipts, sincronização transacional, duplicação, bulk, facts, analytics, export, regressões focais e PostgreSQL descartável continuam por fazer. A Onda 3 não foi iniciada. A comparação completa dos sete recursos e revisão independente também estão pendentes. Resultados do baseline anterior não são gate desta onda.

V11 SHA-256: D6868E470A99494B40DFDB73405888FF8AB5BC2629258BA7A0A205AE41981352
V12 SHA-256: F810507D658EE58ABF3F8ACAF4A531B993E2358F41D59F7D311F517663354B7C
`nGate de baseline JDK 21: 13 testes, 0 falhas, erros ou skips; ver gate-baseline.log. Este resultado não valida a Onda 2.
