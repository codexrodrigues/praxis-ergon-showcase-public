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

Cada piloto publica CRUD, filtro paginado, opções, fontes RESOURCE_ENTITY, cursor por identidade, localização, capacidades, schemas e estatísticas elegíveis. As 16 tabelas estão mapeadas; somente os quatro pilotos têm endpoints de manutenção. Os DTOs expõem os campos cadastrais deliberadamente selecionados: o mapeamento completo de uma tabela não transforma automaticamente todas as suas colunas em campos editáveis.

PUT exige o ETag forte obtido no GET em `If-Match`: ausência retorna 428 e versão divergente retorna 412. Identidades e órgão proprietário são definidos na criação. Campos do ciclo de vida não pertencem ao PUT cadastral.

`POST /api/ergon/vinculos/{id}/actions/registrar-vacancia` recebe `data`, `forma` e `motivo`, exige ETag e estado ATIVO, e registra VACANTE. A ação não calcula folha ou direitos previdenciários. `GET /api/ergon/vinculos/{id}/resumo-funcional` é a superfície de consulta; `diasDesdeExercicio` conta dias corridos, sem equivaler a tempo previdenciário certificado.

## Segurança e órgão

O usuário operacional configurado por ambiente recebe ERGON_READ e ERGON_WRITE. HTTP Basic é adequado ao showcase atrás de HTTPS; as credenciais não são fixadas no código. Health e OpenAPI são públicos para permitir a leitura interna de contratos pelo starter. Dados e discovery exigem autenticação; comandos exigem escrita.

`X-Tenant` seleciona o catálogo de campos do órgão (padrão 1), com precedência das definições específicas sobre as globais. Não é uma fronteira de autorização multitenant: a identidade operacional tem acesso ao cadastro demonstrativo inteiro. Criação/alteração de cargos e vínculos exige coerência entre o órgão persistido e esse contexto.

CPF, identificação civil e contatos carregam governança de dados pessoais e política de mascaramento para IA. Essas anotações não substituem autorização de acesso ao cadastro pelo host.

## Consulta e exportação

Cursor e locate usam ordenação pela identidade imutável, ascendente ou descendente; outras ordenações são rejeitadas com 422. O filtro paginado comum permite as ordenações do starter.

Exportação síncrona em CSV, JSON ou Excel, escopo filtrado, ordem canônica de identidade e teto de 1000 linhas. Seleção, paginação e ordenação particulares não são anunciadas nem aceitas. A allowlist do servidor exclui CPF, contatos e dados bancários; o cadastro civil exporta somente o número funcional. O executor canônico protege os formatos tabulares contra fórmulas. Dos campos dinâmicos, somente o regime de teletrabalho é exportável nesta fase; processo de nomeação e atributos de armamento não pertencem à allowlist.

## Provas e fontes

`mvn test` conecta ao PostgreSQL Neon real e faz rollback das mutações; não usa H2. Use uma branch isolada. A execução desta issue usa `br-super-sound-b7urizmd`, filha de main, no projeto `holy-mouse-40140108`.

Os schemas capturados pelos testes ficam em `target/proof/`, e o Surefire em `target/surefire-reports/`. Veja [plano e impacto](docs/PLANO-305.md), [catálogo de colunas](docs/database-schema.json) e o relatório de prova.

As descrições de negócio foram redigidas a partir dos comentários do banco e de `docs-legado/v7x/aps/VINCULOS.TAB`, `CARGOS_.TAB` e `FUNCIONARIOS.TAB`. Não foi consultado nem copiado código do quickstart.
