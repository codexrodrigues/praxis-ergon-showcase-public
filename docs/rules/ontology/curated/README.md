# Curadoria G1 — chefias, sem persistência

04/11/2026. **Contrato candidato local; G1 global aberto.** Esta unidade cobre funções, vagas de função, designações e substituições. Nenhum provider, RuleSet ativo, annotation pública, migration, banco, snapshot ou head foi modificado.

## Artefatos e alcance

| Família | Schemas de avaliação | Incluídos |
| --- | ---: | --- |
| Funções de confiança | 9 | CREATE, UPDATE, quatro endpoints de lote por item, DELETE unitário e em lote, preview ERG-01101 |
| Vagas de função | 9 | Mesmos oito escopos de manutenção, preview ERG-01154 |
| Designações | 10 | Oito escopos de manutenção e avaliação/confirmação da homologação coletiva |
| Substituições | 11 | Oito escopos de manutenção e previews ERG-01300, ERG-01315 e ERG-01319 |
| **Total** | **39** | **34 escopos de manutenção + 5 escopos de preview** |

Os cinco previews governados pertencem a três endpoints HTTP. A prévia de designação é uma consulta da situação da vaga, sem avaliação de RuleSet; consta como `LOOKUP_PROJECTION_ONLY`. Há 37 identidades HTTP associadas aos 39 schemas; a prévia de designação é a 38ª identidade, excluída do catálogo de avaliações. Não confundir contagem de schemas, endpoints, regras e nós de determinação.

O [manifesto](manifest.json) vincula os arquivos e fixa SHA-256 de 30 fontes locais. Cada família possui:

- `*.fact-catalog.candidate.json`: operações, schemas completos e descritores do perfil candidato v2 do Config;
- `*.operation-bindings.candidate.json`: endpoint, modo, unidade de avaliação, seleção de regras, bindings de conceitos e escopo de cada coleção;
- `*.fixtures.json`: uma árvore positiva por operação, cuja afirmação é **validade estrutural**, não autorização de negócio.

O [mapa de migração](path-migration.candidate.json) dá destino explícito aos **55 paths** lidos pelas expressões de produto e customizações de fábrica do órgão 2 nessas quatro famílias. Os [19 casos semânticos](semantic-fixtures.json) executam distinções selecionadas no dialeto canônico. Não representam paridade integral das 30 regras de produto nem das customizações persistidas.

Os textos e escolhas de campos foram curados explicitamente; a serialização das fixtures não preenche annotations a partir de rótulos ou nomes. Os JSON são os artefatos candidatos revisáveis. Scripts de investigação/reprodução ficam na pasta do blueprint.

## Decisões de contrato

1. **OLD por operação.** CREATE exige `current: null`. UPDATE, cada item do lote e homologação recebem uma cópia destacada do persistido, com identidade obrigatória. DELETE exige `draft: null` e OLD completo. O cadastro mantém todas as propriedades de negócio escalares/embutidas contabilizadas; `version`, `auditoria` e `customFields` têm exclusões explícitas. Isso não transforma o contrato de fatos em contrato de DTO nem certifica os 20 pilares dos recursos.
2. **Nulabilidade não é ausência.** As propriedades declaradas devem existir mesmo quando seu valor é null. Identidades persistidas e componentes de vínculo são obrigatórios. Referência opcional ausente pode resultar em conceito null; FK fornecida sem destino, tenant divergente ou falha de consulta aborta a avaliação. A validação estrutural não comprova que o registro existe.
3. **Papéis e chaves compostas.** `numfunc` e `numero/numvinc` permanecem na referência. ERG-01301 e as regras de indicação de substitutos comparam a pessoa, não a igualdade do vínculo inteiro. O legado confirma a comparação de `numfunc` em `pck_substituicoes_ev.pkb:162` e as referências compostas em `pck_designacoes_ev.pkb`. Não reduzir a identidade para preservar a comparação de negócio.
4. **Travessia real da função.** Em substituições, `funcao` é resolvida a partir de `designacao.funcaoId`. O binding declara as duas arestas e o mesmo órgão; não inventa FK direta no rascunho.
5. **Duas referências de impedimento.** Substituir a união implícita `impedimento` por `feriasImpedimento` e `afastamentoImpedimento`, cada qual com recurso, FK e vínculo titular explícitos. Se ambas forem informadas, ambas são representadas: a regra rejeita a combinação. Preservar `dtfim`, `dtprevfim` e `dtInterrupcao`; a escolha do fim e a elegibilidade do estado ficam na expressão. Isso substitui a proposta anterior de binding discriminado e não cria aliases permanentes.
6. **Coleções com escopo verificável.** Os bindings de `neighborhood` especificam chaves de origem/destino, tenant e exclusão por identidade. Consultas usam relação/identidade/tenant; status, cobertura e sobreposição ficam nas regras. A ausência de FK opcional ou de identidade ainda não persistida pode produzir coleção vazia; indisponibilidade de consulta não pode. Cap de 10.000 itens com rejeição do excesso, também sujeito ao limite global de bytes/nós do Engine: jamais truncar e interpretar o resultado como completo.
7. **Escopo local de iteradores.** `reduce` transporta início/fim no acumulador e lê `current` como item local. A fixture demonstra sobreposição inclusiva e preserva ocorrência cancelada para a regra decidir. Não criar acesso implícito ao `draft` externo dentro de `filter/none`, nem operador ad hoc para contornar o escopo.
8. **Datas e unidade.** Datas civis usam ISO `YYYY-MM-DD`. O operador canônico `date` retorna milissegundos; duração inclusiva em dias usa `((date(fim) - date(inicio)) / 86400000) + 1`. Testados dia único, ano bissexto e fronteira de horário de verão. O operador `-` não muda.
9. **Prévia tem contrato próprio.** Cada preview seleciona o slot composto ativo, preservando customização, com chave `preview-<bindingKey de produto>`. Um replacement que pede fatos indisponíveis nesse escopo deve falhar na admissão da candidata; não fabricar valores, usar fallback de produto nem ampliar silenciosamente o preview.
10. **Aplicabilidade da regra.** `CERG-07003` governa homologação coletiva. A introdução de OLD completo no UPDATE comum não pode passar a exigir estado anterior `DESIGNADA` em toda retificação. `current.criacao` vira `context.lifecycle`; CREATE não dereferencia filhos de OLD inexistente. A migração das expressões deve usar objetos/presença explícitos e acesso null-safe, com paths requeridos compatíveis com o schema da operação.
11. **DELETE dedicado.** As guardas de dependências/estado hoje nos serviços têm destino no escopo DELETE. Não executar a família de CREATE sobre `draft=null`. A seleção dos códigos/slots de exclusão precisa ser materializada no catálogo de erros na implementação, preservando as guardas estruturais e os efeitos posteriores dos serviços.

Os containers com dados pessoais/sensíveis adotam proteção conservadora. Essa classificação candidata não substitui a política canônica de autorização/redaction nem aprova publicação de valores no catálogo de IA.

## Operações e resolução canônica

CREATE e UPDATE usam os IDs explícitos dos controllers. As quatro operações de lote distinguem avaliação/confirmação e atualização uniforme/por item. O snapshot é de **um registro candidato após hidratação**, não do envelope do lote.

DELETE unitário e `DELETE /batch` são herdados. O manifesto usa identidade método/caminho, com `operationId: null`, até a resolução canônica em G2. O template Spring `/{id:^(?!batch$).+}` é normalizado para o caminho público `/{id}` na verificação; nenhuma URL ou porta de ambiente foi adicionada a bibliotecas.

O [teste do host](../../../../src/test/java/br/com/praxis/ergonshowcase/OntologyCuratedLeadershipContractTest.java) confere as annotations reais dos métodos herdados e declarados, os slots dos previews, a contabilidade das propriedades das entidades, os paths das relações/coleções, a coerência de identidade das fixtures e a cobertura do mapa de migração.

Endpoints de consulta, preparo de duplicate-draft e consulta/cancelamento de propostas não são avaliações da família de manutenção. O preparo de rascunho não substitui o futuro CREATE. A descoberta de determinações continua pertencendo ao `ReactiveDeterminationMetadataCompiler`; estes bindings de avaliação não substituem os targets `api_operation/form_config` nem comprovam a publicação HTTP do grafo.

## Gates e sequência de implementação

| Gate | Condição | Situação desta unidade |
| --- | --- | --- |
| Curadoria local | Destino dos paths de produto/fábrica; NEW/OLD e referências por operação | Materializado para as quatro famílias, 55 paths |
| Contrato estrutural | Perfil v2 único, árvores positivas, rejeição de ausência/tipo/data/operação/chave parcial | Testes locais |
| Binding | Endpoint real, concepts e collections com FK/tenant; preview no slot correto | Testes locais; operação herdada ainda requer resolver de G2 |
| G1 global | Restantes 14 famílias, 38 definições operacionais, formatos/limites, consumidores e histórico operacional reconciliados | **Aberto** |
| G2 | Publicar o contrato Metadata e os bindings via resolvers/compilador; goldens HTTP e precedência de autoridade | Não iniciado |
| G3 | Migrar providers, OLD, expressões, templates, parâmetros e DELETE de forma atômica; paridade Java/TypeScript e todos os cenários de produto/órgão | Não iniciado |
| G4 | Admissão tipada no Config/Engine, Studio, snapshots históricos, reparos e consumidores | Não iniciado |
| G5 | Branch Neon de teste, gates integrados, corte coordenado, rollback e limpeza conforme fluxo do projeto | Não iniciado |

Antes de considerar estas famílias executáveis, provar: isolamento/identidade em consultas; falha de fonte e excesso de coleção; distinção de pessoa/vínculo; exclusividade/cobertura de impedimentos; OLD em todos os call sites; aplicabilidade de homologação/DELETE; templates/overrides e admissibilidade em previews; mesmos resultados nos cenários válidos de referência. Os 19 exemplos semânticos não substituem essa matriz.

O ambiente tem orientação própria no [AGENTS do host](../../../../AGENTS.md) e na seção 5.3 do blueprint, atualizados pelo trabalho de ambiente. Esta unidade não consultou banco nem revalidou aquele inventário. As definições persistidas de prova devem ser reconciliadas separadamente com os novos schemas; evidência local de fábrica não as substitui.

## Verificação reproduzível

JDK 21, Maven 3.9.6, sem contexto Spring ou conexão de persistência.

No Config:

```powershell
mvn -B "-Dtest=DomainRuleFactCatalogCandidateContractTest,DomainRuleCuratedHostCandidateContractTest,DomainCatalogContractSynchronizationTest" "-Dontology.g1.host-contracts=D:\Developer\praxis-plataform\praxis-ergon-showcase\docs\rules\ontology\curated" test
```

No host:

```powershell
mvn -B "-Dmaven.compiler.fork=true" "-Dmaven.compiler.executable=D:\Developer\JAVA\graalvm-jdk-21.0.2+13.1\bin\javac.exe" "-Dtest=OntologyCuratedLeadershipContractTest" test
```

Resultado desta execução: **101 testes no Config e 65 no host, zero falhas/erros/ignorados**. O teste cruzado do Config é opt-in pelo caminho explícito; quando essa propriedade não é passada, o teste é ignorado e não constitui evidência deste gate.

**Limite do build:** a repetição com o compilador Maven no mesmo processo falhou em `Getter/Setter.onMethod_` e acessores Lombok de arquivos fora deste lote. Forçar `proc=full` não resolveu; um probe Java isolado com o mesmo JDK/Lombok passou. A compilação completa do host e os 65 testes passaram usando `javac` em processo separado, conforme comando acima. A causa da diferença entre os modos não foi determinada; o build padrão não está certificado. Não houve pin de dependência nem alteração de POM para contornar a falha.

Os logs `ontology-g1-curated-config-tests.log` e `ontology-g1-curated-host-fork-tests.log` registram os gates aprovados. `ontology-g1-curated-host-tests.log` e `ontology-g1-curated-host-proc-full-tests.log` conservam as falhas de compilação. Ficam junto ao blueprint. A validação usa dependências locais do checkout/Maven; não comprova instalação de versão pública. Não foram executadas suíte integral, publicação HTTP, validação de banco ou promoção. Não somar estes resultados com execuções anteriores.

O [recibo de verificação](verification.json) fixa hashes dos artefatos deste lote, verifica fontes e links e registra separadamente os resultados do Config, do host com compilador separado e a limitação do build padrão.

