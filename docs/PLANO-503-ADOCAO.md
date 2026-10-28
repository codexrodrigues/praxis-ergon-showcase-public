# Investigação do 503 e decisões de duplicate-draft

Rodada de 28/10/2026, continuação de `ac6dd29`. Classe: transversal.

## Plano e impacto

1. Reproduzir a recusa de publicação com o mesmo JAR privado, em cópia Neon,
   capturando a causa interna por JFR. Distinguir falha de disponibilidade de
   defeito lógico antes de alterar a implementação.
2. Corrigir a causa demonstrada no Metadata Starter e acrescentar regressão focal;
   não aumentar deadlines, relaxar ACLs, repetir escritas ou servir fotografia antiga.
3. Avaliar os seis recursos pelo contrato canônico de duplicate-draft e pelo domínio.
   Registrar decisões atribuídas; não converter dependência de implementação em N/A.
4. Executar a campanha final quando houver candidato corrigido e atualizar recibos,
   relatório e scorecard, mantendo qualquer gate não demonstrado como pendente.

Dono canônico do guardião: `praxis-metadata-starter`, worktree limpa
`bulk-runtime-consolidation`. Consumidor: Ergon Showcase; contrato HTTP também
consumido pelo cliente Angular do PR #661, cuja biblioteca permanece em outra frente.
Inventário e auditor: `scripts/workspace/`; decisões não dispensam outros gates.
Docs afetadas: especificação do guardião se houver alteração, este plano, relatório,
scorecard e evidências do host. Não há mudança de formato público planejada, release,
DDL de domínio ou razão para alterar landing/corpus HTTP nesta etapa diagnóstica.

O contrato novo de duplicate-draft está em trabalho não commitado no checkout
canônico do Starter e não faz parte do JAR privado testado. A avaliação de adoção
não autoriza copiar esse trabalho incompleto, criar alias local ou certificar sua
integração. Uma adoção aplicável permanece pendente até consumir o contrato canônico
integrado e demonstrar autorização, allowlist, não persistência e criação final.

## Limpeza autorizada

Em 28/10/2026 o mantenedor determinou: "Sim, sempre exclua as branchs apos uso para
evitar lixo. Registre esta decisão". Branches descartáveis criadas para provas serão
excluídas após salvar evidências. A autorização inclui `br-spring-butterfly-b79ht1rc`
e futuras cópias de prova; não abrange bases originais, compartilhadas ou de terceiros.

## Achado adicional de composição

As duas regressões focais reproduziram a ausência de um fence único na validação
de identidade através de múltiplos grupos OpenAPI. A correção pertence a
`OpenApiCanonicalOperationResolver`: reutilizar `withSchemaCacheReadLock` existente
durante a composição inteira, preservando os guards LIVE de entrada/saída.
Não cria API, cache de autoridade ou lock ao redor de escrita de domínio. Impacta
consumidores de resolução estrita e exige regressões de bindings/publicação e
campanha do host no candidato resultante. O achado não atribui retrospectivamente
a causa exata ao 503 da campanha anterior.
