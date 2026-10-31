# Scorecard T08 — completude e evidências

**Estado: APROVADO no gate local e estático (STATIC PASS, exit code 0); praxis-metadata-starter alinhado para 8.0.0-rc.183; adoção de Cargo implementada e homologada (D02 e D04).** Os seis recursos têm 100% de cobertura estrutural dos campos de negócio contabilizados e zero problemas editoriais. Isso não equivale a aprovação funcional, paridade estatutária integral ou aceite visual.

| Recurso | Campos de negócio / resposta | Escritas contabilizadas | Filtros / operações | Quick filters | Ações / intents / fatos | BulkEditable / BulkOps | Lacuna de adoção |
|---|---:|---:|---:|---:|---:|---:|---|
| Órgãos | 12 / 12 | 12/12 | 11 / 7 | 2 | 2 / 0 / 0 | 0 / 0 | Duplicate-draft não aplicável ao cadastro institucional atual |
| Cargos | 17 / 22 | 17/17 | 16 / 8 | 4 | 2 / 0 / 2 | 5 / 2 | **Implementada (CargoDuplicateDraftDTO, D02 flags 
ull, D04 origem VIGENTE)** |
| Pessoas | 68 / 71 | 68/68 | 25 / 9 | 2 | 0 / 1 / 2 | 7 / 2 | Duplicate-draft não aplicável à cópia de pessoa física |
| Vínculos | 59 / 74 | 59/59 | 33 / 12 | 6 | 9 / 2 / 4 | 10 / 4 | Novo provimento exige criação explícita; cópia genérica não aplicável |
| Setores | 12 / 12 | Somente leitura | 7 / 5 | 2 | 0 / 0 / 0 | 0 / 0 | Sem create nesta fronteira |
| Documentos legais | 13 / 13 | Somente leitura | 7 / 5 | 2 | 0 / 0 / 0 | 0 / 0 | Sem create nesta fronteira |

As colunas de resposta incluem projeções adicionais; o percentual do auditor usa os campos de negócio contabilizados. As decisões não aplicáveis têm escopo limitado à jornada atual e não constituem proibição universal. O inventário atribuído é mantido pela revisão independente.

## Provas atuais e limites

- Auditor: 16 entidades, seis bindings resolvidos, seis linhas estruturais PASS, zero problemas editoriais. Veredito global ainda FAILED pela decisão de adoção de Cargo pendente. [Saída completa](t08-static-audit.md), [114 regressões](t08-auditor-regression.json).
- SDK: candidato privado `8.0.0-bulk.308-consolidation.5-SNAPSHOT`, commit `e53a1353`, consumido com override e hash verificado. [Proveniência](t08-sdk-provenance.json), [60 testes focais do handler de constraint](t08-sdk-constraint-tests.json). O POM rc.179 e a publicação Maven não estão certificados.
- Integral reconciliada: rotas 2/2, lote 6/6, contrato 5/5 e apresentação 4/4 aprovados. Cargo Neon **13/14**, com 503 no GET de ações do usuário somente de leitura. Regressão geral 63/63, pilares 6/6, completude nativa 4/4 e contexto 9/9 aprovados. Total integral: **112/113**, sem erros ou skips. [Recibo integral](t08-final-test-results.json). [Recibo de Cargo](t08-cargo-integrated-results.json).
- A revisão identificou asserções negativas permissivas para campo ausente; o reforço dos três cenários foi aplicado depois de arquivar a integral. A focal única terminou **2/3**, com novo 503 em capabilities da origem extinta; READ-only e contexto estrangeiro passaram. [Recibo focal](t08-negative-contract-focal-results.json). A integral vermelha foi preservada.
- Preservação final confirmada para schema e dados das 16 tabelas; branch descartável excluída. [Preservação](t08-neon-preservation.json), [limpeza](t08-neon-cleanup.json). Sequências e conteúdo do ledger técnico não foram comparados.
- Consumo HTTP complementar não executado após o bloqueio; preparação e 676 inputs verificados não equivalem a consumo. Browser desktop/narrow, route/overlay e UX permanecem não executados. Não avançar a T09 por este pacote.
- [Auditor runtime](t08-runtime-audit.md): FAILED por adoção pendente, HTTP/availability falhos, Angular não executado e determinações com prova consumidora incompleta; sem fonte stale ou hash divergente. [Decisão independente](t08-adoption-review.md).
- Config/rules-engine permanece fase futura por decisão explícita do usuário. Identidade federada, entitlement individual por órgão e SLO corporativo não são certificados pelo showcase.

[Execução, matriz A01–A20 e limitações](../T08-EXECUCAO.md) · [Contrato de Cargo](../DUPLICATE-DRAFT-CARGO.md).

## Histórico preservado

A campanha anterior de investigação do 503 teve [80/80 testes](503-final-test-results.json), [consumo HTTP aprovado](503-angular-http-results.json) e [preservação do Neon](503-neon-preservation.json). Ela usa outro fonte e SDK e não certifica T08. A campanha isolada T08 teve [95/97](t08-pre-context-full-results.json); os achados de contexto e fixtures foram corrigidos antes da integração. O timeout na preparação integrada está em [recibo próprio](t08-integrated-focal-publication-timeout.json). Nenhum desses resultados é apagado por uma execução posterior.
