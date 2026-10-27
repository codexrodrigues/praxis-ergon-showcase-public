# Correções da revisão independente

Classificação: contrato-publico e arquitetural. Fonte canônica de contratos: praxis-metadata-starter; regras e comandos: host Ergon; gate operacional: scripts/workspace/audit-java-resource-completeness.py na raiz do workspace.

Impacto: DTOs, mappers, comandos, estado/discovery, testes, schemas e documentação dos quatro pilotos. Sem DDL, migrations ou mudanças de entidades. ddl-auto=validate e flyway.enabled=false permanecem. Não há consumidor Angular neste host; não há corpus global a atualizar.

1. Jornada validada no serviço e no DTO especializado; contagem de vagas, saldo e extinção incluem cargo titular e em comissão, sem dupla contagem.
2. dtPgtoAte passa a ser mantido pelo comando de vacância, com coerência temporal.
3. Registrar e encerrar afastamento usando a tabela afastamentos existente, o código de frequência elegível, datas, estado e documento; preservar vínculo entre comando e assentamento, impedir períodos conflitantes e retorno sem afastamento homologado aberto.
4. Ativar/inativar órgão por ações explícitas com estado e ETag; retirar ativa do PUT cadastral.
5. Auditor com limiar de 100%, associações espelhadas contabilizadas à parte, leitura e destino de escrita separados, rastreamento de comandos do próprio recurso e saída explicitamente estática. Testes de regressão do auditor e testes HTTP negativos do host.
6. Executar auditor estrito, testes Neon e package, atualizar scorecard, relatório e PR.

O auditor da raiz não está em repositório Git. O PR guardará o patch verificável da fonte canônica e de seus testes para revisão e transporte, sem introduzir outro executor no host. Não haverá alteração ou publicação de skills nesta correção.

Complemento de impacto: superfícies de consulta dos quatro pilotos, referências entre recursos e enriquecimento de fatos em lote no serviço do host. Mantida a fronteira de mappers sem consultas. A remoção de ativa do PUT é uma restrição intencional do contrato beta; clientes usam as ações de ativação/inativação com If-Match. Datas passam a ter representação ISO-8601 explícita pelo builder Jackson do Boot.
