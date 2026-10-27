# Relatório de rodada — issue 305

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
