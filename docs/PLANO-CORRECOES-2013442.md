# Correções da revisão 2013442

## Consolidação operacional de 28/10/2026

Integrar o SDK em worktree limpa a partir dos commits canônicos, preservando as
mudanças de terceiros no checkout principal. Medir a atestação LIVE no Neon antes
de corrigir seu transporte; preparar expectativas imutáveis fora da transação e
agrupar ACLs sem retirar fatos de segurança ou ampliar deadlines. Validar testes
focais e negativos de grants, construir candidato privado rastreável e executar
a bateria serial do host e o harness HTTP com o cliente Angular da PR #661.
Regenerar evidências apenas a partir dos resultados efetivos; manter os gates
pendentes se a prova falhar. Excluir a branch descartável após preservar as provas.

Impacto: implementação canônica no Metadata; host consumidor, relatório, scorecard
e recibo de campanha. Sem novo contrato público, alteração de schema Neon ou
mudança em bibliotecas Angular nesta frente; sem derivados de UI a regenerar.

Classificação: transversal, arquitetural e contrato público. Preservar schema Neon, `ddl-auto: validate`, Flyway desabilitado, consolidação preexistente do SDK e proibição de consultar o quickstart.

1. Host: aplicar escopo obrigatório antes da seleção QUERY; proteger aglutinação inclusive entre transações; separar administração de lote de escrita cadastral.
2. Metadata: contrato mínimo reutilizável de presença em DTO parcial (omitido preserva; null explícito é deliberado), sem alterar o formato JSON; recusa tipada de discovery indisponível, sem expor causas internas e sem ampliar prazos.
3. Host: aplicar presença nos três PATCHs e publicar nullabilidade real; diagnósticos públicos acionáveis; provas negativas, corrida de aglutinação e mutações de lote de cargos/pessoas.
4. Consumidor: executar avaliação, confirmação, recibos, cancelamento e retomada pelo cliente Angular oficial em harness com HTTP real. Não criar semântica de lote paralela nem alterar libs apenas para facilitar a prova.
5. Prova e derivados: novo candidato Maven privado; suites focais do SDK, bateria Neon em cópia descartável, auditor, campanha, relatório, scorecard, README e PR. Não declarar aceite visual ou produção corporativa.

Mapa de impacto: Metadata é dono da presença de PATCH e erro HTTP; host é dono do escopo/invariantes/autorização; Angular consome os contratos existentes. Docs do SDK, receita do host e evidências precisam acompanhar. Não há novo shape x-ui ou necessidade inicial de mudar landing/corpus público. Há mudança deliberada de comportamento de null no PATCH e de autoridade de publicação; documentar a migração beta. O POM público não será considerado validado sem candidato com as correções.

Segurança corporativa: separar a administração nesta rodada. Federação de identidade, política de acesso por órgão e aceitação visual completa permanecem fronteiras de adoção que exigem definição própria, não regras inventadas nesta correção.

Inventário Angular: `cancelOperation` já integra o contrato, mas falta o método de transporte no cliente básico (suportado parcialmente). Completar `requestCancellation` no core, sem criar shape ou rota; preservar a resposta terminal do servidor. Impacto: consumidores do serviço e harness; README do core; spec focal, build core e prova HTTP do consumidor. Mudança aditiva, sem quebra de assinatura, sem novo manifesto de componente ou editor visual.
