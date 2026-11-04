# AGENTS.md - Praxis Ergon Showcase

## Escopo e Herança
- Escopo: aplica-se a `praxis-ergon-showcase` e subpastas.
- Herda: segue o `AGENTS.md` da raiz do monorepo (`D:\Developer\praxis-plataform\AGENTS.md`).

## Topologia de Banco de Dados e Projetos Neon
- **Separação Obrigatória Domínio vs. Config (`ErgonPersistenceConfiguration`):**
  - `ErgonPersistenceConfiguration.ConfigStore.configDataSource` valida no startup que o banco Config (`CONFIG_DATASOURCE_URL`, `CONFIG_DATASOURCE_USERNAME`, `CONFIG_DATASOURCE_SCHEMA`) utiliza **`current_database()` e `current_user` distintos** do banco de domínio (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`). Se apontarem para o mesmo database ou usuário, o startup falha imediatamente.
- **Projeto Neon do Ergon vs. Projeto Neon do Quickstart:**
  - O projeto Neon **`praxis-ergon-showcase` (`holy-mouse-40140108`)** é o único projeto Neon deste host:
    - Branch `main` (`br-bitter-bonus-b7i0vfko`): database `neondb` (`public` com 16 tabelas base).
    - Branch `issue-305-host-proof-20261027` (`br-super-sound-b7urizmd`): database `neondb` (`public` com 16 tabelas + `praxis_bulk` com 32 tabelas). Branch base atribuída sem expiração; **nunca** deve receber mutações diretas de suíte nem TTL.
    - Branch `ps3b-gate-20261103` (`br-purple-dust-b70ss3dj`): possui o database de domínio **`ps3b_ergon`** (`public` com 30 tabelas até V10 + `praxis_bulk` com 32 tabelas) e o database Config separado **`ps3b_config`** (`public` com 66 tabelas Config + `ps3b_tx_probe`).
  - O projeto Neon **`praxis-config` (`ancient-leaf-72055027`, branch `production` / `br-aged-lake-a4eb715l`)** pertence ao **`praxis-api-quickstart`** (contendo as definições `desenv`/`local` do Quickstart). Não utilizá-lo nem criar branches nele para tarefas exclusivas do Ergon.

## Escopo de Governança Config e Policy Studio
- **`tenantId` e `environment`:**
  - O `tenantId` no Config corresponde exatamente ao código decimal do órgão (`empCodigo`) em formato String (`"1"`, `"2"`, `"3"`, `"7"`).
  - O `serviceKey` / `ownerServiceKey` canônico é `"praxis-ergon-showcase"` (`ErgonPolicyComposition.OWNER`).
  - A versão ativa do contrato de host em produção é `"ergon-policy-studio/1.0"` (`ErgonPolicyComposition.HOST_CONTRACT`); a coordenada candidata para o corte da migração ontológica é `"ergon-policy-studio/2.0"`.
  - Contas de governança Config são declaradas em `showcase.security.config-principals[i].{username,password,organization,environment,roles}` (`ErgonConfigPrincipals`), gerando a autoridade `ERGON_CONFIG_SCOPE:<organization>:<environment>` validada por `ErgonConfigAccess` (que rejeita com `403` qualquer divergência com os hints `X-Tenant-ID`, `X-Env` ou `X-User-ID`).
- **Estado de Regras, Snapshots e Heads:**
  - O host opera em memória via `ErgonRuleEngineFacade` com as 18 famílias de regras (`productRuleSet()` como padrão e `orgao2FactoryRuleSet()` para o Órgão 2), cenários em `src/main/resources/policy-studio/scenarios.json`, preparação de manifesto em `ErgonPolicyComposition.prepare` e admissão verificada em `ErgonRuleEngineFacade.admitGovernedSnapshot`.
  - No banco `ps3b_config`, a ausência de snapshots publicados (`0` em `domain_rule_snapshot`) e de heads ativos (`0` em `domain_rule_snapshot_head`) é esperada no estágio atual (há apenas 3 definições aprovadas de prova em `domain_rule_definition` para `tenant_id='2'` e `environment='stage'`).

## Inicialização, Migrações e Portas Locais
- **Startup HTTP (`spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=validate`):**
  - Porta padrão do backend: `http://localhost:8080` (`/api/ergon/**`, `/api/ergon/policy-studio/**`, `/api/praxis/config/**`, `/schemas/**`, `/praxis/cockpit/**`).
  - Frontend Ergon (`../praxis-ergon-showcase-ui`): `http://127.0.0.1:4303` (`npm start`).
  - Frontend Policy Studio (`../praxis-policy-studio`): `http://localhost:4302` (`npm start`).
- **Instaladores explícitos de schema (nunca rodam automaticamente no startup HTTP):**
  - **Domínio (`public`):** scripts SQL em `src/main/resources/db/migration/V5__*.sql` até `V10__*.sql`.
  - **Bulk (`praxis_bulk`):** `br.com.praxis.ergonshowcase.operations.InstallBulkStorage` (usa `BULK_OWNER_USERNAME` / `BULK_OWNER_PASSWORD`).
  - **Config (`public` no database Config separado):** `br.com.praxis.ergonshowcase.operations.InstallConfigStorage <caminho-jar-praxis-config-starter>` (usa `CONFIG_MIGRATION_USERNAME` / `CONFIG_MIGRATION_PASSWORD`, obrigatoriamente distintos de `CONFIG_DATASOURCE_USERNAME`).

## Validação e Política de Branches Descartáveis no Neon
- **Validação local rápida (sem exigir branch descartável Neon):**
  - Quando `BULK_PROOF_DISPOSABLE_BRANCH` não está definida, o profile Maven `offline-without-neon-branch` exclui automaticamente `**/*Neon*Test.java`, permitindo rodar `mvn test` ou testes focais (`mvn "-Dtest=<ClasseTest>" test`) sem mutar o Neon.
  - Auditor estático dos 20 pilares: `python ../scripts/workspace/audit-java-resource-completeness.py . --strict`.
- **Provas com PostgreSQL Neon real (`*Neon*Test.java`):**
  - Criar cópia descartável diretamente da branch base atribuída no projeto `holy-mouse-40140108` com `expires_at` de no máximo 24h (jamais encadear descartável a partir de outra descartável).
  - Definir `PROOF_NEON_BRANCH`, `BULK_PROOF_DISPOSABLE_BRANCH` e `BULK_PROOF_EXPECTED_HOST` apontando para a cópia descartável (e `PS3B_NEON_PROOF=true` quando executar `Ps3bConfigNeonTest`).
  - Ao concluir a prova: registrar evidências/hashes em `docs/evidence/`, encerrar processos Java/shells auxiliares e **excluir imediatamente a branch descartável**, verificando na listagem de branches que apenas as branches permanentes permanecem ativas.
