# P5-B — Integração da Onda 4

Data: 2026-11-02. Classificação: `contrato-publico`, com complementos transversais
no engine e no metadata starter. Este relatório registra o recorte local do
showcase; não substitui revisão independente nem aceite integrado.

**Fechamento anterior à P5-C: B01–B08 da revisão original corrigidos.** O
[registro das correções](P5-B-CORRECOES-2026-11-02.md) contém as decisões,
regressões e identidade do candidato final. Esse resultado não substitui revisão
independente nem aceite integrado.

**Resultado histórico da revisão ampliada: CHANGES REQUESTED.** O
parecer de 2026-11-02 reproduziu oito achados
adicionais (três P1 e cinco P2) no candidato dos 385 testes. Os resultados e
hashes abaixo preservam aquele histórico; os gates do candidato corrigido
estão no registro de correções acima.

## P5-C — Escopo residual

As regressões do parecer anterior permanecem em `P5BWriteAndScopeRegressionTest`,
`P5BSalePreviewRegressionTest`, `P5BPreviewTransportTest`,
`CustomFieldDomainMutationRegressionTest` e `ResourceStateCopySupportTest`.
No corte P5-C:

- As consultas de fatos de vínculo, cargo, frequência, afastamento, férias e
  tempo de serviço/licença usam o órgão da operação. Referências de outro órgão
  são rejeitadas antes de entrar no contexto de regras. O cadastro civil e os
  catálogos gerais continuam globais.
- Para dependentes, acesso por item, leitura em lote e seleção usam vínculo
  ativo no órgão corrente (`ATIVO`, `AFASTADO` ou `CEDIDO`).
- Os resultados das prévias de férias são `READ_ONLY` no Jackson; a projeção
  reativa usa `dataPrescricaoPrevista` e preserva `dtPrescricao` para o registro
  oficial. O teste de transporte exercita a determinação e o POST/PUT dos
  controllers de período e gozo.
- O adapter de erro direto e o handler REST compartilham o status do motor.
  Documento Legal publica a dependência `empCodigo`; Tabela é global e não
  publica um filtro que o backend não suporta.
- O [comprovante de duplicação assistida](P5-C-DUPLICATE-DRAFT.md) identifica
  a divergência entre a decisão histórica e as quatro implementações da
  Issue #10. Os endpoints atuais e a prova focal de Cargo são preservados.

**Gate local do Showcase: PASS.** O comando `mvn test "-Dtest=!*Neon*"`
executou 408 testes, sem falhas, erros ou skips, contra o starter
`8.0.0-rc.188` instalado localmente. O
[scorecard completo](evidence/P5-C-RESOURCE-COMPLETENESS.md) registra
**25/25 STATIC PASS** no auditor estrito, com zero problemas editoriais.
O scorecard não compara a existência dos endpoints de duplicação com o status
de adoção; a reconciliação do inventário (B07) continua pendente.

## Validação do recorte após a revisão

A criação de vínculo rejeita órgão divergente antes de validar campos
customizados, mapear a entrada ou consultar fatos. A validação dos campos
customizados em comandos versionados usa o mapa persistido quando a entidade
o declara e preserva a projeção como contexto do rascunho. O preparo de venda
avulsa usa a mesma política no snapshot e no serviço, com regressão para abono
vinculado e venda avulsa.

A rodada compartilhada executou 410 testes e registrou nove erros
`NoClassDefFoundError` de `TenantRequestValidation` durante a recompilação
concorrente de `target/classes`. A repetição em diretório de build isolado
passou em 58 testes, incluindo os nove casos do interceptor, as regressões
novas e os testes de transporte de prévias. A prova de prévias cobre HTTP,
binding e mapeamento naquele recorte. Os mapeadores ainda aceitam
campos oficiais informados pelos DTOs, como `dtPrescricao` e `totDiasDireito`.
O teste do período inclusive envia uma data oficial distinta da prévia e
confirma que ela chega à entidade mapeada. O legado usa a prescrição gravada
em `PCK_PERAQFER` para validar os gozos. Para o aceite do B03, distinguir no
contrato quais valores o servidor calcula e quais admitem retificação formal,
e provar o estado gravado após POST e PUT.

A suíte não-Neon completa no build isolado executou 410 testes com três
falhas e zero erros. Os testes do registro de lookups e da descoberta
encontraram 27 fontes e 30 controllers onde esperavam 23 e 25.
A revisão de DTOs encontrou referências a `id` ausente em DTOs de escrita
e campos de Quick View repetidos na identidade dos novos recursos de chefia.
O resultado está em `target/p5c-followup-isolated-full.log`.

O auditor estrito voltou a registrar 25/25 no recorte P5-C com os fontes do
starter recompilado. A rodada posterior com o inventário candidato encontrou
cinco controllers adicionais de funções de confiança e substituição na árvore
compartilhada. O log `target/p5c-followup-inventory-candidate.log` contém dez
falhas estruturais dessa frente. O 25/25 anterior se aplica à árvore de 25
recursos identificada no [scorecard](evidence/P5-C-RESOURCE-COMPLETENESS.md).
O inventário oficial mais recente passou a listar 30 recursos. O auditor
registrou 20 apontamentos para os cinco novos recursos, incluindo decisões
de adoção pendentes, chaves que o auditor exige como literais e lacunas de
completude. O log é `target/p5c-followup-current-audit.log`. O candidato
`target/p5c-inventory/praxis-ergon-showcase.json` parte desse inventário e
altera apenas as quatro entradas da P5-C, com os hashes dos recibos verificados.
A rodada com o candidato retornou os mesmos 20 apontamentos, todos ligados
aos cinco recursos novos. O recibo autorizado e as quatro decisões foram incorporados ao inventário oficial.

## Prova de persistência B03

`P5CVinculoFeriasPostgresTest` usa PostgreSQL local descartável, o esquema
gerado pelas entidades JPA, os repositórios Spring Data, os serviços,
o motor de regras e os controllers de período aquisitivo e gozo. O teste
submete a determinação, envia a prévia no POST, e repete no PUT com
valores de prévia adulterados. Cada gravação é seguida de uma leitura
em outra transação. O contrato mantém a separação entre prévia e os
campos oficiais que o domínio valida, como a data de prescrição e os
dias solicitados de gozo.

No legado, `PCK_PERAQFER` usa a data de prescrição gravada nas validações
de gozo e consulta o parâmetro aplicável. As férias estatutárias federais
têm regime próprio na Lei 8.112/1990, arts. 77 a 80. A motivação formal
de atos que afetem direitos na Administração Pública federal segue
a Lei 9.784/1999, art. 50. O teste prova o contrato de gravação local.
A revisão da autoridade para retificações deve considerar a norma
do ente e o ato administrativo aplicável.
[Lei 8.112/1990](https://www.planalto.gov.br/ccivil_03/leis/l8112cons.htm?lang=pt) ·
[Lei 9.784/1999](https://www.planalto.gov.br/ccivil_03/leis/l9784.htm?origin=instituicao)

Na rodada focal, POST e PUT persistiram os valores oficiais do período
e da parcela. A releitura confirmou a nova data de prescrição em
29/02/2028, 11 dias de gozo e o avanço das versões. Os valores de
prévia enviados no PUT foram descartados. O total focal foi de 58
testes aprovados. O log é `target/p5c-closure-final.log`.

A suíte completa sem Neon executou 437 testes no build isolado, com
436 aprovações e um erro. O `ChefiaSlice7SqlTest` recebeu HTTP 403 da
guarda de órgão no preparo em lote de designações. A pilha passa por
`DesignacaoFuncaoService.previewHomologacao` e `ErgonTenantAccess`.
O log é `target/p5c-closure-nonneon.log`.

## Scorecard do recorte de fechamento

O auditor resolveu 30 controllers. As linhas abaixo cobrem os recursos afetados
pela ERG-00300, B03 e B07. O gate geral registrou nove apontamentos na frente
de chefia e substituição. Log completo em `target/p5c-closure-final-audit.log`.

| Resource (`resourceKey`) | Entity | BizCols | RespCols | Cov % | Accounted writes | FilterCols | FilterOps | `@QuickFilter` | QuickViewCols | RichIdentity | `@ResourceRef` (`preview`) | `@UiSurface` | `@WorkflowAction` | `@ResourceIntent` | `@ResourceFact` | `MicroViz` | `UiAnalytics` | `ReactiveDet` | BulkEditable | BulkOps | EditorialIssues | Status |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|:---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| `administracao-pessoal.averbacoes-tempo-servico` | `AverbacaoTempoServico` | 30 | 37 | 100.0% | 30/30 | 18 | 12 ops | 3 | 4 | YES | 11 (11) | 4 | 5 | 2 | 3 | 1 | 2 | 1 | 17 | 3 | 0 | **PASS** |
| `administracao-pessoal.ferias-gozo` | `FeriasGozo` | 31 | 44 | 100.0% | 31/31 | 21 | 9 ops | 5 | 6 | YES | 15 (15) | 3 | 6 | 2 | 3 | 1 | 5 | 1 | 21 | 3 | 0 | **PASS** |
| `administracao-pessoal.licencas-especiais-gozo` | `LicencaEspecialGozo` | 22 | 30 | 100.0% | 22/22 | 18 | 11 ops | 3 | 5 | YES | 13 (13) | 4 | 6 | 2 | 3 | 1 | 2 | 1 | 12 | 3 | 0 | **PASS** |
| `administracao-pessoal.periodos-aquisitivos-ferias` | `PeriodoAquisitivoFerias` | 43 | 53 | 100.0% | 43/43 | 21 | 9 ops | 5 | 6 | YES | 10 (10) | 4 | 8 | 2 | 4 | 1 | 5 | 1 | 31 | 3 | 0 | **PASS** |
| `administracao-pessoal.periodos-aquisitivos-licenca-especial` | `PeriodoAquisitivoLicencaEspecial` | 33 | 40 | 100.0% | 33/33 | 18 | 12 ops | 3 | 5 | YES | 12 (12) | 5 | 8 | 2 | 4 | 2 | 2 | 1 | 19 | 3 | 0 | **PASS** |
| `administracao-pessoal.vendas-licenca-especial` | `VendaLicencaEspecial` | 21 | 31 | 100.0% | 21/21 | 21 | 11 ops | 3 | 5 | YES | 14 (14) | 2 | 5 | 1 | 3 | 1 | 2 | 1 | 8 | 3 | 0 | **PASS** |
| `administracao-pessoal.vinculos` | `Vinculo` | 59 | 74 | 100.0% | 59/59 | 33 | 12 ops | 6 | 6 | YES | 14 (14) | 9 | 9 | 2 | 4 | 1 | 5 | 1 | 10 | 4 | 0 | **PASS** |

## Decisão de acumulação interórgãos

O legado v7x separa a validação de empresa (`PACK_HADES.VALIDA_EMPRESA`)
da verificação de concomitância (`PCK_VINCULOS.VALID_VINC_CONCOMIT`).
Esta última procura sobreposição pelo titular em toda a base quando o
parâmetro `VINC_CONCOMIT` está ativo. A consulta ordinária do Showcase
tem escopo de órgão, por isso um resultado vazio só evidencia ausência
de vínculos naquele órgão. A ERG-00300 agora tem uma consulta própria na base do Showcase.

A adoção corporativa requer definir quem está autorizado a aferir a
acumulação, quais vínculos integram o universo consultado e qual a finalidade
e a base jurídica da consulta. O contexto `X-Tenant` do Showcase seleciona
o órgão. A autenticação demonstrativa oferece papéis globais e carece de
vínculo verificado entre operador e órgão. A decisão interórgãos deve
consumir os fatos mínimos necessários em uma fronteira autorizada, retornar
somente a decisão e seu motivo seguro, e encaminhar dados indisponíveis
ou incompletos para análise sem aprovar a operação automaticamente.

O Showcase habilita a fonte local por lista de órgãos autorizados em
`SHOWCASE_ACCUMULATION_ORGS` e exige a autoridade autenticada
`ERGON_ACCUMULATION_CHECK`.
A consulta seleciona datas e categoria pelo titular. Exclui o próprio
vínculo e preserva as consultas ordinárias restritas ao órgão. A leitura
ocorre depois do bloqueio pessimista do titular, na transação da operação.
Com fonte desabilitada ou autorização ausente, a decisão fica inconclusiva.
O executor 2.1 considera a vigência da EC 138 pela data inicial da
sobreposição. Pares potencialmente elegíveis ainda exigem evidência de
compatibilidade dos horários. A base local oferece jornada semanal e
carece dessa evidência, então esses casos seguem inconclusivos.

A configuração demonstra a fronteira do Showcase. A adoção institucional
depende de autorização documentada, cobertura da fonte e prova confiável
da compatibilidade, conforme o âmbito do ente. A lista de órgãos habilita
uma finalidade na base local e deve ser preenchida pelo seu responsável.

A prova em PostgreSQL confirmou a leitura de dois vínculos do outro
órgão com a autorização específica, a ausência de fatos sem essa
autorização e a retenção do bloqueio do titular até o fim da transação.
Uma segunda conexão recebeu SQLSTATE `55P03` ao tentar adquirir o
mesmo bloqueio com `NOWAIT`. O teste do motor cobre a data imediatamente
anterior à vigência da EC 138, a data da publicação e a recusa do terceiro
vínculo concomitante. As fontes legais foram consultadas em 03/11/2026.

O critério de acumulação deve ser versionado por fundamento e vigência.
A EC 138/2025 atualizou a combinação envolvendo cargo de professor.
A aferição concreta de horários deve observar o Tema 1081 do STF no seu
âmbito de aplicação. O acesso aos fatos depende da finalidade e da
necessidade, conforme os arts. 6, 23 e 26 da LGPD.
[EC 138/2025](https://www2.camara.leg.br/legin/fed/emecon/2025/emendaconstitucional-138-19-dezembro-2025-798508-publicacaooriginal-177473-pl.html) ·
[Tema 1081 do STF](https://portal.stf.jus.br/jurisprudenciaRepercussao/verAndamentoProcesso.asp?classeProcesso=RE&incidente=5819218&numeroProcesso=1246685&numeroTema=1081) ·
[LGPD](https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html)

Para o aceite, demonstrar pelo fluxo real um terceiro vínculo concomitante
em outro órgão, datas sem sobreposição, compatibilidade de horários,
vigências distintas da regra, fonte indisponível e dois provimentos
concorrentes do mesmo titular. O parecer deve comprovar que o retorno e a
auditoria preservam a finalidade da consulta e o isolamento dos órgãos.

## Resultado inicial da reabertura da Slice 7

O 403 da designação vinha da cópia reflexiva de um proxy Hibernate.
A cópia lia os campos do proxy sem primeiro obter a entidade carregada.
Isso deixava o órgão ausente no candidato e acionava a guarda de isolamento.
O código atual resolve o proxy antes da cópia. A regressão usa
`Session.getReference` após limpar a sessão e mantém as verificações de
proposta sem gravação, confirmação e rollback.

O preparo assistido foi classificado como não aplicável para vagas numeradas
e para o radar de leitura. O recibo P5-C cobre as duas decisões. Funções,
designações e substituições continuam com decisão de adoção pendente na Fase 2.
Designações agora declara os pares de avaliação e confirmação para edições
uniformes e por registro. Funções e vagas protegem a exclusão com dependências,
e substituições preserva os atos que já integram o histórico.

O auditor ainda exige as superfícies de bulk, fatos, analytics e determinações
previstas para a Fase 2. Também sinaliza `VagaFuncao.titularAtualId`.
A coluna é derivada da designação em exercício e é atualizada pelo serviço
de designações. O auditor procura o comando no controller da própria vaga.
A cobertura desse efeito entre recursos segue pendente no auditor.

O gate focal passou com 42 testes. O auditor estrito caiu de nove para sete
apontamentos. São três decisões de adoção e quatro linhas de implementação
que ainda concentram as lacunas descritas acima. Todos os cinco recursos da
Slice 7 têm 100% de cobertura de colunas em resposta. A vaga tem 14 de 15
destinos de escrita reconhecidos pelo auditor. O radar passou no gate de
estrutura e teve a decisão de duplicação registrada. Logs em
`target/p5c-reopen-focal.log` e `target/p5c-slice7-audit.log`.

### Resultado anterior da P5-C

O recibo B07 registra a adoção local em funções, designações e
substituições. Cada rascunho oferece uma única sugestão de cadastro e
deixa os dados do novo ato para a inclusão explícita. O auditor atribui
`titularAtualId` aos hooks de criação, atualização e exclusão do serviço
de designações, que reconciliam a entidade gerenciada da vaga. O teste
`ChefiaSlice7SqlTest` cobre confirmação e rollback em PostgreSQL local.

Os quatro registros de recursos continuam com lacunas de implementação.
Designações precisa de fatos, visualização, analytics e determinações
reativas. Funções, substituições e vagas precisam de bulk governado,
fatos, analytics e determinações reativas. O recorte permanece
`CHANGES REQUESTED` até a implementação e a validação dessas superfícies.
O gate estrito passou em 26 dos 30 recursos. Vagas tem 15 de 15
escritas contabilizadas. A matriz completa está em
`target/p5c-slice7-reaudit.log`.

A suíte sem Neon foi reexecutada em uma cópia isolada dos fontes e
do POM. Dos 460 testes, 456 passaram. Dois casos de cadastro pessoal
divergiram nos códigos de regra esperados. Afastamento e frequência
tiveram um erro cada em consulta sem retorno configurado no mock de
`EntityManager`. A regressão de rascunhos, a reconciliação de titular
e a prova de persistência B03 passaram nessa execução.

### Fases de completude da Slice 7

1. Fatos e análises usam o filtro e o escopo do cadastro. As projeções contam
   funções por categoria, vagas por situação, designações por situação de
   exercício e substituições por impedimento. As quatro séries usam o início
   da vigência. O histograma de dias mínimos aplica-se às funções de confiança.
   A duração da designação conta as duas datas e fica vazia em vigência aberta.
2. Os lotes de funções, vagas e substituições usam avaliação e confirmação
   separadas, versões por registro e recibos do executor durável. A edição
   cobre denominações por item, anotações e documentos de fundamento. A
   classificação institucional, o símbolo e os requisitos também podem ser
   retificados em funções. Identidades, vigências, titularidade, publicação,
   impedimentos e efeitos remuneratórios ficam protegidos nesses lotes.
3. As prévias de create e update calculam a duração da função e da
   substituição e consultam o controle da função, a situação da vaga e o
   mínimo remunerado da função do titular. As consultas respeitam o órgão
   efetivo. A prévia de vaga não faz reserva e a gravação mantém sua
   validação transacional.

O auditor estrito aprovou 30 dos 30 recursos na cópia isolada em
`target/p5c-slice7-final/audit.log`. O compilador verificou 50 contratos de
lote e 18 determinações, cada uma nas operações de inclusão e edição.
O teste JPA confirmou a contagem de vagas do órgão efetivo e a recusa
de prévia de vaga de outro órgão.

A suíte isolada sem Neon aprovou 453 casos, incluindo os quatro que
falharam na execução anterior. A classe de migração de licenças parou no
bootstrap por exceder a espera de 10 segundos do PostgreSQL embutido.
Com a espera da fixture ampliada para dois minutos, os 17 casos dessa
classe passaram na mesma cópia isolada. Os 470 casos foram verificados
em duas execuções, registradas em `target/p5c-slice7-final/non-neon.log`
e `target/p5c-slice7-final/postgres-retry.log`.

## Prova técnica de horários da ERG-00300

No Oracle, `PCK_VINCULOS.VALID_VINC_CONCOMIT` procura outro vínculo
do mesmo titular com exercício e vacância concomitantes e emite o erro
300 quando encontra um (`pck_vinculos.pkb:568–592`). A chamada é
condicionada por `VINC_CONCOMIT` (`pck_vinculos.pkb:1815–1820`).
Para instruir a análise de compatibilidade no Showcase, a fonte legada
das janelas concretas de cada vínculo é a apuração de frequência.

O código do Ergon v7x confirma a resolução da escala por vínculo e data
civil. `PACK_APUR_FREQ_CACHE.GET_HORARIOS_DATA(NUMFUNC, NUMVINC, DATA)`
configura o cache para o vínculo e chama `MONTA_CACHE_HORARIOS_DATA`.
O montador consulta `EP__AF_MONTA_HORARIOS_DATA` antes da lógica padrão.
Uma implementação institucional desse ponto de extensão pode substituir
o cálculo.

| Passo | Linhagem em `docs-legado/v7x/aps/fontes_ergon/pack_apur_freq_cache.pkb` |
| --- | --- |
| Resolver a parametrização na data | `GET_PARAMETRIZACAO_BASE` e seleção do tipo em 1528–1547. A parametrização diária de horário especial (895–900) e a de escala alternada (973–1009) exigem o par funcional e a vigência. No fluxo padrão do cache, a escala alternada é aplicada depois do horário especial. |
| Resolver a escala alternada | `GET_HORARIOS_ALT` em 3306 e leitura de `ERG_ESCALA_ALTERNADA` por `NUMFUNC`, `NUMVINC` e interseção com o mês em 3356–3375. A geração aplica os limites da vigência e o ciclo de jornada e descanso. |
| Resolver o horário especial | `ERG_HORA_ESPECIAL.ID_HOR_ESP` → `ERG_HORA_ESP_DIA.ID_HOR_ESP` → `ERG_HORA_ESP_DIA_DET.ID_HOR_ESP_DIA`. A consulta mantém o par funcional e aplica a recorrência na data, em 1722–1744. |
| Resolver o padrão e o fallback | Quando cabível, `EVENTO_FUNC` é filtrado pelo par funcional, pelo órgão de `FLAG_PACK.GET_EMPRESA`, pelo cargo vigente e pela interseção com a data, em 1667–1678. O código de horário selecionado liga `HORARIO_TRAB.CODIGO` a `ERG_HORA_DIA_SEMANA.ID_HORARIO` e então a `ERG_HORA_DIA_SEM_DET.ID_HORA_DIA_SEMANA`, em 1813–1836. O resultado contém janelas de entrada e saída. |

Os parâmetros de base e categoria vêm de `ERG_PARAM_APURACAO`, com
recorte de vigência. A categoria parte do par funcional e da data inicial
da janela de montagem do cache (698–712 e 841–850). Ao atravessar uma
mudança de categoria, a coleta deve abrir uma nova janela de cache.

O cache do pacote não tem órgão na chave de reconfiguração (2168–2185).
Na coleta interórgãos, cada vínculo deve ser resolvido em contexto de
empresa isolado ou com reinicialização comprovada do cache, mantendo a
interpretação do dia da semana do Oracle e o fuso do local de trabalho.
A fonte precisa distinguir folga comprovada de janela desconhecida, levar
em conta turnos que atravessam a meia-noite e declarar a cobertura e a
proveniência das janelas. A confirmação acima é do fonte versionado do
legado. A parametrização efetivamente instalada e o ponto de extensão de
cada órgão ainda precisam de validação institucional.

`VinculoScheduleEvidenceSource` é a porta do host para uma fonte de
escala efetiva. A consulta de fatos verifica a autoridade de acumulação,
o escopo dos dois órgãos e a interseção das vigências antes de chamar a
fonte com as duas chaves funcionais. A fixture PostgreSQL usa uma tabela
temporária de escalas resolvidas e contém linhas de controle para outro
órgão e outra data. Sem uma fonte instalada, o resultado para um par
elegível sem horários continua inconclusivo.

A regra aceita fatos internos de horários concretos com fuso, cobertura e
intervalo mínimo de deslocamento do caso. Sobreposição ou deslocamento
insuficiente bloqueia o par. A aprovação técnica exige cobertura integral
de um período delimitado. A prova focal usa dados sintéticos e cobre
compatibilidade, conflito, deslocamento insuficiente e evidência parcial.
A consulta atual do Showcase fornece os fatos de vínculos sem a escala.
A avaliação institucional continua a depender da fonte autorizada, da
documentação dos órgãos envolvidos e do exame de fatores aplicáveis.
O Tema 1081 fixa a análise concreta. A orientação do SIPEC ilustra a
consideração de deslocamento, descanso e alimentação no seu âmbito.
[Tema 1081 do STF](https://portal.stf.jus.br/jurisprudenciaRepercussao/verAndamentoProcesso.asp?classeProcesso=RE&incidente=5819218&numeroProcesso=1246685&numeroTema=1081) ·
[Orientação SIPEC sobre acumulação](https://legis.sigepe.gov.br/sigepe-bgp-ws-legis/legis-service/download/?id=0025650314-ALPDF%2F2025)

## Contratos e impacto

- `ErgonRuleViolationException` estende a exceção canônica. POST/PUT e comandos
  PATCH/actions conservam razões, campos e proveniência pelo handler nativo.
  Os cinco códigos `ERG-08393`, `ERG-00424`, `ERG-04300`, `ERG-03193` e `ERG-00300`
  publicam categoria `CONFLICT`; rejeições mistas seguem o status do engine.
  Os métodos `getBody`, `getStatusCode` e `getReason` são apenas adaptadores.
- A admissão de JSON Logic usa `CustomFieldValidationRules.validate`, a porta
  pública do starter que delega a `CustomFieldJsonLogic.validateDefinition`.
  Não fornece raiz implícita, relógio ou timezone. As nove avaliações redundantes
  nos hooks de invariantes foram removidas. O handler conserva todos os erros
  e os targets `customFields.<key>`.
- As determinações das Slices 1–6 usam operações e DTOs reais. Resultados que
  dependem de campos também informáveis aparecem em campos de prévia somente
  leitura, evitando ciclos e a substituição de entradas. A Slice 6 publica
  CREATE e EDIT pelo builder canônico.
- `ErgonTenantAccess` reúne a política de órgão para as duas bases de serviço:
  `empCodigo`, `empresa`, registros globais declarados e dependentes cujo titular
  tem vínculo no órgão corrente. Filtros normais, locate, export e seleção bulk
  usam o mesmo órgão efetivo, inclusive o default sem header. CREATE de órgão e
  dependente também verifica escopo. Comandos autorizam antes de adquirir locks.
- As prévias de férias autorizam vínculo/período antes de usar seus dados e
  restringem frequência, afastamento e parcelas pelo órgão corrente. A leitura
  SQL de resultados bulk de dependentes exige vínculo do titular no órgão.
- Lookups usam `description` como ordenação padrão com opções correspondentes.
  As fontes com `empCodigo` preservam suas dependências e filtros em cascata;
  dependentes mantêm `numfunc` e o escopo obrigatório de órgão no servidor.
- Duplicate-draft de período, gozo e venda de licença especial conserva órgão
  e quantidades revisadas, sem copiar identidade, titular, datas ou atos. O
  atributo `abonoConstitucional` citado na Issue pertence a férias e não existe
  no contrato da licença especial; não foi criado nem equiparado ao abono
  pecuniário. Após a investigação descrita abaixo, o gozo passa a conservar a
  proposta de abono da licença pelo conjunto `abonoPecuniario`, `diasAbono` e
  `iniFimAbono`, sujeito à revisão e à política do período escolhido no create.
- Os quatro patches da Slice 6 usam `ResourceStateCopySupport`; venda usa
  `executeVersionedMutation`, preservando a ordem de locks do abono vinculado.
  Os métodos mortos `mapper`, `wire`, `validateEntity` e injeções sem uso foram
  removidos de `ErgonBulkDomain`.

O engine precisou permitir extensão da exceção e um grupo de violações diretas
imutável. O starter precisou expor o helper de patch e validar custom fields na
projeção resultante de comandos versionados contra o estado anterior autorizado.
São complementos locais às dependências pré-release `.17`/`.188`, sem publicação.

Semântica de negócio consultada para as prévias: `docs-legado/v7x/aps/peraqfer.tab`
(DTFIM, TOTDIAS, FALTAS e prescrição) e `docs-legado/v7x/aps/FERIAS.tab`
(início, término, direito e abono), sob `D:/Developer/Techne/ErgonX/migracao`.

## Decisão investigada: abono no duplicate-draft da licença especial

A resposta posterior do solicitante delegou a investigação e a decisão desta
divergência. A referência a `abonoConstitucional` em M-05 é uma inconsistência de
nomenclatura da Issue/auditoria, não evidência de um novo atributo da licença.

| Fonte primária consultada | Semântica comprovada |
| --- | --- |
| [FERIAS.tab:132](D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/FERIAS.tab:132) e [linha 299](D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/FERIAS.tab:299) | `ABONO` identifica pedido de abono pecuniário; `ABONO_CONST` é outro campo, referente ao pagamento do abono constitucional |
| [PCK_LIC_ESP:262](D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/pck_lic_esp.pkb:262), [linha 305](D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/pck_lic_esp.pkb:305) e [linha 368](D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/pck_lic_esp.pkb:368) | O abono da licença é conversão de dias; a política decide admissão e proporção sobre direito (`D`) ou gozo (`G`), com reflexo em `ERG_VENDA_LICESP` |
| [ERG_FNC_LICESP_TEM_ABONO:69](D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/fontes_ergon/erg_fnc_licesp_tem_abono.fnc:69) | A possibilidade de abono depende dos parâmetros efetivos do vínculo/período |
| [FeriasGozoDados:291](D:/Developer/praxis-plataform/praxis-ergon-showcase/src/main/java/br/com/praxis/ergonshowcase/dto/FeriasGozoDados.java:291) e [LicencaEspecialGozoDados:57](D:/Developer/praxis-plataform/praxis-ergon-showcase/src/main/java/br/com/praxis/ergonshowcase/dto/LicencaEspecialGozoDados.java:57) | O host mantém os conceitos separados: adicional constitucional de férias versus abono pecuniário da licença |

`LIC_ESP.tab` também foi consultado, mas o arquivo disponível é um conjunto de
alterações incrementais; ele não permite concluir, isoladamente, a ausência de
uma coluna. A decisão está apoiada nos packages executáveis, nas descrições de
`FERIAS` e no contrato real do host, e não numa busca textual sem resultados.

**Política adotada:** conservar no rascunho a intenção já expressa na origem,
usando os nomes canônicos da licença e copiando os três campos em conjunto.
Copiar somente `abonoPecuniario=true` deixaria os defaults `diasAbono=0` e
`iniFimAbono=null`, contradizendo a estrutura exigida pela escrita. Redefinir
sempre para `false` descartaria silenciosamente a intenção do usuário ao preparar
um modelo. O conjunto copiado é uma sugestão revisável, não uma autorização,
um cálculo válido para qualquer período ou uma liquidação.

O novo rascunho continua sem período, titular/vínculo, datas, sequência da
parcela, documentos e referência de folha da origem; inicia `PROGRAMADA` e não
cria venda/abono. O create final exige completar o contexto e revalida a
permissão de abono, proporção, saldo e demais invariantes no servidor. Se o novo
período tiver outra política, os dias sugeridos devem ser revistos; o servidor
rejeita a proposta incompatível. Uma origem sem abono conserva `false`, zero e
posição nula. Não foi criado alias, novo campo, migração ou equivalência com
`abonoConstitucional`.

Esta alteração é posterior ao candidato dos 385 testes e à revisão ampliada.
Sua prova focal e os limites ficam registrados em
`.codex/p5-b-abono-decision-20261102/`. Ela não resolve nem encerra B01–B08 e não
altera os hashes históricos de `evidence/p5-b/validated-inputs.json`.

| Validação desta decisão | Resultado |
| --- | --- |
| `mvn test -Dtest=P5CustomFieldsAndDraftTest,TempoLicencaCrudContractTest,TempoLicencaIssue10RegressionTest` | **44 testes, zero falhas/erros/ignorados**, incluindo seis novos casos de política de cópia; [log focal](../.codex/p5-b-abono-decision-20261102/focal-tests.log) |
| Auditor `--strict` | **25/25 STATIC PASS; zero EditorialIssues**; [scorecard](../.codex/p5-b-abono-decision-20261102/resource-completeness.md) |
| Procedência | [Manifesto e delta delimitado](../.codex/p5-b-abono-decision-20261102/decision-manifest.json); somente serviço de gozo, teste focal e este documento foram alterados nesta decisão |

O JAR local do engine `.17` foi substituído por outra execução entre a primeira
bateria focal e o registro dos artefatos. A bateria foi repetida com hashes
capturados antes/depois, estáveis durante a execução final: engine
`e7ae99edde4350389e81ede78c3c140f56f1343cc21531f02e8a7e267e9cc13b` e metadata
`25e1423582ffc1e00b0bfc39f511384a0212d7664a85ba5949f17f429089ccfa`. A diferença
de bytes do engine em relação ao candidato anterior não foi atribuída a esta
alteração. O primeiro erro de compilação da sondagem, corrigido pela tipagem
explícita de `JsonNode` na asserção, também foi preservado no diretório de prova.

Os testes usam serviços e mappers reais com persistência simulada. Não foi
reexecutada a suíte integral dos 385 testes nem houve nova prova HTTP, browser,
Neon, publicação ou reavaliação dos oito achados da revisão ampliada. Os DTOs,
nomes de campos e metadados públicos permanecem os existentes; não há contrato
novo que exija atualizar bibliotecas Angular, corpus HTTP ou documentação de
plataforma fora deste host.

## Provas permanentes

| Cenário | Teste |
| --- | --- |
| R1, R2 e R3 | `P4CustomFieldAdoptionRegressionTest`, `P5CustomFieldsAndDraftTest` |
| C-01 / HTTP e comandos | `ErgonRuleViolationHttpTest`; suites de regras existentes |
| C-02 / read-only em ambos os modos bulk | `P5CustomFieldsAndDraftTest` com adapter e serviços reais |
| C-03 / schemas de todas as determinações | `ErgonReactiveDeterminationMetadataTest`: 14 definições, 28 projeções CREATE/EDIT |
| C-04 / órgão padrão, Orgao, Dependente, export/include/locate/bulk | `ErgonTenantIsolationTest` |
| C-05 / prévias e consultas de férias | `ErgonTenantIsolationTest` |
| H-05 / rejeição antes de lock | `ErgonTenantIsolationTest` |
| Metadados, lookups e expressões | `PraxisMetadataCompletenessTest`, gates nativos existentes |

As sondagens de `docs/evidence/p4-review/` e o relatório temporário P4 foram
removidos conforme solicitado. O estado inicial permanece no backup local
`.codex/p5-b-20261102/`, retirado da árvore ativa na higienização P5-C
e movido para `target/p5c-cleanup/`; ele não era artefato de publicação.

## Gates e identidade do candidato

| Gate | Resultado | Evidência |
| --- | --- | --- |
| `mvn test "-Dtest=!*Neon*"` com Java 21 | **385 testes; 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS**, 12:43:10 −03 | [Log integral](evidence/p5-b/showcase-tests.log) |
| `python ../scripts/workspace/audit-java-resource-completeness.py . --strict` | **exit 0; STATIC PASS; 25/25** | [Scorecard por recurso](evidence/p5-b/resource-completeness.md) |
| Engine: `PraxisRuleViolationExceptionTest` | **8 testes aprovados**, instalação local do candidato | [Log](evidence/p5-b/engine-install.log) |
| Starter: snapshot/patch, custom fields, lifecycle, executor, HTTP e versão | **66 testes aprovados**, instalação local do candidato | [Log](evidence/p5-b/starter-install.log) |

O gate do host inclui o compiler para 14 determinações e seus 28 schemas
CREATE/EDIT. As fixtures anteriores foram alinhadas ao carregamento sem lock
seguido de autorização/refresh com lock da P5-A, aos órgãos explícitos dos
registros de teste e ao consumo de códigos estruturados em vez de prefixos de
mensagem. As asserções de status, códigos, versões e efeitos foram conservadas.

| Critério | Resultado |
| --- | --- |
| Colunas de negócio / campos de resposta | 616 / 773; cobertura de **100%** em 25 recursos |
| Destino de escrita | 100% contabilizado nos recursos mutáveis; projeções somente leitura identificadas no scorecard |
| QuickViewCols / RichIdentity | 133 / 25 de 25 |
| ResourceReference com preview | 193 / 193 |
| MicroVisualization / RichEntityLookup | 23 / 23 fontes com inventário e subcontratos nativos |
| EditorialIssues | **0** |
| Filterable / variedade | 435 filtros; 4 a 16 operadores distintos por recurso |
| QuickFilter | 90 |
| ResourceIntent / WorkflowAction | 24 / 73 |
| BulkEditable / BulkOps | 233 / 41 |
| ResourceFact / UiAnalytics | 70 / 65 |
| ReactiveDet | 14, com compilação em ambos os modos |
| Invariantes e determinações testadas | 385 testes não-Neon aprovados; alcance detalhado nas suites e nos limites abaixo |

Coordenadas consumidas: `praxis-rules-engine:0.1.0-beta.17` e
`praxis-metadata-starter:8.0.0-rc.188`, ambas pré-release. Os JARs instalados
coincidiram com os JARs produzidos nos respectivos diretórios `target`:

| Artefato | SHA-256 |
| --- | --- |
| Engine `.17` | `6aadb8f020c87f3fdc7e02567c846a4f8a6b2f292016824d31d7ebdb3ca31925` |
| Metadata `.188` | `25e1423582ffc1e00b0bfc39f511384a0212d7664a85ba5949f17f429089ccfa` |

O [manifesto de entradas](evidence/p5-b/validated-inputs.json) registra os hashes
dos fontes/configurações do host, das dependências e dos complementos canônicos.
O [delta de fontes do host](evidence/p5-b/showcase-source-delta.patch) compara com
o backup inicial desta execução, preservando a separação do trabalho anterior.

## Limites de aceite

A revisão independente [P5-A](../../praxis-metadata-starter/docs/technical/P5-A-HARDENING.md)
está em `CHANGES REQUESTED`. Ela mantém sete achados de plataforma, incluindo
custom fields, classificação/admissão bulk, autorização de status e fallback
de cursores. Este recorte não os declara resolvidos nem concede aceite às Ondas
1–3. Em particular, os diagnósticos públicos por item de bulk continuam seguindo
a projeção sanitizada do SDK; não são um envelope RFC 7807 por item com toda a
proveniência interna. A correção da herança não amplia esse contrato público.

Não houve Neon, boot/HTTP integrado do showcase, browser, publicação, implantação,
commit, push ou tag. A compilação de metadata usa controllers, DTOs, resolver e
compiler reais em processo, sem certificar um endpoint implantado. A suíte não-Neon
inclui a fixture PostgreSQL local já existente; isso não certifica transações
concorrentes ou rollback de todos os fluxos do host.

As alterações do host são projeções dos contratos existentes. Foram atualizados
os guias dos donos canônicos e esta documentação do consumidor; não há mudança
do dialeto, formato de `x-ui`, URLs ou tipos Angular que exija regenerar landing,
corpus HTTP ou pacotes frontend. As árvores já continham alterações extensas;
o diff contra HEAD não representa apenas a P5-B.
