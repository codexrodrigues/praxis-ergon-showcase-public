# Decisão de adoção — duplicate-draft de Cargo

Data: 2026-10-28. Responsável: Codex / frente Ergon Showcase, por solicitação do
mantenedor, com revisão independente de `review_cargo_adoption` e coordenação
autorizada com o chat `01a17f50-2166-7e72-bc94-3e9b7bf37332`.

**Decisão funcional: adotar. Estado do gate: pendente.** A implementação está
integrada no host compartilhado; o aceite permanece bloqueado por indisponibilidade
recorrente no discovery canônico. Não há decisão de não aplicabilidade, dispensa
de prova ou aprovação operacional. Este documento substitui somente a avaliação
atual de Cargo; as decisões dos outros cinco recursos e o parecer histórico
`ergon-duplicate-draft-review-20261028.md` permanecem intactos.

## Política atribuída

Conforme `T07-decisoes-mantenedor.md`, registrado pelo mantenedor na frente Ergon,
e a semântica de classificação/identidade/atos/vagas de
`D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/cargos_.tab`:

- Origem vigente: `dtExtincao == null`; data futura não é exceção.
- Sugerir somente nome, categoria, subcategoria, tipoCargo, cargoFuncao,
  escolaridade e cbo. Limpar identidade, órgão, atos, vagas, datas, agregação
  e atributos livres.
- As três flags permanecem null no draft; CREATE exige escolha explícita,
  aceitando false. Não enfraquecer CREATE nem modificar defaults de UPDATE.
- Exigir leitura e criação, consulta da origem no órgão corrente e negação
  indistinguível no POST para origem ausente/inacessível. Manter guarda no serviço.
- Preparo não persistente, sem flush incidental, reserva, versão ou ledger;
  POST final revalida as invariantes e não sobrescreve o vencedor de uma corrida.

O acesso demonstrativo global do host não equivale a autorização multi-tenant de
produção. A política de origem não cria uma nova proibição sobre cargo aglutinador.

## Implementação e evidência efetivas

Integração do delta isolado `e0b1a5d6460c274d6e0d03d8e24b84db691fa77a` sobre
baseline `1769b0958bbb3499e02843619415b7c373a95745`, preservando ApiPaths.
Inclui DTO parcial, nulabilidade/controles públicos, validação de contexto HTTP,
detecção JPA de entidade nova e regressões de não escrita/concorrência.

Dependência efetiva: override privado
`8.0.0-bulk.308-consolidation.5-SNAPSHOT`, JAR SHA-256
`11194e06878268a184fbeb476f7b1a4fbd09a287c91badf36cb86e3e7072abd0`.
O POM permanece `8.0.0-rc.179`; esta prova não certifica a dependência pública.

Recibos em `praxis-ergon-showcase/docs/evidence/`:

| Campanha | Resultado | Fonte efetiva |
|---|---|---|
| `t08-final-test-results.json` | 113 testes: 112 aprovados, 1 falha, 0 erros/skips | `891ee0799cd95beeb496e4249fe10a029b503044814784247ee0f295eb183f0d` |
| `t08-negative-contract-focal-results.json` | 3 testes: 2 aprovados, 1 falha, 0 erros/skips | `8f4d93e777f3338ca7af7f4b32f4ba1815cffc4265c9a1170fc4d03fc484f723` |

O segundo hash difere somente por `CargoDuplicateDraftNeonTest`, conforme
`t08-negative-oracle-change.json`: booleano false explícito, HTTP200/shape válido
antes de ausência de link e ausência de estado efetivo da origem inacessível.
Produção, SDK, configuração e limites permaneceram iguais. Não transportar a
campanha integral ao novo hash, somar reexecuções como testes únicos ou declarar
14/14 de Cargo numa campanha que não ocorreu.

Na integral, o GET actions do perfil somente leitura retornou 503 às 22:08:05.
Na focal única, esse caso e a origem inacessível passaram, mas GET capabilities
de origem extinta retornou 503 às 23:34:38.860. Ambos estão correlacionados a
`TransactionTimedOutException` na publicação governada. A iteração de data
passada/futura da segunda falha não é identificável nos recibos disponíveis.
O reforço do teste revelou um 503 que as antigas asserções poderiam aceitar
indevidamente; não há evidência de que a mudança do teste causou a indisponibilidade.

## Bloqueio e critérios de retomada

Responsável pelo diagnóstico: mantenedores do Metadata Starter, fronteira de
publicação/discovery governados, em coordenação com o host Ergon Showcase.

1. Correlacionar orçamento da transação, aquisição de conexão, atestação LIVE,
   restauração/fence e leitura do snapshot no caminho que produziu 503. Preservar
   JFR/logs/recibos; causa física ou defeito específico ainda não demonstrados.
2. Se identificado defeito, corrigir a fonte canônica e adicionar regressão
   dirigida à causa; registrar versão/hash do novo artefato e impacto downstream.
   Não aumentar prazos, dispensar atestação ou preparar estado especial apenas
   para fazer o teste passar.
3. Executar os cenários afetados com os oráculos completos e a preparação normal,
   atribuindo o novo resultado aos bytes efetivos. Não repetir até obter verde,
   aceitar 200 ou 503 no lugar de 200 ou apagar campanhas reprovadas.
4. Submeter a correção/prova a nova revisão antes de marcar implementada.
   Aceite operacional/T08/SLO, publicação e browser continuam gates distintos.

Parecer detalhado: `output/cargo-duplicate-gate-20261028/revisao-codigo.md`.
O hash deste documento atesta integridade documental, não certifica runtime.
