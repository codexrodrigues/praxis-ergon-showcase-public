# Slice 7 · Benchmark de redução de código

Medição parcial de 3 de novembro de 2026 sobre a árvore local do `praxis-ergon-showcase`, com base Git `f6bfa42e9d30e90fd892892691c11d17e361ee0c`. Os três slices foram medidos na mesma árvore. Assim, os números do Slice 5 incluem refatorações presentes no checkout e representam o código encontrado na medição. Os valores históricos do plano são uma referência separada.

## Telemetria comparativa

| Slice | Recurso | Controller LOC | Service LOC | Registro bulk LOC | RuleSet LOC |
| --- | --- | ---: | ---: | ---: | ---: |
| 5 | ParametroContagemFerias | 513 | 422 | 3 | 1.152 |
| 5 | PeriodoAquisitivoFerias | 685 | 753 | 3 | 958 |
| 5 | FeriasGozo | 571 | 720 | 3 | 1.094 |
| 5 | RadarFeriasPrescricao | 302 | 522 | 0 | — |
| 6 | AverbacaoTempoServico | 253 | 188 | 3 | — |
| 6 | PeriodoAquisitivoLicencaEspecial | 300 | 196 | 3 | — |
| 6 | LicencaEspecialGozo | 265 | 214 | 3 | — |
| 6 | VendaLicencaEspecial | 232 | 290 | 3 | — |
| 6 | RadarTempoServicoLicencaEspecial | 39 | 72 | 0 | — |
| 7 | FuncaoConfianca | 47 | 36 | 0 | 47 |
| 7 | VagaFuncao | 47 | 39 | 0 | 53 |
| 7 | DesignacaoFuncao | 102 | 160 | 9 | 108 |
| 7 | SubstituicaoFuncao | 47 | 51 | 0 | 136 |
| 7 | RadarChefiaSubstituicao | 47 | 31 | 0 | — |

LOC são linhas físicas de Java, incluindo imports, comentários e linhas em branco, contadas com `Path.read_text(encoding="utf-8-sig").splitlines()`. Controller, Service e RuleSet são os arquivos homônimos em seus respectivos pacotes. Registro bulk conta as linhas que declaram providers por chave de recurso em `ErgonBulkConfiguration.java`. O travessão indica ausência de RuleSet próprio. As colunas excluem a infraestrutura compartilhada e os testes.

O Slice 6 tem 324 LOC em `TempoLicencaValidation` e 161 em `TempoLicencaLifecycle`, que também contém efeitos transacionais. O Slice 7 tem 47 LOC compartilhadas em `ChefiaResourceService`, 125 em `ChefiaRuleSetSupport` (com derivação automática de raízes ontológicas `availableRoots` e `requiredFactPaths` por AST JSON Logic) e 104 em `SubstituicaoFuncaoFacts`. A etapa adicionou decisões à substituição e removeu **zero** linhas de validação procedural existente.

## Estado do gate de produtividade

O radar do Slice 7 tem 47 LOC de controller (meta de até 45) e 31 de service (meta de até 75). Seus equivalentes medidos no Slice 5 têm 302 e 522 LOC. A redução bruta é de 84,4% no controller e 94,1% no service. A comparação registra tamanho físico e depende de equivalência funcional para servir como evidência de produtividade.

Designação usa a base bulk e os três providers explícitos para edição uniforme, edição por item e homologação coletiva. Os demais controllers mutáveis do Slice 7 ainda usam a base CRUD da Fase 1. A meta de registro programático dos providers e o gate de produtividade permanecem abertos.

A auditoria estrita passou a reconhecer as cinco chaves do Slice 7 declaradas em `ApiPaths`. O resultado atual é 26 recursos aprovados de 30. Restam decisões de duplicação assistida, capacidades de descoberta e limitações do auditor para handlers bulk herdados e campos derivados.

## Recorte da migração de substituições

Os bindings migrados registram a origem em `pck_substituicoes_ev.pkb`. Quando se solicita remuneração, `CERG-07001` exige que os dias corridos de exercício, incluindo as duas pontas do intervalo, alcancem o maior valor entre o mínimo cadastrado na função e o piso institucional. O template `ergon:substituicoes.diasMinimosRemunerados` usa 15 dias no produto e 10 no Órgão 2. As consultas usam o órgão da substituição e a chave do vínculo correspondente. O teste Hibernate/H2 comprova que a consulta desconsidera afastamento de outro órgão e férias após a interrupção. A decisão de impedimento usa um registro explícito que cubra todo o intervalo. O legado também considera combinações de eventos e outros tipos de impedimento. O modelo atual liga a substituição a uma designação por ID e usa a data de exercício como início de vigência funcional. A equivalência completa com as rotinas Oracle e a concorrência entre gravações ainda exigem prova.

## Indicação de substitutos na designação

`DesignacaoFuncaoRuleSet` aplica `ERG-01203`, `ERG-01204` e `ERG-01205` na criação e na atualização. A origem é `pck_designacoes_ev.pkb`, rotina `VALID_DESIGNACOES`, linhas 287 a 303. Os fatos usam a identidade da pessoa para preservar a rejeição mesmo quando os vínculos têm números diferentes.

## Homologação coletiva e ocupação

A avaliação em `POST /api/ergon/designacoes/bulk/homologar-designacoes-lote/evaluation` recebe data de exercício, alvos explícitos e suas versões. A confirmação em `POST /api/ergon/designacoes/actions/homologar-designacoes-lote` recebe a proposta retida e a chave de idempotência. Cada item aprovado passa pela revalidação de versão e pelas regras da vaga antes de mudar de `DESIGNADA` para `EM_EXERCICIO`.

A ocupação acompanha o estado de exercício. A escrita bloqueia as vagas do mesmo órgão em ordem de ID e atualiza a titularidade na transação da designação. Ao sair da vaga, o ato libera a posição anterior se não houver outro titular. A exclusão rejeita designações com substituições vinculadas. A coluna de titular atual é derivada e somente leitura no contrato da vaga.

A prova local combina o contrato de avaliação e confirmação em MockMvc com o motor bulk simulado e o adapter real em Hibernate/H2. O teste transacional verifica avaliação sem mutação, commit, rollback, rejeição da versão obsoleta e liberação da vaga ao desvincular ou excluir o ato. A publicação e o replay do recibo durável ainda exigem a prova em PostgreSQL com o ambiente descartável configurado.

Em 03/11/2026, a prova HTTP usou a branch Neon `br-floral-bird-b7liwyv1` (PostgreSQL 17.11), derivada da base de provas `br-super-sound-b7urizmd` e com expiração em 04/11/2026 às 08:27, horário de São Paulo. A base herdada tinha o ledger bulk, mas ainda não tinha o esquema do Slice 5 em diante. Os predecessores foram preparados com os mapeamentos Hibernate. As migrações V5 a V9 foram aplicadas na cópia, com V5 tratando a coluna de política já criada pelo Hibernate como existente. O migrador do starter provisionou as três identidades bulk de designações.

O teste `BulkNeonIntegrationTest#homologatesDesignacaoAndReplaysWithoutReapplyingOnNeon` iniciou o host com os papéis de runtime e controle e chamou o endpoint administrativo real. A tentativa fria recebeu HTTP 503 durante a geração do OpenAPI. A tentativa pelo endpoint pooled alcançou a composição e revelou que o identificador do path estava incorretamente listado como campo protegido do corpo do PUT. O contrato foi corrigido nos dois pontos. A publicação, com a correção, retornou HTTP 503 `TRANSACTION_TIMEOUT`. A leitura direta confirmou `UNCOMPOSED`, geração `0` e zero fixtures de designação da prova. A avaliação, a confirmação e o replay seguem pendentes de execução no Neon.
