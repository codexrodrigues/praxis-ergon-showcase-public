# Correções da revisão 2013442

Classificação: transversal, arquitetural e contrato público. Preservar schema Neon, `ddl-auto: validate`, Flyway desabilitado, consolidação preexistente do SDK e proibição de consultar o quickstart.

1. Host: aplicar escopo obrigatório antes da seleção QUERY; proteger aglutinação inclusive entre transações; separar administração de lote de escrita cadastral.
2. Metadata: contrato mínimo reutilizável de presença em DTO parcial (omitido preserva; null explícito é deliberado), sem alterar o formato JSON; recusa tipada de discovery indisponível, sem expor causas internas e sem ampliar prazos.
3. Host: aplicar presença nos três PATCHs e publicar nullabilidade real; diagnósticos públicos acionáveis; provas negativas, corrida de aglutinação e mutações de lote de cargos/pessoas.
4. Consumidor: executar avaliação, confirmação, recibos, cancelamento e retomada pelo cliente Angular oficial em harness com HTTP real. Não criar semântica de lote paralela nem alterar libs apenas para facilitar a prova.
5. Prova e derivados: novo candidato Maven privado; suites focais do SDK, bateria Neon em cópia descartável, auditor, campanha, relatório, scorecard, README e PR. Não declarar aceite visual ou produção corporativa.

Mapa de impacto: Metadata é dono da presença de PATCH e erro HTTP; host é dono do escopo/invariantes/autorização; Angular consome os contratos existentes. Docs do SDK, receita do host e evidências precisam acompanhar. Não há novo shape x-ui ou necessidade inicial de mudar landing/corpus público. Há mudança deliberada de comportamento de null no PATCH e de autoridade de publicação; documentar a migração beta. O POM público não será considerado validado sem candidato com as correções.

Segurança corporativa: separar a administração nesta rodada. Federação de identidade, política de acesso por órgão e aceitação visual completa permanecem fronteiras de adoção que exigem definição própria, não regras inventadas nesta correção.

Inventário Angular: `cancelOperation` já integra o contrato, mas falta o método de transporte no cliente básico (suportado parcialmente). Completar `requestCancellation` no core, sem criar shape ou rota; preservar a resposta terminal do servidor. Impacto: consumidores do serviço e harness; README do core; spec focal, build core e prova HTTP do consumidor. Mudança aditiva, sem quebra de assinatura, sem novo manifesto de componente ou editor visual.
