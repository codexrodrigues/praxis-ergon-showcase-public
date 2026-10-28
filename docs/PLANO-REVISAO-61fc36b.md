# Plano de fechamento da revisão 61fc36b

Classificação: contrato-publico e arquitetural. Não haverá merge nesta execução.

Fontes: revisão independente em .local/REVISAO-61fc36b.md; AGENTS.md vigente; comentários das 16 entidades e catálogo PostgreSQL; documentação legada VINCULOS.TAB, CARGOS_ e FUNCIONARIOS; contratos e testes canônicos do metadata starter e do runtime Angular. Nenhum código do quickstart será consultado.

## Impacto

- Dono canônico de schemas, determinações, referências e superfícies: praxis-metadata-starter rc.179, mantido como dependência.
- Dono das consultas, invariantes, projeções e regras de preview: host Ergon.
- Dono da materialização: runtime oficial de praxis-ui-angular; provas devem consumir o contrato publicado, sem fórmulas ou aliases locais de frontend.
- Auditor canônico: scripts/workspace da raiz; transportar patch e testes verificáveis no PR porque a raiz não é um repositório Git.
- Derivados: schemas, relatório, scorecard, matriz de paridade delimitada, evidências HTTP e prova de consumo. Nenhum release de plataforma é necessário.
- Risco de contrato: referências auxiliares de leitura e superfícies novas; restrições de entrada passam a recusar dados incompatíveis antes da persistência. Banco permanece com 16 tabelas/392 colunas, ddl-auto=validate e flyway.enabled=false.

## Fase 1 — Contratos completos e gate estrutural

Curar FKs, valores monetários e datas, controles, ícones, fontes de opções e referências reais. Publicar consultas auxiliares de setores e documentos sobre entidades existentes, sem criar persistência. Evitar repetição de título/subtítulo no corpo Quick View. Ampliar auditor para esses critérios, preservando recursos somente de leitura e separando critérios estruturais de prova operacional. Gate: compilação, auditor e testes negativos do auditor.

## Fase 2 — Consulta, superfícies e integridade

Eliminar cargas por linha no caminho completo de página/cursor/exportação; validar tamanhos dos três PATCHs; centralizar provimento sem bloquear manutenção histórica; fechar nulidade intermediária na cronologia. Expor relações master-detail reais entre os pilotos, com paginação e autorização. Gate: HTTP positivo/negativo, consulta em contexto frio e invariantes contra Neon isolado com rollback.

## Fase 3 — Determinações e aceitação do consumidor

Registrar determinações reativas com operationIds e bindings explícitos, endpoint não persistente e a mesma regra utilizada pelo submit. Documentar aplicabilidade de regras estatutárias e do motor governado, sem inventar aprovação de snapshots ou legislação ausente. Provar schemas por operação, autorização, rejeições e consumo pelo runtime oficial; fechar matriz de paridade do escopo piloto por fonte, campo e capacidade. Gate: contratos materializados, execução HTTP e prova do consumidor, seguida de suite integral e empacotamento.

## Entrega

Atualizar PR #1, relatório, scorecard e evidências com os resultados efetivamente obtidos e limites explícitos. Não converter STATIC PASS ou testes verdes em aprovação arquitetural independente automática.

## Resultado dos gates

Fase 1: implementada; seis recursos STATIC PASS, incluindo duas consultas auxiliares; 29 regressões do auditor reproduzidas após transporte isolado; destinos das referências exercitados por HTTP.

Fase 2: implementada; três PATCHs recusam excesso antes do banco; manutenção histórica aceita em PUT/PATCH; cronologia fecha extremos sem data intermediária; coleções relacionadas paginadas e autorizadas; página/cursor/exportação com quatro consultas para uma ou três linhas em contexto frio.

Fase 3: determinações HOST e bindings CREATE/EDIT implementados; seis schemas e seis execuções do runtime Angular oficial comprovados; matriz de paridade delimitada publicada. A integração governada permanece pendente da identidade de um snapshot aprovado do Config, solicitada ao responsável. Não se presume aprovação nem se cria regra estatutária para encerrar um contador. O manifesto operacional deve falhar nesse gate até resolver o insumo.

Banco: nenhuma entidade ou configuração de DDL alterada; validação contra a branch Neon isolada. Derivados atualizados no host: schemas, recibos, matriz, relatório, scorecard e transporte do auditor. Não há alteração de contrato canônico das bibliotecas ou publicação que exija atualização do site/corpus oficial. Não foram alteradas skills: a dificuldade encontrada é disponibilidade de artefato aprovado e divergência fonte/dist no ambiente, não evidência suficiente para reescrever a orientação canônica.
