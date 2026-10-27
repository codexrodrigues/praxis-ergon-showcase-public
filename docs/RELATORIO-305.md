# Relatório de rodada — issue 305

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
