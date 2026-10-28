# Bulking governado — Issues host #2 / Metadata #308

Classificação: contrato-publico e arquitetural. Branch: feat/ergon-showcase-scaffold-pilots. Sem merge, release ou alteração das 16 tabelas/392 colunas de domínio.

## Aceite anterior

O responsável aprovou explicitamente a Fase 3A de e6b9fa0: determinações HOST, invariantes transacionais e consumo pelo runtime Angular. Não existe snapshot externo do Config nesta etapa. A integração Config/rules-engine pertence a uma fase futura dedicada; o gate anterior deixa de ser pendência desta fase por decisão explícita de escopo, não por inferência do executor.

## Mapa de impacto

- Metadata starter: dono de BulkEditable, BulkResourceOperations, BulkOperation, protocolo, composição, readiness, codecs e kernel durável. Inspecionar a dependência rc.179 e não inferir execução pela presença de annotations.
- Host: três recursos mutáveis (vínculos, pessoas, cargos), DTOs efetivos de PUT, candidatos completos, autorização, escopo de órgão, invariantes, parâmetros de atos e integração transacional com recibos.
- Neon: somente infraestrutura técnica oficial praxis_bulk do migrator, caso necessária, com identidades runtime/control-plane separadas e instalação explícita. ddl-auto=validate e flyway.enabled=false preservados. Toda fixture tem rollback ou limpeza determinística.
- Consumidor Angular: ler contrato real de writableFields/clearableFields e referências de avaliação/confirmação usando o runtime oficial; preservar prova HOST existente.
- Auditor: fonte canônica em scripts/workspace da raiz, colunas BulkEditable e BulkOps, testes negativos e transporte reproduzível no PR.
- Derivados: README, relatório, matriz de requisitos, scorecard, schemas e recibos. Sem alteração de contrato nas libs Angular, não há release/site/corpus público a sincronizar por reflexo.
- Risco: novos endpoints públicos; updateSource precisa preservar operationIds das determinações já publicadas ou migrá-los deliberadamente no mesmo ciclo; campos de workflow/identidade continuam indisponíveis ao SET/CLEAR.

## Fases e gates

1. Contrato e curadoria: matriz de colunas reais, allowlists explícitas por modalidade, fonte PUT, limites/CLEAR/proteções, rótulos executivos dos facts e colunas principais visíveis nas duas consultas auxiliares. Gate: compilação, schema real e testes do auditor.
2. Protocolo durável e domínio: evaluation sem efeitos, proposta/preview paginado, confirmação idempotente e resultados por item; UNIFORM_UPDATE/QUERY, PER_ITEM_UPDATE e dois DOMAIN_COMMAND de vínculo; controle de versões, escopo, autorização, cancelamento e replay. Gate: PostgreSQL real, efeitos+recibos na mesma transação, casos mistos e negativos, limpeza de fixtures.
3. Consumo e fechamento: discovery/capabilities, normalização Angular, suite integral Neon, smoke do JAR, auditor/scorecard, relatório e atualização do PR #1. Nenhuma aprovação arquitetural independente é presumida.

## Reconciliação inicial da especificação com o contrato real

- controleVaga é o nome real de Cargo; controlaVaga não será criado como alias.
- recebeAdiantamento e cargaHorariaPadrao não existem nas entidades/colunas atuais; não serão simulados em JSONB ou criados por DDL. A investigação ampliada encontrou adiantamentos em férias/parâmetros e jornada padrão no histórico de jornadas do cargo, fora das 16 tabelas. Registrar a lacuna exata do requisito e as fontes em RECONCILIACAO-CAMPOS-BULK-308.md, sem chamar essas capacidades de implementadas.
- Identidade/propriedade de vínculo: id composto numfunc/numero, empCodigo, cargo e dtnom; orgaoId/cargoId/dtadmissao não são novos nomes wire.
- O enum canônico de seleção é EXPLICIT, não EXPLICIT_IDS. QUERY conserva filtro e exclusões e congela o conjunto na avaliação; não consulta novamente na confirmação.
- correio é um indicador booleano NOT NULL, não um código de malote. Aceita SET true/false; CLEAR é recusado porque a coluna não admite null. Não se anuncia uma capacidade de limpeza incompatível com o banco preservado.
- protectedUpdateFields precisa conter somente nomes realmente aceitos no schema PUT; campos ausentes de PUT já não constituem superfície gravável. O compilador canônico rejeita nomes fictícios.
- updateSourceOperationId apontará para o PUT efetivo já publicado, preservando seus bindings de determinações; não haverá uma segunda operação/alias somente para reproduzir a grafia ilustrativa da issue.

## Gap canônico encontrado na prova HTTP

O RequestSchemaReader da rc.179 recusa a composição oneOf/anyOf/not que o próprio CustomOpenApiResolver gera para filtros BETWEEN. A fonte PUT passou na composição; a seleção QUERY falhou ao compilar o FilterDTO completo. Classificação: lacuna real de integração de contratos canônicos. Corrigir a fonte Metadata em checkout isolado, com aceitação estritamente delimitada ao formato de intervalo emitido pelo próprio starter, preservando rejeição de uniões arbitrárias e limites de profundidade/nós. Adicionar provas positivas e negativas do leitor e do consumidor. Não reduzir FilterDTO, retirar predicados ou reescrever o schema no host.

O candidato deve usar coordenada Maven distinta e cache isolado; não substituir os bytes locais da coordenada rc.179 já consumida. A adoção do candidato é prova local, separada de release público. O POM do host continua com a referência anterior até haver versão apropriada para adoção; registrar explicitamente o override usado na campanha.

O tratamento HTTP das recusas tipadas do kernel pertence ao GlobalExceptionHandler canônico: conflito, capacidade, expiração e proposta não executável devem manter o envelope padrão e não virar erro genérico 500. Validar o mapeamento de todos os motivos tipados, sem expor SQL, plano ou identidade de alvo.

Impacto do ajuste de transporte: BulkCatalogPhase (interno ao Metadata), leitores de runtime/control plane e suas provas JDBC. Consolidar os sete inventários frescos em uma única instrução SQL e um único ResultSet, preservando parâmetros de roles, cardinalidade, tipos e validações de identidade/ACL/corpos/triggers. Sem cache entre chamadas, sem DDL e sem aumento de orçamento. Validar falhas de inventário, arrays/bytea, liberação de recursos, consulta efetiva no Neon e suites focais dos consumidores internos. Não há mudança wire externa.

Resultado da experiência de transporte: a consulta única preservou os fatos e passou nos testes, mas não reduziu o tempo observado no Neon (cerca de 2,6–2,8 s para o control plane sem trabalho útil). Experiência retirada, incluindo o ajuste de buffer JDBC. Nenhuma ampliação de timeout foi aplicada.

Próxima hipótese mensurável: reduzir transferência dos 80 KB de prosrc com dicionário imutável de corpos exatos das migrations empacotadas. A consulta calcula SHA-256 fresco de cada corpo no PostgreSQL; somente corpos com digest conhecido podem ser reconstruídos de bytes idênticos locais, antes de todas as validações existentes. Corpo desconhecido viaja integralmente e conserva comparação normalizada. Não é cache de atestação ou de autorização. Provar omissão inválida, digest desconhecido, troca de corpo, alteração de ACL após aquecimento e fallback por diferença de whitespace. Adotar somente se reduzir custo e passar no HTTP real.

Publicação entre reinícios: o digest inclui bytes de swagger-config, além dos documentos OpenAPI. Servers relativo no OpenAPI e springdoc.swagger-ui.oauth2-redirect-url relativo evitam incorporar a porta local aleatória à fotografia. A prova exige reconciliação explícita da mesma fotografia após novo contexto; não normaliza/remova URLs no SDK e não aceita digest divergente. O host segue autenticado por Basic; esta configuração não introduz autenticação OAuth.

Correção da hipótese de origem: a captura efetiva provou que Springdoc absolutiza o redirect relativo com a porta interna. Configurar a propriedade oficial springdoc.swagger-ui.oauth2-redirect-url como URL absoluta da origem anunciada do host (padrão documentado localhost:8080, SERVER_PORT quando configurado; a propriedade oficial pode ser sobrescrita no ambiente). O teste com portas internas aleatórias conserva essa origem anunciada; comparar duas fotografias completas para comprovar estabilidade.

Gap adicional de disponibilidade: requirePublishedSnapshot classifica IllegalStateException do guard, mas um timeout transacional ou DataAccessException do mesmo guard escapava como 500. Corrigir somente essa fronteira conhecida para o subtipo governado 503, preservando a causa privada, a recusa sem saída parcial e o 500 de falhas de callback fora do guard. Não ampliar timeout nem permitir recaptura/retry automático.
