# Relatório de rodada — issue 305

## Estado atual — correções da revisão de 61fc36b

Esta seção prevalece sobre os registros históricos abaixo. Plano e mapa de impacto: [execução por fases](PLANO-REVISAO-61fc36b.md). A entrega amplia contrato público e arquitetura do host, conserva o starter local 8.0.0-rc.179 e não altera nenhuma das 16 entidades, 392 colunas ou configuração de criação de schema. Não houve acesso ao código do quickstart, merge ou publicação de biblioteca.

| Achado | Correção / resultado | Evidência |
|---|---|---|
| R1: consultas cresciam de 7 para 14 | EntityGraph nas consultas de cargos/vínculos e enriquecimento em lote; medição começa antes de carregar entidades em contexto frio | reviewDerivedFactsUseOneBatchQueryRegardlessOfPageSize; [contagem](evidence/cold-query-counts.json): página, cursor e exportação 4/4 para tamanhos 1/3 |
| R2: agência de 11 caracteres gerava 500 | Limites reais de armazenamento nos DTOs cadastrais e nos três PATCHs; agência até 10, excesso recusado antes do flush | phaseTwoPartialFormsRejectStorageOverflowBeforeWriting: HTTP 400; limite válido aceito |
| R3: PUT bloqueava histórico com cargo extinto | Removida validação duplicada incondicional; a invariante central aplica elegibilidade de provimento aos estados ocupantes | phaseTwoHistoricalMaintenanceAllowsExtinctCargo: VACANTE/APOSENTADO aceitos em PUT/PATCH; testes negativos de novo provimento preservados |
| R4: extremos de contrato invertidos com meio nulo | Comparação direta entre início e prorrogação, além das comparações adjacentes | phaseTwoChronologyRejectsReverseEndpointsWithMissingMiddle: criação e atualização 422 |
| R5: FKs, valores e referências incompletos | Curadoria explícita de tipo/controle/ícone, opções nomeadas, comissão/aglutinação e setores; dois recursos auxiliares reais, Quick View sem repetição de título/subtítulo | phaseOneAuxiliaryReferencesAreReadOnlyAndResolvable; schemas materializados; auditor com 29 regressões e seis recursos STATIC PASS |
| R6: determinações, relações e consumidor ausentes | Três definições HOST em CREATE/EDIT, operações somente de avaliação, coleções relacionadas paginadas e matriz de paridade delimitada | quatro testes phaseThree; phaseTwoRelatedCollectionsRespectParentAndPagination; [runtime Angular](evidence/angular-consumer-proof.json); [matriz](MATRIZ-PARIDADE-305.md) |

R6 **não está integralmente encerrado**: o gate de decisões governadas depende da identificação de snapshot aprovado do Config. Foi solicitada identidade e endpoint; não há aprovação presumida, regra estatutária inventada ou dispensa N/A. O [manifesto operacional](evidence/runtime-gates.json) registra awaiting-input e o auditor com --require-runtime-evidence deve retornar falha nesse gate, mesmo com todos os gates estruturais satisfeitos. A próxima revisão independente deve avaliar esse insumo e os limites de paridade antes de aprovar merge.

### Profundidade por recurso

Órgãos agora abrem seu quadro de cargos. Pessoas abrem vínculos funcionais. Cargos abrem ocupantes titulares e em comissão. Todos usam recursos reais, existência do pai, filtro pelo relacionamento, paginação e ERGON_READ. Setores e documentos legais possuem ficha e opções somente de leitura; os outros cadastros auxiliares não ganharam CRUD artificial.

As prévias não persistentes são: PcD pelos cinco indicadores reais, nome/saldo do cargo selecionado e nome do cargo aglutinador elegível. O submit recusa campos de prévia informados pelo cliente e revalida as regras de escrita. Saldo consultado não reserva uma vaga. Nenhuma fórmula foi deslocada ao frontend e nenhum resultado dependente de tenant foi colocado na definição cacheável.

A política de documentos foi explicitada por operação: referência cadastral a norma de emissor externo é permitida quando existente; ato de afastamento exige documento do órgão do vínculo. Isso não certifica a aplicabilidade jurídica de qualquer norma. phaseThreeDocumentScopeIsExplicitPerOperation cobre ambos os casos e a aceitação do ato local.

### Consumo oficial e fricções

O ensaio usa normalizeSchemaFields do Core e ReactiveDeterminationRuntimeService das fontes oficiais do workspace Angular, sem alterações nas bibliotecas, aliases novos ou fórmula local. Foram seis schemas CREATE/EDIT e seis execuções com HTTP real, fonte HOST, saída calculada, proteção contra resposta antiga e erro de avaliação visível. Fixture reservada de cargo aglutinador removida no finally; recibo sem dados pessoais. Os hashes de entrada do consumidor estão em evidence/angular-consumer-input-hashes.json.

Houve divergência entre dist local antigo do Core e o código-fonte atual do Dynamic Form: o dist não exportava normalizeScalarArrayValue. A prova passou a usar o tsconfig oficial do workspace, que resolve as fontes canônicas. Isso comprova consumo de fonte oficial; não comprova pacote npm publicado. O registry Azure local também não forneceu as dependências mínimas; o ensaio privado usou versões pinadas do lockfile Angular e registry público, sem mudar configuração global. Setores/documentos já existentes tiveram de ser publicados como consultas reais para que referências fossem resolvíveis. Não foi necessária mudança nas skills; a orientação de preservar proveniência e distinguir fonte/publicação permanece adequada.

Não houve ensaio visual desktop/narrow: o escopo implementou backend e um harness de contrato/runtime, sem construir tela host Angular. A prova não certifica apresentação visual das fichas/coleções. Não foi executada carga concorrente para a última vaga, nem certificação jurídica de todas as regras estatutárias. A paridade está delimitada na matriz por campo/capacidade; contagem de colunas não equivale ao Ergon inteiro.

### Verificação e preservação

Resultados definitivos e hashes estão nos recibos de evidence. A suíte integral usa Neon real isolado, com rollback; o empacotamento executa os mesmos testes. O transporte do auditor foi aplicado em diretório temporário e passou nos 29 testes. O scorecard distingue estrutura, operação, paridade delimitada e pendências. ddl-auto continua validate; flyway.enabled continua false. Não há migrations DDL, alteração de entidades ou escrita na branch principal do Neon.

Execução definitiva em 27/10/2026: **mvn package aprovado, 52 testes, zero falhas/erros/ignorados, 211,8 segundos na classe de integração**. Smoke do JAR final: health/OpenAPI/consulta de vínculo HTTP 200; seis execuções Angular aprovadas e fixture removida. Processo Java auxiliar encerrado. A conferência posterior por SQL retornou 16 tabelas, 392 colunas e zero fixtures de órgãos, cargos, pessoas, vínculos ou afastamentos; órgão base ativo e vínculo base ATIVO. Recibos: [testes](evidence/neon-integration-tests.txt), [smoke](evidence/packaged-phases-smoke.json), [preservação](evidence/neon-preservation-61fc36b.json) e [auditoria operacional](evidence/operational-audit.md).

Derivados atualizados: schemas por operação, recibos, plano, matriz, README, relatório, scorecard e patch/hashes do auditor canônico. Não houve alteração no starter ou nas bibliotecas Angular que exija release ou sincronização do site/corpus público. PR #1 atualizado para nova revisão; nenhuma alegação de autoaprovação arquitetural.

## Histórico — correções anteriores ao commit 61fc36b

Esta seção substitui o estado da entrega anterior, preservada abaixo como histórico. Classificação e mapa de impacto: [plano de correções](PLANO-CORRECOES-REVIEW-305.md). Mantidos o starter local rc.179, as 16 entidades e as 392 colunas, sem DDL, migrations ou alterações nos mapeamentos JPA.

| Lacuna da revisão | Correção implementada | Evidência focal em NeonIntegrationTest |
|---|---|---|
| Jornada inválida no PATCH de lotação | Positive/Max(168) no DTO e faixa 1..168 na invariante transacional, também para chamada direta do serviço | reviewPatchRejectsBothInvalidJornadaBounds; reviewServiceAlsoRejectsInvalidJornada |
| Cargo em comissão não ocupava vaga nem impedia extinção | Ocupação inclui cargo titular OU cargoComissao, uma vez por vínculo; aplica extinção e saldo a ambos, mantendo ATIVO/AFASTADO/CEDIDO como ocupantes | reviewCommissionOccupancyPreventsExtinctionAndDoubleCounting; reviewCommissionCannotUseExtinctOrFullCargo; reviewCommissionCannotExceedVacancies |
| dtPgtoAte sem destino de escrita | Parâmetro opcional da vacância; data final de pagamento não pode preceder a saída | reviewVacancyMaintainsPaymentLimit; reviewVacancyRejectsPaymentBeforeDeparture |
| Estado AFASTADO sem entrada/retorno | registrar-afastamento e encerrar-afastamento usam afastamentos e codigos_frequencia existentes, documento do mesmo órgão, homologação, cronologia, sobreposição e vínculo proprietário; retornam nova versão e afastamentoAbertoId | reviewAfastamentoRoundTripPersistsLicenseAndVersion; reviewAfastamentoRejectsInvalidCodeAndChronology; reviewAfastamentoRejectsEndBeforeStart; reviewRetornoRejectsForeignLicense; reviewAfastamentoRejectsOverlappingPeriodAndEarlyReturn |
| Órgão sem capacidades semânticas | ativar-orgao/inativar-orgao, estados ATIVO/INATIVO, If-Match obrigatório; ativa sai do PUT e permanece na criação/leitura. Nova admissão exige órgão ativo | reviewInstitutionalActionsProtectStateAndETag; reviewOrgaoStateIsNotWritableByCadastralPut; reviewInactiveOrgaoRejectsNewAdmission |
| Quick View e referências incompletas | Identidades com subtítulo e metadados; 5/5/5/6 campos executivos; título com referência própria; referências entre pilotos apontam a fichas realmente publicadas. Rótulos curtos e controles de data explícitos | reviewReadSurfacesPublishRealETagAndReferences; schemas capturados em evidence |
| Consulta por linha no mapper | ErgonReadFacts enriquece coleções em lote, incluindo consulta, cursor, lookup por IDs, exportação e respostas de comandos; mappers não executam essas consultas | reviewDerivedFactsUseOneBatchQueryRegardlessOfPageSize: uma consulta para uma ou três linhas, por categoria de fato |
| Auditor aceitava 85%, associações contadas incorretamente e campos apenas de leitura | Gate de 100%, destino de escrita obrigatório por mapper ou comando alcançável do próprio recurso, joins somente leitura tratados como espelhos e referências resolvidas. Recurso somente de leitura não é obrigado a publicar mutações | [18 testes do auditor](evidence/auditor-tests.txt), [scorecard](evidence/SCORECARD-COMPLETUDE.md), [patch canônico reproduzível](tooling/README.md) |

O ciclo de afastamento registra concessão administrativa já homologada por autoridade ERGON_WRITE, com documento legal, e retorno. A data de término é o último dia afastado, inclusive, e qtdDias inclui início e fim. Não implementa instrução de processo, perícia, homologação pendente ou todos os limites estatutários por espécie de licença. O schema legado existente e sua documentação foram consultados; nenhuma coluna adicional foi simulada em JSONB.

A auditoria estática contabiliza **12/12** atributos de órgão, **17/17** de cargo, **68/68** de pessoa e **59/59** de vínculo, em leitura e destino de escrita. IDs, versão, auditoria técnica e associações espelhadas somente leitura não compõem o denominador; uma relação gravável não é descartada. Os totais não equivalem às 392 colunas das 16 tabelas nem à prova de paridade funcional de todas as telas legadas.

O auditor da raiz foi corrigido no local canônico. Como essa raiz não é um repositório Git, o PR transporta base textual, patch, hashes e testes reproduzidos em diretório temporário; não instala uma cópia executável concorrente no host. As verificações de Quick View adicionadas à versão vigente do workspace foram preservadas e estendidas. Ele retorna **STATIC PASS**, sem substituir revisão independente, testes ou certificação jurídica.

Fricção nova: o ObjectMapper de fallback do starter usa a serialização padrão de datas como arrays quando não há bean do host. O round-trip de dtPgtoAte revelou a divergência. JsonConfig usa o Jackson2ObjectMapperBuilder oficial do Boot, conserva CompositeIdJacksonModule e aplica write-dates-as-timestamps=false. O teste valida ISO-8601 e a identidade composta; não há serializer ad hoc ou troca de versão do starter. A evolução do fallback canônico deve ser tratada em seu módulo proprietário; não foi misturada à dependência local desta prova.

### Validação desta rodada

`mvn clean test` e `mvn clean package` passaram com **42 testes de integração, zero falhas, zero erros e zero ignorados**. O empacotamento final recompilou a versão definitiva dos DTOs e repetiu os 42 testes. Os **18 testes Python** do auditor passaram também após aplicar o patch em diretório temporário vazio. O auditor estrito retornou STATIC PASS nos quatro pilotos.

A consulta pós-suíte no Neon isolado confirmou **392 colunas**, zero fixtures de órgãos/cargos/pessoas/vínculos/afastamentos, órgão 1 ativo e vínculo 1001:1 ATIVO. As mutações dos testes têm rollback; a branch principal do banco não foi usada como destino de escrita. As entidades JPA não foram alteradas nesta rodada.

O JAR final respondeu HTTP 200 para health, OpenAPI, vínculo autenticado, catálogo das ações de afastamento e as três novas fichas de consulta; ETag e data ISO-8601 presentes. O processo foi encerrado. Evidências atuais: resumo Surefire de 42 testes, oito schemas, hashes do starter/JAR, smoke do pacote, conferência de rollback, scorecard e testes do auditor em evidence. Logs completos ficam somente em .local, ignorada pelo Git.

O scorecard comprova estrutura estática; a matriz acima relaciona as verificações de comportamento. A rodada anterior de 25 testes permanece abaixo somente para rastreabilidade histórica.

Limites de aceitação: sem prova visual Angular, ensaio de carga concorrente, Reactive Determinations ou integração governada com motor de regras. Setores/documentos e outras tabelas auxiliares não são APIs próprias nesta fase; seus rótulos não recebem links para endpoints inexistentes. A leitura integral de campos e o gate estático não certificam toda a Visão 360°, todos os fluxos do legado ou as fases 1–3 do novo AGENTS.md. Não se declara aprovação arquitetural automática ou merge autorizado.

---

## Complementação após CHANGES REQUESTED

Revisões lidas integralmente: [issue, comentário 6455474606](https://github.com/codexrodrigues/praxis-metadata-starter/issues/305#issuecomment-6455474606) e [PR, comentário 6455474233](https://github.com/codexrodrigues/praxis-ergon-showcase/pull/1#issuecomment-6455474233). A revisão registra a correção das nove fricções da rodada inicial nos commits bd7eb3f31f (guias) e 766fc1d (skills); a seção histórica abaixo preserva o diagnóstico original.

O solicitante definiu expressamente a manutenção do schema existente, sem DDL ou migrations. Permanecem ddl-auto=validate, flyway.enabled=false, 16 entidades e 392 colunas. Nomes e colunas inexistentes na revisão foram substituídos pelos atributos reais conforme a orientação posterior; nenhum campo institucional foi escondido no JSONB.

### Entrega complementar

- Fichas cadastrais e respostas cobrem todos os atributos persistidos de negócio dos quatro pilotos. A suíte compara os campos JPA com a hierarquia dos DTOs de resposta para impedir omissões. Grupos organizam identificação, filiação, documentos, endereço, bancos, acessibilidade, requisitos, vigência, ingresso, contratos, cessão, aposentadoria e FGTS; detalhes ficam ocultos na grade. Auditoria institucional aparece somente na leitura.
- Governança pessoal, financeira e sensível nos campos pertinentes e nos critérios de pesquisa. PcD conserva os cinco indicadores e obsDeficiencia; possuiDeficienciaPcD e somentePcD usam sua disjunção. Como não existe um indicador geral separado, a declaração específica já identifica a deficiência; observações complementam o registro.
- Filtros avançados cobrem LIKE, STARTS_WITH, EQUAL, IN, NOT_IN, BETWEEN, GREATER_OR_EQUAL, ON_DATE, IN_LAST_DAYS, IN_NEXT_DAYS, IS_NULL, IS_NOT_NULL e IS_TRUE, incluindo pessoa.nome, pessoa.cpf e cargoEntidade.nome. Os quatro recursos têm filtros rápidos.
- Três PATCH com ResourceIntent e UiSurface PARTIAL_FORM: lotacao-exercicio e previdencia-pagamento no vínculo; dados-bancarios-contato na pessoa. Propriedades ausentes ou nulas preservam o valor persistido. Identidade, órgão e campos fora da superfície não são atribuídos.
- Ações registrar-cessao, encerrar-cessao, conceder-aposentadoria, homologar-estagio-probatorio e extinguir-cargo, além da vacância existente. Cada comando verifica versão e estado persistido; dados de ciclo de vida são exclusivos dessas operações. O provedor único de estado atende vínculos e vigência dos cargos.
- Fatos publicados e calculados: emEstagioProbatorio, possuiCargoComissao, emCessaoAtiva, idadeAnos, possuiDeficienciaPcD, cargoExtinto e saldoVagasDisponiveis. Estágio indica exercício ativo sem homologação; idade e dias são medidas civis, sem certificar elegibilidade jurídica.
- Invariantes: cronologia de ingresso/contrato, cargo extinto inclusive quando a extinção tem data futura, saldo de vagas, redução do quadro abaixo da ocupação, cargo aglutinador habilitado no mesmo órgão, lotação/exercício/cargo em comissão no mesmo órgão, registro profissional com número e tipo, CPF/CNPJ únicos. Acumulação operacional limitada a dois vínculos ativos nas categorias MAGISTERIO ou SAUDE. Campos inexistentes de conselho/acumulação não foram criados.
- Decisões de provimento bloqueiam o cargo, e decisões sobre vínculos da pessoa bloqueiam seu cadastro. Consultas de validação evitam autoflush anterior à decisão. A bateria comprova regras e transações; não representa ensaio de carga concorrente ou certificação jurídica da acumulação.

### Evidência da complementação

Gates finais: mvn clean test e mvn clean package passaram, ambos com **25 testes, zero falhas, zero erros e zero ignorados**, em JDK 21 e PostgreSQL Neon real. O JAR final foi iniciado: health, OpenAPI, consulta autenticada de vínculo e catálogo com as novas ações retornaram HTTP 200; ETag presente. O processo foi encerrado. O resumo atualizado do Surefire e os oito schemas capturados constam em evidence. A conferência pós-suíte confirmou 392 colunas, nenhuma fixture residual e o vínculo 1001:1 ainda ATIVO. Todas as mutações usam a branch isolada e rollback; o banco principal não é destino dos testes.

As falhas intermediárias foram corrigidas: combinação de provedores de estado em uma única fronteira e publicação explícita de eMail no JSON. Não houve alterações de schema, dependências, plataforma ou código do quickstart.

Nenhum frontend Angular consome ainda esses novos contratos neste repositório; a prova desta entrega é backend HTTP, JPA, schema e discovery. Não há artefatos globais de landing/corpus a sincronizar nesta etapa.

---

## Identificação e escopo

- Data: 27/10/2026.
- Executor: Codex; implementação e correções sem edição humana de código.
- Projeto novo: `D:\Developer\praxis-plataform\praxis-ergon-showcase`.
- Branch: `feat/ergon-showcase-scaffold-pilots`.
- Java 21.0.10, Spring Boot 3.5.15, Maven 3.9.6.
- Starter: `io.github.codexrodrigues:praxis-metadata-starter:8.0.0-rc.179`, artefato local solicitado; não representa comprovação de publicação no Maven Central.
- Neon: projeto `holy-mouse-40140108`, database `neondb`, PostgreSQL 17.11. Branch isolada `br-super-sound-b7urizmd` (`issue-305-host-proof-20261027`), derivada de `br-bitter-bonus-b7i0vfko`.

O pedido do usuário substitui os pilotos originais da issue por orgaos, cargos, servidor-pessoas e vinculos. As tabelas continuam empresas, cargos, funcionarios e vinculos. Os resourceKeys administracao-pessoal.* são os cadastrados no Neon; alterá-los para ergon.* desconectaria o catálogo já populado.

## Material e método

Leitura das skills praxis-java-host-project, resource-baseline, host-checklist, praxis-dto-annotations, praxis-java-resource-authoring e praxis-resource-entity-lookup-backend; GUIA-01, GUIA-02, protocolo e template de prova. A skill praxis-java-stats-export-authoring complementou a política de exportação. Hashes das fontes centrais: [source-hashes.json](source-hashes.json).

Handoff final: arquivo da raiz informado pelo usuário; a cópia anterior em docs/architecture também foi lida. Fontes de domínio: comentários completos do pg_catalog e documentos VINCULOS.TAB, CARGOS_.TAB e FUNCIONARIOS.TAB em docs-legado/v7x/aps. As descrições de DTO foram redigidas deliberadamente; não houve enriquecimento heurístico de @Schema. O [snapshot do catálogo](database-schema.json) preserva a proveniência dos comentários.

Nenhum código do praxis-api-quickstart foi consultado ou copiado. Diante das divergências, foram inspecionadas assinaturas públicas do JAR com javap e trechos do starter canônico. Portanto esta entrega demonstra um host corrigido e validado, mas não uma execução de primeira tentativa sustentada exclusivamente pelos exemplos dos guias.

## Implementação

- 16 entidades, 392 colunas, comentários Hibernate e JavaDoc em português; tipos CHAR explícitos, JSONB, timestamps e versionamento. AuditoriaErgon é @Embedded; quatro identidades naturais compostas implementam CompositeResourceId.
- Quatro conjuntos de DTO de criação, atualização, resposta e filtro, com documentação de domínio e metadados de UI. OrgaoMapper prova a geração MapStruct; os demais mappers implementam explicitamente o contrato ResourceMapper.
- Identidade e órgão proprietário preservados na manutenção; validação de vínculo, órgão e cargo. PUT usa pré-condição forte de versão sob bloqueio transacional; flush e refresh precedem a captura da representação.
- Campos customizados globais e específicos por órgão, obrigatoriedade, regex, limites, opções de itemtabelas e governança. @CustomFields em entidades e DTOs permite filtros JSONB reais.
- Fontes RESOURCE_ENTITY e referências FK/display. Identidade composta é string no wire, opções e schema.
- Ação registrar-vacancia: ATIVO → VACANTE, data não anterior ao exercício, fundamentação e forma jurídica, If-Match, permissão de escrita e contexto de órgão. Resumo funcional é uma @UiSurface; diasDesdeExercicio é @ResourceFact.
- Cursor e locate usam identidade imutável; não prometem paginação por qualquer coluna. Exportação limitada, allowlist no servidor e executor canônico CSV/JSON/Excel. Regime de teletrabalho é o único atributo dinâmico exportável.
- Estatísticas operam apenas campos registrados: estados/categorias/UF, datas e quantidades legítimas. CPF, documentos e identidades não são medidas.

## Fricções e ajustes

| Achado | Evidência e tratamento |
|---|---|
| Assinatura de concorrência divergente | GUIA-02 e resource-baseline mostram requireMatch(id, version); rc.179 oferece requireMatch(long). Corrigido no host após javap. Atualizar os exemplos canônicos. |
| Cursor prometido, porém ausente na base | resource-baseline afirma implementação básica; AbstractBaseQueryResourceService.filterEntitiesByCursor lança UnsupportedOperationException. O teste retornou 501. Implementado o ponto de extensão com keyset pelas identidades Ergon, reutilizando predicates e contratos canônicos. A promessa genérica da skill precisa correção. |
| ResourceReference do exemplo não compila contra esta API | Exemplo usa resourcePath/idField; o artefato expõe resourceKey/valueField. Host usa a API real. |
| Handoff internamente divergente | Introdução da cópia final menciona funcoes, cargo composto e identidade gerada de funcionarios; dicionário detalhado e banco têm 16 tabelas sem funcoes, cargo simples e numero atribuído. O mapeamento segue pg_catalog e o dicionário detalhado. |
| CHAR requer tipo JDBC explícito | Apenas columnDefinition=character(n) não basta: Hibernate esperava VARCHAR e encontrou CHAR. Adicionado @JdbcTypeCode(SqlTypes.CHAR), mantendo ddl-auto=validate. |
| Discovery depende de HTTP interno | MockMvc isolado não atende a leitura de OpenAPI; depois houve 401. Prova usa servidor real em porta aleatória e OpenAPI acessível ao leitor interno. Dados continuam autenticados. |
| Estatísticas têm gate global | AUTO + StatsFieldRegistry não bastam com praxis.stats.enabled=false. Configurado enabled=true e executados testes operacionais. |
| @CustomFields também na entidade | DTOs projetavam schema, mas filtro JSONB retornava Entity has no @CustomFields container. Marcado o atributo persistente e repetida a prova. |
| /resource/schemas é redirecionamento | O protocolo pede 200; o starter retorna 302 para /schemas/filtered. O teste verifica Location e valida diretamente a superfície estrutural com 200. |

Erros de implementação corrigidos durante a rodada, sem atribuí-los ao guia: import inicial de UISchema no pacote errado, tentativa de DomainGovernance em classe quando seu target é campo, ação inicialmente fora de /actions/, uso inicial de operation=create em vez de post e falta de metric.operation no payload de estatística.

## Limites explícitos

X-Tenant é contexto de catálogo, não autenticação de tenant. O usuário operacional acessa o domínio demonstrativo inteiro. Não há promessa de isolamento corporativo por órgão. Não foram implementados folha, previdência, frontend Angular ou comandos de outros ciclos de vida.

As 16 entidades têm mapeamento completo; somente os quatro pilotos são APIs e seus DTOs publicam uma seleção deliberada de campos. Não há SnapshotAtoLegal inventado sobre colunas inexistentes: o banco possui referências a documentos legais, e a prova de @Embedded ocorre com AuditoriaErgon.

## Evolução das fontes canônicas

As fricções acima devem alimentar GUIA-02, resource-baseline e checklist. Não foi sincronizado o catálogo de skills nesta rodada: o repositório canônico encontrado contém centenas de alterações preexistentes, e o próprio resource-baseline tem mais de mil linhas locais alteradas. Atualizar hashes e sincronizar esse estado ampliaria a execução além de uma correção isolável. O diagnóstico e a proposta ficam preservados neste relatório, sem sobrescrever o trabalho em curso.

Nenhuma alteração necessária em Angular, landing ou corpus HTTP global: eles ainda não publicam estes recursos. Artefatos derivados locais são README, snapshot de banco, relatório e schemas capturados pelos testes.

## Gates e veredito

| Gate | Resultado |
|---|---|
| mvn clean test-compile | BUILD SUCCESS, JDK 21 |
| mvn test | 14 testes, 0 falhas, 0 erros, 0 ignorados |
| mvn clean package | BUILD SUCCESS; os mesmos 14 testes passaram novamente |
| Hibernate ddl-auto=validate | 16 entidades / 392 colunas, PostgreSQL 17.11 |
| JAR empacotado iniciado por java -jar | Health, OpenAPI e GET autenticado de vínculo retornaram 200; ETag presente |
| Schemas request/response | 8 capturas, ETag e X-Schema-Hash; chave composta, x-ui, governança e campos por órgão verificados |
| CRUD, filtros, cursor, locate, opções, stats e exportação | Integração PostgreSQL real aprovada, incluindo casos negativos de versão, campo obrigatório e medida/exportação não permitida |

Evidências versionadas em [evidence](evidence): resumo Surefire, schemas capturados, hashes do starter e do JAR e smoke do pacote. Os logs completos permanecem em .local, ignorados pelo Git; não contêm configuração de conexão impressa.

Os testes usam Neon real, não mocks de persistência nem H2. Mutações têm rollback; a branch principal do banco não recebe escrita. A branch Neon de prova permanece disponível para revisão. Todos os servidores/shells auxiliares iniciados para validar o host foram encerrados.

A rodada inicial revela lacunas nos materiais e não sustenta a afirmação de prova de fogo sem fricção. **Host aprovado nos gates técnicos após correções autônomas; materiais reprovados como receita suficiente sem diagnóstico adicional.** Consumo Angular não foi executado e não pertence ao escopo solicitado. A próxima rodada pode partir deste backend; antes de repetir a prova cega de geração, corrigir os materiais enumerados.
