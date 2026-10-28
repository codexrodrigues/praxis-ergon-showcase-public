# Consolidação das correções R1–R7

Rodada de 28/10/2026. Escopo: fechar a integração canônica do SDK e as provas
do host, mantendo a biblioteca Angular sob responsabilidade do agente do PR #661.
O plano e o mapa de impacto estão em [PLANO-CORRECOES-2013442.md](PLANO-CORRECOES-2013442.md).

## SDK e origem do candidato

A worktree limpa `bulk-runtime-consolidation` integra a `main` canônica
`0b162f2abca8502d090e3184fe323342ec88aca2` e as correções de `0a2db557a7` no merge
`21f89ae167`. O commit `3822eca0aff4592566e3765171e2463f13efee3f`, enviado ao PR #311,
acrescenta a correção de transporte e preparação descrita abaixo. O checkout
principal do Metadata, com alterações de outra frente, foi preservado.

O candidato é `8.0.0-bulk.308-consolidation.1-SNAPSHOT`, instalado somente no cache
Maven isolado. A única transformação para empacotamento foi a versão do projeto
no POM; o POM original foi restaurado e a worktree voltou a ficar limpa. Commit,
árvore Git, transformação e SHA-256 do JAR estão na
[proveniência](evidence/consolidation-sdk-provenance.json).
Não houve publicação Maven Central, merge ou certificação da rc.179 do POM do host.

## Diagnóstico e correção

As sete consultas de catálogo medidas no Neon executaram no servidor em menos
de 2 ms cada. O cliente apresentou oscilação de transporte, inclusive para uma
consulta pequena, além do custo inicial de carregar expectativas do artefato.
Os 58 corpos de funções instalados já correspondiam ao dicionário de hashes do SDK;
não havia evidência de falha dessa compressão.

O volume restante incluía centenas de repetições de tabela/papel nas ACLs. A
captura LIVE agora agrupa permissões por tabela, papel, grant option e propriedade
e reconstrói os fatos individuais antes de validá-los. Permissões de coluna,
duplicatas e inventários inesperados continuam presentes. As expectativas
imutáveis são preparadas na construção da infraestrutura, sem acesso ao banco,
fora das transações operacionais.

A soma aproximada dos valores de colunas convertidos para texto caiu de 104 KB
para 64 KB (cerca de 39%); essa métrica não representa bytes exatos na rede.
As seis sondagens de lifecycle posteriores passaram. Isso não constitui um SLO
de latência nem substitui a campanha funcional. Deadlines, grants esperados,
revalidação LIVE e comportamento de recusa permanecem inalterados.

## Provas do SDK e isolamento Angular

- Baseline integrado: 130 testes aprovados, antes da correção de transporte.
- Correção e fronteiras transacionais: 49 testes aprovados no código final.
- Neon: paridade dos fatos de ACL agrupados/originais, rejeição de grant excessivo
  após aquecimento e restauração dos grants originais, em um teste aprovado.
- Auditor canônico: 46 testes de regressão aprovados, sem alteração no auditor.

As contagens são de execuções distintas e não devem ser somadas como casos únicos.
Detalhes em [consolidation-sdk-tests.json](evidence/consolidation-sdk-tests.json).

O cliente Angular está no commit `421012f90`, PR #661, com worktree limpa.
O checkout principal mantém somente as duas alterações preexistentes de
`praxis-page-builder`. Esta frente não alterou suas bibliotecas. O harness foi
recompilado apontando para a worktree isolada do cliente oficial; sua execução
HTTP pertence à prova do host e será registrada separadamente da aceitação visual.

## Campanha do host e gates finais

A bateria integral executou 74 testes: 73 aprovados, uma falha, nenhum erro ou
ignorado. Vínculos completou todas as modalidades, comandos, QUERY/exclusão e
recibos. A confirmação de lote de pessoas recebeu 503 de publicação governada.
Não houve repetição focal nem promoção da união de rodadas a uma campanha verde.

O primeiro harness concluiu lote e submit parcial, mas faltaram os arquivos de
determinações no diretório isolado. As fixtures foram removidas. Com os insumos
atuais gerados pelo Java, uma nova execução completa passou: seis schemas, seis
execuções reativas e seis contratos de lote, além de CLEAR, idempotência, retomada,
cancelamento terminal, conflito concorrente e submit parcial. A falha de preparação
não foi tratada como defeito Angular nem como motivo para repetir a escrita 503.

| Gate | Estado e evidência |
|---|---|
| Integração canônica SDK | Concluída na branch do PR #311, com main integrada e testes focais/Neon verdes; sem merge na main ou release |
| Campanha, recibos e auditor | Pendente: 73/74; hashes atuais, sem evidência obsoleta; HTTP/Bulk pendentes |
| Isolamento Angular | Concluído: PR #661 separado; worktree limpa; duas alterações preexistentes de Page Builder preservadas |
| Higiene host/Neon | `.local/` ignorado; servidor auxiliar encerrado; banco descartável excluído após registrar provas |

O auditor canônico mudou durante a execução: passou a exigir inventário fechado
e decisão atribuída de adoção de duplicate-draft. As 46 regressões atuais passaram.
Seis recursos estão com decisão pendente no inventário governado, além dos dois
gates operacionais. A cobertura dos campos permanece em 100%, mas o veredito é
FAILED, com oito pendências. Não é uma obrigação de implementar cópia de cadastros.
Esta frente não alterou o inventário de outra tarefa para produzir PASS.
Foram executados o comando padrão solicitado e a variante com `--metadata-sources`
apontando para o sources.jar do candidato efetivamente consumido.

A consulta administrativa via conector confirmou PostgreSQL 17 em aws-us-east-1,
compute ativo/fixo de 1 CU, suspensão automática desligada e nenhum reinício
na campanha. O snapshot mostrou 8/450 conexões, zero locks em espera e zero
deadlocks. Não inclui histórico de CPU/memória. Computer Use não conseguiu
verificar a URL do Chrome e bloqueou a leitura do painel; nenhuma configuração
foi alterada. Esses dados não atribuem a causa específica do 503 nem justificam
ampliar compute ou deadlines. A captura privada de exceções registrou I/O em
conexões ociosas do pool, sem timeout/publicação indisponível no harness observado.

Antes da exclusão autorizada de `br-winter-bird-b7sa7euc`, foram comparadas as
392 colunas das 16 tabelas e os hashes dos dados de domínio: idênticos à original.
A listagem posterior confirmou `br-super-sound-b7urizmd` preservada. Recibos:
[resultado integral](evidence/consolidation-full-test-results.json),
[campanha](evidence/PILLARS-CAMPAIGN.json),
[administração Neon](evidence/consolidation-neon-admin.json),
[preservação](evidence/consolidation-neon-schema-comparison.json) e
[exclusão](evidence/consolidation-neon-deletion.json).

Permanecem abertos: causa/prova integral do 503 e as decisões independentes de
adoção introduzidas pelo novo auditor. O candidato não é publicação Maven/npm,
aceitação visual ou aprovação de prontidão corporativa. Não houve merge/release.
