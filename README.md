# Praxis Ergon Showcase

Host Java 21 / Spring Boot 3.5.15 criado do zero para a [issue 305](https://github.com/codexrodrigues/praxis-metadata-starter/issues/305). A rc.179 permanece declarada no POM; as correções atuais dependem do candidato privado indicado abaixo. A compilação contra o artefato público não está certificada.

Bulking #2/#308 está em correção após a revisão de `2013442`; os resultados anteriores não certificam o código desta rodada. O estado vigente está no relatório e no scorecard. A prova usa candidato Maven privado do [PR Metadata #311](https://github.com/codexrodrigues/praxis-metadata-starter/pull/311), sem substituir os bytes da rc.179. O comando padrão abaixo não é uma prova aprovada do novo lote. Consulte o [relatório](docs/RELATORIO-305.md), o [scorecard](docs/evidence/SCORECARD-COMPLETUDE.md) e a [reconciliação dos campos](docs/RECONCILIACAO-CAMPOS-BULK-308.md).

## Execução

Defina as variáveis de [.env.example](.env.example) no ambiente do processo. Spring Boot não carrega esse arquivo automaticamente. Use credenciais da branch Neon escolhida e uma chave aleatória de ETag. Não versione valores reais.

```powershell
$env:JAVA_HOME='D:\Developer\praxis-plataform\.codex-jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
# Candidato privado instalado a partir do PR #311; não equivale à rc.179 pública.
mvn '-Dpraxis.core.version=8.0.0-bulk.308-consolidation.3-SNAPSHOT' '-Dmaven.repo.local=.local/m2-bulk308' test
mvn '-Dpraxis.core.version=8.0.0-bulk.308-consolidation.3-SNAPSHOT' '-Dmaven.repo.local=.local/m2-bulk308' clean package -DskipTests
java -jar target/praxis-ergon-showcase-0.1.0-SNAPSHOT.jar
```

Porta padrão: 8080. Os testes iniciam Tomcat em porta aleatória. O schema de domínio existente é validado; não há migrations de domínio nem seed automático. O armazenamento técnico de lote tem instalação explícita separada, descrita abaixo.

## Recursos

| Endpoint | Tabela | Identidade no wire |
|---|---|---|
| /api/ergon/orgaos | empresas | inteiro |
| /api/ergon/cargos | cargos | inteiro |
| /api/ergon/servidor-pessoas | funcionarios | inteiro longo |
| /api/ergon/vinculos | vinculos | pessoa:sequencial, por exemplo 1001:1 |

Os resourceKeys `administracao-pessoal.*` preservam a vinculação ao catálogo de campos dinâmicos já existente no Neon. Os nomes de recursos desta entrega seguem a instrução do solicitante, que substitui os quatro nomes originalmente listados na issue.

Cada piloto publica CRUD, filtros avançados e rápidos, opções, fontes RESOURCE_ENTITY, cursor por identidade, localização, capacidades, schemas e estatísticas elegíveis. As 16 tabelas estão mapeadas; os quatro pilotos expõem em leitura todos os seus atributos persistidos de negócio, organizados em grupos semânticos. Campos de ciclo de vida permanecem exclusivos dos comandos específicos. Dados secundários são ocultos na grade e disponíveis na ficha; dados pessoais, financeiros e de saúde carregam governança.

PUT exige o ETag forte obtido no GET em `If-Match`: ausência retorna 428 e versão divergente retorna 412. Identidades e órgão proprietário são definidos na criação. Campos do ciclo de vida não pertencem ao PUT cadastral.

`POST /api/ergon/vinculos/{id}/actions/registrar-vacancia` recebe `data`, `forma`, `motivo` e, opcionalmente, `dtPgtoAte`, exige ETag e estado ATIVO, e registra VACANTE. A data final de pagamento não pode anteceder a vacância. A ação não calcula folha ou direitos previdenciários. `GET /api/ergon/vinculos/{id}/resumo-funcional` é a superfície de consulta; `diasDesdeExercicio` conta dias corridos, sem equivaler a tempo previdenciário certificado.

Outras ações de vínculo: `registrar-cessao`, `encerrar-cessao`, `conceder-aposentadoria` e `homologar-estagio-probatorio`, sob `/{id}/actions/`. Cargos publicam `/{id}/actions/extinguir-cargo`; a extinção exige ausência de ocupantes ativos, afastados ou cedidos.

`registrar-afastamento` recebe código de frequência elegível, início, término previsto opcional, motivo e documento legal do órgão. Registra afastamento HOMOLOGADO e muda ATIVO → AFASTADO; rejeita sobreposição. `encerrar-afastamento` recebe afastamentoId, dtfim e documentoLegalId, confere propriedade e período aberto, contabiliza dias inclusivos e retorna AFASTADO → ATIVO. `afastamentoAbertoId` identifica o único registro aberto; é nulo se nenhum ou mais de um existir. Ambas as ações exigem If-Match.

Órgãos publicam `ativar-orgao` e `inativar-orgao`, com estado e If-Match; ativa não pertence ao PUT cadastral. Novas admissões exigem órgão ativo. Cargos em comissão integram saldo de vagas e bloqueio de extinção, sem dupla contagem quando coincidem com o cargo titular. Jornada semanal informada deve estar entre 1 e 168 horas.

Superfícies de consulta: `GET /orgaos/{id}/ficha-institucional`, `/cargos/{id}/ficha-cargo` e `/servidor-pessoas/{id}/ficha-cadastral`, sob `/api/ergon`; mantêm ETag e autoridade ERGON_READ. Os títulos e referências apontam para fichas reais. Setores e documentos legais publicam recursos auxiliares somente de leitura em `/api/ergon/setores` e `/api/ergon/documentos-legais`, com ficha, filtros e fontes de opções nomeadas. FKs possuem controles ENTITY_LOOKUP, tipo e ícone explícitos; detalhes de comissão, aglutinação e unidade possuem referências de navegação.

Coleções relacionadas paginadas: órgão `/{id}/quadro-cargos`; pessoa `/{id}/vinculos-funcionais`; cargo `/{id}/vinculos-titulares` e `/{id}/vinculos-comissao`. Exigem ERGON_READ e verificam a existência do pai antes de filtrar os filhos.

Determinações HOST de formulário: POST `/servidor-pessoas/determinations/pcd`, `/vinculos/determinations/provimento` e `/cargos/determinations/aglutinador`, sob `/api/ergon`. Os schemas CREATE/EDIT publicam bindings de operação; a avaliação exige ERGON_READ, não persiste dados e respeita o órgão do contexto na seleção de cargo. Campos de prévia são informativos, não podem ser enviados no submit e não reservam vagas. O backend revalida as invariantes na escrita. O responsável aprovou a Fase 3A de e6b9fa0 com PASS e definiu Config/rules-engine como fase futura dedicada; não existe snapshot externo a integrar nesta etapa.

Manutenções especializadas com If-Match: PATCH `/api/ergon/vinculos/{id}/lotacao-exercicio`, `/api/ergon/vinculos/{id}/previdencia-pagamento` e `/api/ergon/servidor-pessoas/{id}/dados-bancarios-contato`. Propriedades ausentes preservam os valores existentes; `null` explícito limpa campos anuláveis. `correio` e `descontaIr` não aceitam `null` e retornam 400; esses formulários mantêm somente seus campos próprios. Nome público de correio eletrônico da pessoa: `eMail`.

As invariantes verificam cronologia de ingresso e contrato, cargo extinto, saldo de vagas, cargo aglutinador, unidades/cargo em comissão no mesmo órgão, pares de identificação profissional e unicidade de CPF/CNPJ. A regra operacional de acumulação desta prova admite no máximo dois vínculos ativos e exige categorias MAGISTERIO ou SAUDE em ambos; não certifica todos os requisitos jurídicos de acumulação.

PcD usa as cinco colunas existentes (`deffisica`, `defvisual`, `defauditiva`, `defmental`, `defintelectual`) e `obsDeficiencia`. O fato e o filtro `somentePcD` usam a disjunção desses indicadores, preservando deficiências múltiplas. Não há coluna sintética ou alteração do schema.

## Operações em lote — Issues #2 / #308

A campanha de lote usa o candidato do [PR Metadata #311](https://github.com/codexrodrigues/praxis-metadata-starter/pull/311), com coordenada distinta `8.0.0-bulk.308-consolidation.3-SNAPSHOT` e cache Maven isolado. O POM do host ainda conserva rc.179; não houve substituição dos bytes dessa coordenada nem release público. Consulte o [plano e as reconciliações de schema](docs/PLANO-BULKING-308.md) e o scorecard para o estado de aceitação. A presença de endpoints e annotations não certifica a execução do lote.

Vínculos, pessoas e cargos declaram respectivamente 10, 7 e 5 campos BulkEditable. Cada recurso oferece avaliação uniforme e por item, proposta/preview paginado, confirmação com Idempotency-Key, execução/recibos e cancelamento. Vínculos acrescentam realocar-setor-lote e homologar-estagio-lote, com documento legal do mesmo órgão. A avaliação conserva a intenção protegida; a confirmação revalida domínio, autorização e versão por registro. QUERY aplica o órgão de operação antes do limite e congela a população na avaliação, incluindo exclusões. Pessoas são globais neste modelo; X-Tenant ainda não constitui autorização multi-órgão.

O runtime usa a role comum de JPA/JDBC `ergon_showcase_bulk_runtime`; o control plane usa `ergon_showcase_bulk_control` em conexão separada. BULK_OWNER_USERNAME identifica o owner esperado; a senha owner pertence somente ao instalador explícito InstallBulkStorage, que usa conexão direta. A aplicação não executa migrations automaticamente. O instalador oficial cria apenas o schema técnico praxis_bulk; as 16 tabelas/392 colunas de domínio continuam com ddl-auto=validate e flyway.enabled=false.

A identidade cadastral recebe ERGON_READ/ERGON_WRITE; a identidade opcional SHOWCASE_BULK_ADMIN_USERNAME/SHOWCASE_BULK_ADMIN_PASSWORD recebe ERGON_READ/ERGON_BULK_ADMIN, sem escrita cadastral. Configure credenciais distintas. Sem administrador configurado, a publicação permanece inacessível. Após instalar e validar a infraestrutura técnica, a publicação exige ERGON_BULK_ADMIN e é explícita em `POST /api/ergon/operations/bulk/publication`: operationId declarado, expectedGeneration lido no control plane e intent PUBLISH, RECONCILE ou SUSPEND. A suspensão global também avança gerações de operações READY; leia o novo estado antes da próxima transição. Nunca repita automaticamente um CAS perdido ou marque READY por SQL.

Servers no OpenAPI é relativo. O redirect do Swagger UI usa a origem absoluta anunciada do host, padrão localhost:8080; em outro endereço, configure a propriedade oficial springdoc.swagger-ui.oauth2-redirect-url. Isso impede que portas internas diferentes alterem a fotografia pública. BULK_CURSOR_KEY deve conter 32 bytes aleatórios em Base64, compartilhados entre réplicas da mesma implantação e renovados ao preparar uma cópia restaurada para prova.

## Segurança e órgão

O usuário operacional configurado por ambiente recebe ERGON_READ e ERGON_WRITE. HTTP Basic é adequado ao showcase atrás de HTTPS; as credenciais não são fixadas no código. Health e OpenAPI são públicos para permitir a leitura interna de contratos pelo starter. Dados e discovery exigem autenticação; comandos exigem escrita.

`X-Tenant` seleciona o catálogo de campos do órgão (padrão 1), com precedência das definições específicas sobre as globais. Não é uma fronteira de autorização multitenant: a identidade operacional tem acesso ao cadastro demonstrativo inteiro. Criação/alteração de cargos e vínculos exige coerência entre o órgão persistido e esse contexto.

CPF, identificação civil e contatos carregam governança de dados pessoais e política de mascaramento para IA. Essas anotações não substituem autorização de acesso ao cadastro pelo host.

## Consulta e exportação

Cursor e locate usam ordenação pela identidade imutável, ascendente ou descendente; outras ordenações são rejeitadas com 422. O filtro paginado comum permite as ordenações do starter.

Exportação síncrona em CSV, JSON ou Excel, escopo filtrado, ordem canônica de identidade e teto de 1000 linhas. Seleção, paginação e ordenação particulares não são anunciadas nem aceitas. A allowlist do servidor exclui CPF, contatos e dados bancários; o cadastro civil exporta somente o número funcional. O executor canônico protege os formatos tabulares contra fórmulas. Dos campos dinâmicos, somente o regime de teletrabalho é exportável nesta fase; processo de nomeação e atributos de armamento não pertencem à allowlist.

## Provas e fontes

`mvn test` conecta ao PostgreSQL Neon real; não usa H2. Os testes cadastrais fazem rollback. As avaliações e execuções de lote possuem transações próprias e recibos duráveis: execute-os somente em cópia descartável da branch isolada atribuída e exclua essa cópia após registrar as provas. Defina PROOF_NEON_BRANCH e BULK_PROOF_DISPOSABLE_BRANCH com a identidade verificada dessa cópia e BULK_PROOF_EXPECTED_HOST com seu endpoint confirmado no provisionamento; o JDBC deve apontar para esse host. A branch original atribuída é `br-super-sound-b7urizmd`, no projeto `holy-mouse-40140108`; não deve receber as mutações duráveis da suíte de lote.

Os schemas capturados pelos testes ficam em `target/proof/`, e o Surefire em `target/surefire-reports/`. Veja [plano e impacto](docs/PLANO-305.md), [catálogo de colunas](docs/database-schema.json) [relatório de prova](docs/RELATORIO-305.md), [scorecard](docs/evidence/SCORECARD-COMPLETUDE.md) e [correções do auditor canônico](docs/tooling/README.md).

As descrições de negócio foram redigidas a partir dos comentários do banco e de `docs-legado/v7x/aps/VINCULOS.TAB`, `CARGOS_.TAB` e `FUNCIONARIOS.TAB`. Não foi consultado nem copiado código do quickstart.

Fechamento da revisão 61fc36b: [plano por fases](docs/PLANO-REVISAO-61fc36b.md) e [matriz de paridade e limites](docs/MATRIZ-PARIDADE-305.md). O teste de consultas mede o caminho inteiro em contexto frio. O auditor distingue STATIC PASS de recibos operacionais; execute `python ../scripts/workspace/audit-java-resource-completeness.py . --strict --require-runtime-evidence` para incluir os gates do manifesto. governedDecisions é NOT_APPLICABLE por decisão explícita de escopo; bulkOperations exige novas provas e não herda o aceite da Fase 3A.

O helper `scripts/workspace/prove-angular-consumer.mjs` compila e executa o normalizador Core e ReactiveDeterminationRuntimeService das fontes oficiais do workspace Angular, usando seu tsconfig oficial. Preparação: copie angular-consumer-package.json e angular-consumer-package-lock.json para `.local/angular-proof/package.json` e `package-lock.json`, execute npm ci nesse diretório (registry público disponível), carregue SHOWCASE_USERNAME/SHOWCASE_PASSWORD por ambiente e defina PROOF_NEON_BRANCH e BULK_PROOF_DISPOSABLE_BRANCH com a cópia descartável provisionada. Não há fallback para a branch original. Com o host iniciado contra essa branch e schemas gerados por mvn test, execute `node scripts/workspace/prove-angular-consumer.mjs ../praxis-ui-angular <base-url-do-host> target/proof`. O helper exige a ausência dos cargos reservados 9590/9592 e da pessoa 9591 e remove as fixtures em finally. Além dos seis schemas e execuções reativas, exercita avaliação/confirmação, recibos, idempotência, retomada, cancelamento terminal, resultado parcial por edição concorrente, CLEAR e submit parcial. O recibo só aprova a execução quando completed e fixtureRemoved forem true; não certifica pacote npm publicado nem composição visual desktop/narrow. A rodada de correção completa o método de cancelamento no cliente oficial Core. A prova usa suas fontes locais; não implica publicação npm.
