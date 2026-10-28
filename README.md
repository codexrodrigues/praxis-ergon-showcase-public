# Praxis Ergon Showcase

Host Java 21 / Spring Boot 3.5.15 criado do zero para a [issue 305](https://github.com/codexrodrigues/praxis-metadata-starter/issues/305), consumindo `io.github.codexrodrigues:praxis-metadata-starter:8.0.0-rc.179`. Requer o artefato atualizado no Maven local indicado pela execução.

## Execução

Defina as variáveis de [.env.example](.env.example) no ambiente do processo. Spring Boot não carrega esse arquivo automaticamente. Use credenciais da branch Neon escolhida e uma chave aleatória de ETag. Não versione valores reais.

```powershell
$env:JAVA_HOME='D:\Developer\praxis-plataform\.codex-jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn clean test-compile
mvn test
mvn clean package
java -jar target/praxis-ergon-showcase-0.1.0-SNAPSHOT.jar
```

Porta padrão: 8080. Os testes iniciam Tomcat em porta aleatória. O schema existente é validado; não há migrations, seed automático nem criação de tabelas.

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

Determinações HOST de formulário: POST `/servidor-pessoas/determinations/pcd`, `/vinculos/determinations/provimento` e `/cargos/determinations/aglutinador`, sob `/api/ergon`. Os schemas CREATE/EDIT publicam bindings de operação; a avaliação exige ERGON_READ, não persiste dados e respeita o órgão do contexto na seleção de cargo. Campos de prévia são informativos, não podem ser enviados no submit e não reservam vagas. O backend revalida as invariantes na escrita. A integração com decisões governadas depende de identificar um snapshot aprovado do Config; o gate operacional permanece pendente.

Manutenções especializadas com If-Match: PATCH `/api/ergon/vinculos/{id}/lotacao-exercicio`, `/api/ergon/vinculos/{id}/previdencia-pagamento` e `/api/ergon/servidor-pessoas/{id}/dados-bancarios-contato`. Propriedades ausentes ou nulas preservam os valores existentes; esses formulários mantêm somente seus campos próprios. Nome público de correio eletrônico da pessoa: `eMail`.

As invariantes verificam cronologia de ingresso e contrato, cargo extinto, saldo de vagas, cargo aglutinador, unidades/cargo em comissão no mesmo órgão, pares de identificação profissional e unicidade de CPF/CNPJ. A regra operacional de acumulação desta prova admite no máximo dois vínculos ativos e exige categorias MAGISTERIO ou SAUDE em ambos; não certifica todos os requisitos jurídicos de acumulação.

PcD usa as cinco colunas existentes (`deffisica`, `defvisual`, `defauditiva`, `defmental`, `defintelectual`) e `obsDeficiencia`. O fato e o filtro `somentePcD` usam a disjunção desses indicadores, preservando deficiências múltiplas. Não há coluna sintética ou alteração do schema.

## Segurança e órgão

O usuário operacional configurado por ambiente recebe ERGON_READ e ERGON_WRITE. HTTP Basic é adequado ao showcase atrás de HTTPS; as credenciais não são fixadas no código. Health e OpenAPI são públicos para permitir a leitura interna de contratos pelo starter. Dados e discovery exigem autenticação; comandos exigem escrita.

`X-Tenant` seleciona o catálogo de campos do órgão (padrão 1), com precedência das definições específicas sobre as globais. Não é uma fronteira de autorização multitenant: a identidade operacional tem acesso ao cadastro demonstrativo inteiro. Criação/alteração de cargos e vínculos exige coerência entre o órgão persistido e esse contexto.

CPF, identificação civil e contatos carregam governança de dados pessoais e política de mascaramento para IA. Essas anotações não substituem autorização de acesso ao cadastro pelo host.

## Consulta e exportação

Cursor e locate usam ordenação pela identidade imutável, ascendente ou descendente; outras ordenações são rejeitadas com 422. O filtro paginado comum permite as ordenações do starter.

Exportação síncrona em CSV, JSON ou Excel, escopo filtrado, ordem canônica de identidade e teto de 1000 linhas. Seleção, paginação e ordenação particulares não são anunciadas nem aceitas. A allowlist do servidor exclui CPF, contatos e dados bancários; o cadastro civil exporta somente o número funcional. O executor canônico protege os formatos tabulares contra fórmulas. Dos campos dinâmicos, somente o regime de teletrabalho é exportável nesta fase; processo de nomeação e atributos de armamento não pertencem à allowlist.

## Provas e fontes

`mvn test` conecta ao PostgreSQL Neon real e faz rollback das mutações; não usa H2. Use uma branch isolada. A execução desta issue usa `br-super-sound-b7urizmd`, filha de main, no projeto `holy-mouse-40140108`.

Os schemas capturados pelos testes ficam em `target/proof/`, e o Surefire em `target/surefire-reports/`. Veja [plano e impacto](docs/PLANO-305.md), [catálogo de colunas](docs/database-schema.json) [relatório de prova](docs/RELATORIO-305.md), [scorecard](docs/evidence/SCORECARD-COMPLETUDE.md) e [correções do auditor canônico](docs/tooling/README.md).

As descrições de negócio foram redigidas a partir dos comentários do banco e de `docs-legado/v7x/aps/VINCULOS.TAB`, `CARGOS_.TAB` e `FUNCIONARIOS.TAB`. Não foi consultado nem copiado código do quickstart.

Fechamento da revisão 61fc36b: [plano por fases](docs/PLANO-REVISAO-61fc36b.md) e [matriz de paridade e limites](docs/MATRIZ-PARIDADE-305.md). O teste de consultas mede o caminho inteiro em contexto frio: página, cursor e exportação mantiveram quatro statements para uma ou três linhas. O auditor distingue STATIC PASS de recibos operacionais; execute `python ../scripts/workspace/audit-java-resource-completeness.py . --strict --require-runtime-evidence` para incluir os gates do manifesto. Ele deve recusar o aceite enquanto governedDecisions estiver pendente.

O helper `scripts/workspace/prove-angular-consumer.mjs` compila e executa o normalizador Core e ReactiveDeterminationRuntimeService das fontes oficiais do workspace Angular, usando seu tsconfig oficial. Preparação: copie angular-consumer-package.json e angular-consumer-package-lock.json para `.local/angular-proof/package.json` e `package-lock.json`, execute npm ci nesse diretório (registry público disponível), carregue SHOWCASE_USERNAME/SHOWCASE_PASSWORD por ambiente e defina PROOF_NEON_BRANCH=br-super-sound-b7urizmd. Com o host iniciado contra essa branch e schemas gerados por mvn test, execute `node scripts/workspace/prove-angular-consumer.mjs ../praxis-ui-angular <base-url-do-host> target/proof`. O helper exige a ausência do cargo reservado 9590 e o remove ao terminar. A prova exercitou seis schemas e seis execuções, resultados obsoletos e erro visível; não certifica pacote npm publicado nem composição visual desktop/narrow. Nenhuma biblioteca Angular foi alterada nesta entrega.
