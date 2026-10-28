# Correções da revisão 2013442

Rodada de 28/10/2026. A classificação e o mapa de impacto estão no [plano](PLANO-CORRECOES-2013442.md).
Este documento complementa a revisão histórica; não a reescreve como se os defeitos nunca tivessem existido.

**Estado vigente:** a [consolidação](CONSOLIDACAO-2013442.md) executou 74 testes com uma falha 503 no lote de pessoas. O ciclo completo de vínculos passou. As contagens e o candidato da seção histórica abaixo não são o resultado atual.

| Achado | Correção | Prova correspondente |
|---|---|---|
| R1 | A seleção QUERY combina o filtro canônico com o órgão obrigatório antes de limitar IDs. Pessoas continuam globais. | `reviewBulkSelectionAppliesOrganizationBeforeLimit` passou no Neon; o ciclo HTTP completo de vínculos, incluindo QUERY/exclusão, passou na consolidação (`consolidation-full-test-results.json`) |
| R2 | Pai com dependentes não pode perder a função aglutinadora; admissão do filho mantém lock compartilhado do pai, enquanto a alteração do pai usa lock exclusivo. | `reviewAggregatorCannotBeDisabledWithDependentCargo`; `reviewAggregatorSerializesConcurrentChildAdmissionAndDisable` |
| R3 | SDK recusa perda da prova final com 503 tipado e sanitizado. Preparação OpenAPI e timeout transacional recebem classificação operacional, sem ampliar prazos nem autorizar replay de escrita. | `BulkDiscoveryProjectionFailureTest`; `OpenApiPublicationPreparationTest`; discovery concorrente Neon |
| R4 | `PartialUpdateInput` registra presença pelos setters tipados. Omitido preserva; null limpa somente colunas anuláveis. Booleanos obrigatórios rejeitam null. | `PartialUpdateInputTest`; `reviewPartialUpdatesDistinguishOmissionClearAndRequiredBooleans` |
| R5 | Cliente oficial Angular com HttpClient e discovery reais: avaliação, confirmação, recibos, idempotência, retomada, recusa por versão, CLEAR por item e cancelamento terminal. Java cobre mutações nos três recursos. | harness `angular-consumer-proof.ts`; `cargosAndPessoasExecuteBothUpdateModesAndClear` |
| R6 | Receita usa candidato/cache explícitos; branch descartável obrigatória; testes conferem endpoint JDBC informado no provisionamento. | README, `.env.example`, guarda de destino e comparação de schema |
| R7 | Códigos de conflito, permissão e validação têm orientações distintas; `JORNADA_FORA_INTERVALO` publica campo e limites sem copiar exceções internas. | `blockedProposalExplainsStaleVersionAndInvalidItemWithoutMutation` |

Administração de publicação agora requer `ERGON_BULK_ADMIN`. Identidades de manutenção e administração são distintas e não herdam as permissões uma da outra. O provedor passa a `ergon-bulk/2`; uma implantação precisa suspender/republicar explicitamente sua geração e gerar novas propostas. Não foi criada convivência de versões.

O revisor semântico nativo foi corrigido no Metadata: estado marcado simultaneamente com `@JsonIgnore` e `@Schema(hidden=true)` não é coluna de negócio. Ocultar somente o schema continua sujeito ao gate. O auditor Python não precisou de mudança de semântica nesta rodada; suas 45 regressões foram executadas novamente.

## Limites da prova histórica

- O candidato Maven é privado: `8.0.0-bulk.308-review-SNAPSHOT`; a rc.179 do POM não é certificada por estas provas.
- O banco de domínio permanece com `ddl-auto=validate` e Flyway desabilitado. A comparação dos atributos das 392 colunas de 16 tabelas entre original e cópia está em `evidence/review-neon-schema-comparison.json`.
- A prova Angular é um harness HTTP das fontes oficiais. Não é aprovação visual desktop/narrow, nem prova de instalação do pacote npm público. Não foi criada tela paralela.
- Cancelar execução já terminal preserva seu resultado. Não se afirma reversão de itens confirmados nem suporte ASYNC no cliente básico.
- Basic Auth do showcase não é identidade corporativa federada. X-Tenant seleciona o órgão operacional; não comprova autorização organizacional. Federação, política por órgão e a fase Config/rules-engine continuam fora deste aceite.

## Fricções encontradas

As tentativas iniciais falharam antes das novas provas de domínio: geração fria de OpenAPI e atestação transacional excederam os prazos existentes. A opção de JVM `-XX:TieredStopAtLevel=1` foi usada para reduzir custo de inicialização na campanha seguinte; não altera prazos, grants, estado READY ou regras de domínio. Esses resultados devem permanecer registrados, mesmo quando uma rodada posterior passar.

A classificação de indisponibilidade cobre também preparação administrativa e timeout antes da projeção. Ela exige verificar estado durável e geração antes de uma nova tentativa explícita; não transforma timeout em sucesso.

## Resultado histórico da rodada anterior em 5fd8483

- Execução integral: 74 testes, 4 falhas, 0 erros, 0 ignorados. Três indisponibilidades 503 e uma falha do teste de identidade administrativa: o Basic Auth padrão do MockMvc substituía o usuário mockado. A prova foi corrigida para autenticar o administrador real por HTTP.
- Repetição focal: 5 testes, 3 aprovados e 2 falhas. Segregação administrativa, schemas e analytics passaram. Publicação recebeu `TRANSACTION_TIMEOUT` e impediu iniciar o ciclo positivo de vínculos. Em conjunto há prova positiva de 73/74 casos distintos, sem uma execução integral Maven verde.
- SDK: 130 testes focais aprovados. Auditor: 45 regressões e STATIC PASS nos seis recursos. O gate operacional permanece pendente e o auditor com `--require-runtime-evidence` falha deliberadamente enquanto essa lacuna persistir.
- Harness HTTP oficial: aprovado, incluindo recuperação/idempotência, cancelamento terminal, CLEAR, resultado parcial `CONFIRMED`/`CONFLICT` com edição concorrente preservada, submit parcial com null explícito e seis execuções reativas. Fixtures removidas. A montagem foi corrigida para usar uma única identidade Angular/HttpClient conforme o lockfile da prova.
- A responsabilidade pela biblioteca Angular foi transferida, por solicitação do usuário, a outro agente: PR #661 isolado, 17 specs, builds Core/Table e pureza conferidos por ele, sem novas alterações. O checkout principal teve somente nossa cópia de três arquivos removida; Page Builder preexistente preservado.

**Não pronto para merge:** o tratamento seguro de erro encerra o vazamento de 500 observado, mas não resolve a margem de disponibilidade da publicação LIVE. Não houve aumento de prazos, retry automático de escritas, alteração de grants ou cache positivo de autorização. É necessário fechar essa disponibilidade e repetir a campanha integral antes do aceite operacional.

Após registrar os recibos e conferir zero fixtures, a branch descartável `br-dry-breeze-b7pc58x5` foi excluída com a autorização explícita do usuário. A listagem posterior confirmou a original `br-super-sound-b7urizmd` preservada. O servidor auxiliar foi encerrado. Ver [campanha](evidence/PILLARS-CAMPAIGN.json), [scorecard](evidence/SCORECARD-COMPLETUDE.md) e [limpeza](evidence/review-neon-deletion.json).
