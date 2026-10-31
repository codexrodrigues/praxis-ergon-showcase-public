# Centralização de caminhos — 28/10/2026

## Classificação e decisão

Mudança **transversal**, com extração de constantes no host e orientação
**docs-apenas** no starter/skills. Capacidade já suportada, com lacuna de adoção.
As rotas HTTP, chaves das fontes, DTOs e políticas conservam seus valores.

O Showcase já tinha `constants/ApiPaths.java` para quatro recursos e três
endpoints de opções. A auditoria encontrou 40 endpoints literais nos DTOs e
referências adicionais em registry, controllers, superfícies, links e segurança.
A classe agora compõe o prefixo comum, seis recursos, seis chaves de fontes e os
cinco endpoints de opções usados pelos DTOs. Os matchers de segurança reutilizam
a base, preservando seus padrões e a ordem das regras.

## Mapa de impacto e plano

| Superfície | Tratamento |
| --- | --- |
| Host Java | Reutilizar `ApiPaths` nos DTOs/filtros/comandos, registry, controllers, surfaces, links e segurança. |
| Metadata Starter | Preservar contrato de `UISchema`, registry e runtime; atualizar guia e exemplos de autoria. |
| Skills canônicas | Reforçar DTOs, lookups, filtros, recursos e scaffold; hashes e instalação com auditoria. |
| Angular, Quickstart e corpus HTTP | Inspecionar consumo e referência; não precisam mudar porque os valores HTTP permanecem iguais. |
| Publicação | Nenhuma tag, versão, release ou deploy neste corte. |

Sequência: inventário e pesquisa → extração literal sem renomear → validação de
endpoints contra registry/mappings → documentação e skills → auditorias locais.
Alterações preexistentes de duplicate-draft e lote são preservadas; a aceitação
dessas frentes não faz parte desta refatoração.

## Padrão

```java
@UISchema(controlType = FieldControlType.ENTITY_LOOKUP,
    endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
```

`OptionSourcesConfig` usa `ApiPaths.ORGAOS` e `ApiPaths.ORGAO_SOURCE`. O starter
continua sendo o dono da publicação de `x-ui.optionSource`, incluindo recarga por
IDs. Opções estáticas não exigem endpoint. Origem/porta não pertencem à classe.

Veja a [decisão canônica, alternativas, pesquisa e gate de adoção](../../praxis-metadata-starter/docs/guides/API-PATHS-E-OPTION-SOURCES.md).

## Validação e limites

- **Java (execução anterior, não repetida neste incremento do auditor):** 6 testes aprovados, sem falhas ou skips: `ApiPathsContractTest` (2) e
  `PraxisMetadataCompletenessTest` (4). Verificam 40 campos com endpoints,
  metadata gerada, seis descritores e mappings Spring, além dos gates nativos.
- Execução com Java 21 e o candidato privado já presente no host
  `8.0.0-bulk.308-consolidation.4-SNAPSHOT`, cache `.local/m2-bulk308`, SHA-256
  `8fa91173e5a741336fd15c6530da3c2e4c112b3fdfd6d475f9dc89386c16c33c`.
  O POM permanece `rc.179`; esta prova não certifica build contra a versão pública.
- **Auditor e documentação (nova execução):** 105 testes aprovados: 36 de
  resolução/hierarquia/oráculo Java, 17 do grafo de recursos, 49 de completude e
  três do exemplo documental. Os oráculos executam 16 cenários com `javac`,
  incluindo herança válida e sombreamentos reais; não são provas HTTP.
- **Herança resolvida:** as 16 declarações anteriormente não comprovadas agora
  têm valores resolvidos pelo auditor. A prova usa o tipo declarante exato e os
  fontes selecionados das superclasses/interfaces, sem exceções para este host.
  O universo de fontes e os valores individuais estão registrados na evidência.
  Fonte ausente, colisão possível ou sintaxe não suportada continuam bloqueantes.
- **Coerência de rotas:** revisão independente confirmou igualdade exata das 54
  substituições de literais (40 nos DTOs). Nenhuma rota renomeada.
- **Skills:** seis fontes canônicas atualizadas no repositório
  `D:/Developer/praxis-codex-skills`, com manifestos e validação de estrutura/YAML.
  Workspace e instalação receberam a orientação. Foram preservadas divergências
  anteriores: 2/6 árvores instaladas têm paridade integral com a fonte canônica;
  na closure de dependências, 83/102. Isso não é sincronização integral.
- **Revisão independente da resolução de herança:** nenhum bloqueio residual
  após corrigir e retestar literals `char`, classes locais, imports dentro de
  strings e custo de travessia. As seis linhas do scorecard têm zero problemas
  editoriais; a aprovação global continua pendente exclusivamente pela decisão
  preexistente de duplicate-draft de Cargo, preservada neste corte.
- Não houve teste HTTP/Neon, publicação ou validação visual: esta extração preserva
  os valores compilados e não muda UI. As provas locais não substituem integração
  nem comprovam publicação/consumo de artefatos.

Comando Java reproduzível (executar com Java 21 na raiz do Showcase):

```powershell
mvn -o "-Dmaven.repo.local=D:/Developer/praxis-plataform/praxis-ergon-showcase/.local/m2-bulk308" "-Dpraxis.core.version=8.0.0-bulk.308-consolidation.4-SNAPSHOT" "-Dtest=ApiPathsContractTest,PraxisMetadataCompletenessTest" test
```

Scorecard executado com a fonte canônica explicitada (o POM público não contém
as classes ainda em desenvolvimento da frente duplicate-draft):

```powershell
python scripts/workspace/audit-java-resource-completeness.py praxis-ergon-showcase --metadata-sources praxis-metadata-starter/src/main/java --strict
```

### Scorecard estático

| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | `MicroViz` | `UiAnalytics` | `ReactiveDet` | BulkEditable | BulkOps | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| `administracao-pessoal.cargos` | `Cargo` | 17 | 22 | 100.0% | 17/17 | 16 | 8 ops | 4 | 5 | YES | 5 (5) | 3 | 2 | 0 | 2 | 1 | 2 | 1 | 5 | 2 | 0 | **PASS** |
| `administracao-pessoal.documentos-legais` | `DocumentoLegal` | 13 | 13 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 2 (2) | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
| `administracao-pessoal.empresas` | `Orgao` | 12 | 12 | 100.0% | 12/12 | 11 | 7 ops | 2 | 5 | YES | 1 (1) | 2 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
| `administracao-pessoal.funcionarios` | `ServidorPessoa` | 68 | 71 | 100.0% | 68/68 | 25 | 9 ops | 2 | 5 | YES | 2 (2) | 3 | 0 | 1 | 2 | 1 | 2 | 1 | 7 | 2 | 0 | **PASS** |
| `administracao-pessoal.setores` | `Setor` | 12 | 12 | 100.0% | N/A (read-only) | 7 | 5 ops | 2 | 5 | YES | 4 (4) | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **PASS** |
| `administracao-pessoal.vinculos` | `Vinculo` | 59 | 74 | 100.0% | 59/59 | 33 | 12 ops | 6 | 6 | YES | 13 (13) | 3 | 9 | 2 | 4 | 1 | 2 | 1 | 10 | 4 | 0 | **PASS** |

`RichEntityLookup`: seis descritores ricos verificados no teste nativo.
Invariantes/determinações transacionais não foram reexecutadas; as contagens acima
são evidência estática. O gate global não foi aprovado.

Logs, inventário de substituições e auditorias de instalação estão em
`../output/api-paths-20261028/heranca/` a partir da raiz do Showcase.
