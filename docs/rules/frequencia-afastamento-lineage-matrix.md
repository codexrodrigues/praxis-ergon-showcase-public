# Matriz de Linhagem 4-Hops e Governança Determinística — Frequência (`PCK_FREQUENCIAS`) e Afastamentos (`PCK_LIC_AFAST`)

## 1. Visão Geral e Protocolo de 4 Saltos (`4-Hop Lineage Protocol`)

Este documento registra a extração de linhagem direta em 4 saltos e a modelagem determinística das famílias de regras de **Lançamento de Ocorrências de Frequência** (`Frequencia`) e **Concessão/Prorrogação de Licenças e Afastamentos** (`Afastamento`) do Ergon RH Público sobre o motor **`io.github.codexrodrigues:praxis-rules-engine:0.1.0-beta.17`** (Engine Contract `1.6` | Dialect `1.0`).

### Rastreabilidade dos 4 Saltos no Legado Oracle (`docs-legado/v7x/aps/`)
1. **Salto 1 — Telas e Transações de Origem:**
   - `ERGadm00189` (`FREQUENCIAS` — lançamento e manutenção de ocorrências de frequência e abono de faltas);
   - `ERGadm00154` (`LIC_AFAST` — concessão, prorrogação, substituição e encerramento de licenças e afastamentos);
   - `ERGadm00036` (`REGRAS_FREQ` — parametrização estatutária por órgão, regime jurídico, vínculo, categoria, subcategoria e modalidade).
2. **Salto 2 — Packages de Tabela e Rotinas Centrais (`fontes_ergon/`):**
   - `pck_frequencias.pkb` (`MAIN_PRE`, `MAIN_POS`, `valida_hora_entrada_saida`, `verifica_afastamento`, `verifica_data_freq`, `verifica_data`, `verifica_concomitancia_periodo`, `verifica_concomitancia_afast`, `valid_regras_freq`);
   - `pck_lic_afast.pkb` (`MAIN_PRE`, `MAIN_POS`, `valid_licafast`, `valid_reg_licafast`, `verifica_dtfim`);
   - `pck_codigos_freq.pkb` (`MAIN_PRE` — `ERG-03274` a `ERG-03283`) e `pck_tipo_freq.pkb` (`MAIN_PRE` — `ERG-03269` a `ERG-03272`).
3. **Salto 3 — Procedures Auxiliares e Ponte Dinâmica com `REGRAS_FREQ`:**
   - `pck_frequencias.valid_regras_freq` (L1002–1620) e `pck_lic_afast.valid_reg_licafast` (L681–1157): resolvem a linha vigente de `REGRAS_FREQ` para a chave funcional do vínculo `(emp_codigo, regimejur, tipovinc, categoria, subcategoria, tipofreq, codfreq)` na data do evento;
   - `erg_pm_geralicafast.prc`: geração de licença médica / perícia (`ERG-07512` a `ERG-07518`).
4. **Salto 4 — User Exits e Customizações de Órgãos (`fontes_c_ergon/`):**
   - `pck_before_cergon.pkb` (`EXEC_EPB__FREQUENCIAS`, `EXEC_EPB__LIC_AFAST`), `pck_after_cergon.pkb` (`EXEC_EPA__FREQUENCIAS`, `EXEC_EPA__LIC_AFAST`);
   - `ep__verifica_frequencias.fnc` (`ERG-10400` / `ERG-10600`), `ep__verifica_lic_afast.fnc` (`ERG-10300`), `EP__PERIODO_AFAST.fnc` (`ERG-20000`), `EP__VERIFICA_AFAST_SUBSTITUTO.FNC`.

---

## 2. Ponte Dinâmica com `RegraFrequenciaEstatutaria` (`effectiveStatutoryRule`)

No legado Oracle, tanto `PCK_FREQUENCIAS.valid_regras_freq` quanto `PCK_LIC_AFAST.valid_reg_licafast` consultavam a tabela `REGRAS_FREQ` para aplicar os limites estatutários sobre o evento concreto do servidor. No showcase Praxis, `FrequenciaService` e `AfastamentoService` resolvem a entidade vigente `RegraFrequenciaEstatutaria` (respeitando especificidade de órgão/subcategoria e fallback para regra geral do ente público) e a injetam como o nó canônico **`effectiveStatutoryRule`** na árvore JSON de fatos (`facts`), juntamente com as métricas históricas pré-calculadas a partir das coleções pré-carregadas (`existingFrequencias`, `existingAfastamentos`, `activeAfastamentos`, `activeFrequencias`):

- `effectiveStatutoryRule.present`: indica se há regra estatutária vigente para o escopo funcional;
- `effectiveStatutoryRule.naoSeAplica`: vedação estatutária expressa à modalidade para o regime/vínculo/categoria do servidor;
- `effectiveStatutoryRule.qtdMinDias` / `qtdMaxDias`: duração mínima e máxima permitida por ocorrência/concessão;
- `effectiveStatutoryRule.intProrrogDias` + `accumulatedProrrogDays`: janela de prorrogação e soma acumulada de dias na cadeia de prorrogação;
- `effectiveStatutoryRule.intervMinDias` + `daysSincePreviousOccurrence`: interstício mínimo exigido desde o término da ocorrência anterior da mesma espécie;
- `effectiveStatutoryRule.qtdMaxConsecMes`, `qtdMaxInterpMes`, `qtdMaxConsecAno`, `qtdMaxInterpAno` + acumulados mensais/anuais (`projectedConsecMesDays`, `projectedInterpMesDays`, `projectedConsecAnoDays`, `projectedInterpAnoDays`);
- `effectiveStatutoryRule.exigeDocumentoLegal` (`documentoLegalId != null` ou `ruleParams.pontlei == 'S'`) e `effectiveStatutoryRule.prorrogacaoPermitida` (`ruleParams.prorrogacao != 'N'`);
- `effectiveStatutoryRule.permiteConcomitancia`: controla se a modalidade admite concomitância cruzada entre `Frequencia` e `Afastamento`.

---

## 3. Matriz de Linhagem — `FrequenciaLancamentoRuleSet` (`ergon.administracao-pessoal.frequencias.lancamento`)

- **`domainScope`:** `workforce.frequency-and-absence`
- **`resourceKey`:** `administracao-pessoal.frequencias`
- **`ruleSetKey`:** `ergon.administracao-pessoal.frequencias.lancamento`
- **`availableRoots`:** `["draft", "vinculo", "pessoa", "tipoFrequencia", "codigoFrequencia", "documentoLegal", "effectiveStatutoryRule", "existingFrequencias", "activeAfastamentos", "tenant", "context"]`

| Código | `slotKey` | `DecisionStage` | `OverridePolicy` | Cardinalidade / Agregação | Executor | Linhagem PL/SQL (`pck_frequencias.pkb` / `ep__*`) | Semântica Determinística |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`ERG-00415`** | `freq.periodo_ordem_valida` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `verifica_data` (L793–799): `p_dtini > p_dtfim AND p_dtfim IS NOT NULL -> trata_erro(415)` | Data final da frequência não pode anteceder a data inicial (`draft.dtfim == null \|\| date(draft.dtfim) >= date(draft.dtini)`). |
| **`ERG-00400`** | `freq.codigo_habilitado_via_frequencia` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L213–225): `v_cod_freq.via_frequencia = 'N' -> trata_erro(400)` | O código de frequência deve existir, pertencer ao `tipofreq` informado e permitir lançamento via frequência (`codigoFrequencia.viaFrequencia == true`). |
| **`ERG-00410`** | `freq.vinculo_vigente_no_periodo` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `verifica_data_freq` (L751–789): `p_dtini < v_dtexerc -> 410; >= v_dtvac -> 411; >= v_dtaposent -> 413` | O período da frequência deve estar contido na vigência ativa do vínculo (`dtini >= vinculo.dtexerc` e anterior a `dtvac`/`dtaposent`). |
| **`ERG-00436`** | `freq.horario_entrada_saida_valido` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valida_hora_entrada_saida` (L672–722): exige `hora_entrada` e `hora_saida` coerentes (`436`/`438`) | Quando `codigoFrequencia.horaEntradaSaida == true` (ou horário parcial é informado), exige `horaEntrada` e `horaSaida` preenchidas com `horaSaida >= horaEntrada`. |
| **`ERG-00443`** | `freq.compatibilidade_sexo_servidor` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L203–209): `v_cod_freq.sexo <> 'A' AND v_cod_freq.sexo <> v_sexo -> trata_erro(443)` | O código de frequência deve ser compatível com o sexo cadastral do servidor (`codigoFrequencia.sexo in [null, 'A', 'I'] \|\| codigoFrequencia.sexo == pessoa.sexo`). |
| **`ERG-00441`** | `freq.regra_estatutaria_obrigatoria` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `valid_regras_freq` (L1105–1123): `v_cod_freq.regras_freq = 'S'` sem regra vigente -> `trata_erro(441)` | Quando `codigoFrequencia.regrasFreq == true`, exige regra estatutária vigente (`effectiveStatutoryRule.present == true`) para o escopo funcional do vínculo. |
| **`ERG-00430`** | `freq.vedacao_nao_se_aplica` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `valid_regras_freq` (L1609–1611): `v_regras_freq.nao_se_aplica = 'S' -> trata_erro(430)` | Bloqueia lançamento quando a regra estatutária vigente veda expressamente a modalidade para o vínculo (`effectiveStatutoryRule.naoSeAplica != true`). |
| **`ERG-00427`** | `freq.duracao_minima_estatutaria` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_regras_freq` (L1138–1140): `v_qtd < v_regras_freq.qtd_min -> trata_erro(427)` | A quantidade/duração apurada da ocorrência deve respeitar o mínimo da modalidade (`codigoFrequencia.qtdmin`) e da regra estatutária (`effectiveStatutoryRule.qtdMinDias`). |
| **`ERG-00428`** | `freq.duracao_maxima_estatutaria` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_regras_freq` (L1144–1148): `v_qtd > v_regras_freq.qtd_max -> trata_erro(428)` | A quantidade/duração apurada da ocorrência não pode exceder o máximo da modalidade (`codigoFrequencia.qtdmax`) nem o teto estatutário (`effectiveStatutoryRule.qtdMaxDias`). |
| **`ERG-00429`** | `freq.limite_prorrogacao_acumulada` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_regras_freq` (L1152–1198): soma na janela `int_prorrog > qtd_max -> trata_erro(429)` | A soma de dias de ocorrências contínuas dentro da janela de prorrogação (`intProrrogDias`) não pode ultrapassar `effectiveStatutoryRule.qtdMaxDias`. |
| **`ERG-00440`** | `freq.intersticio_minimo_entre_ocorrencias` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_regras_freq` (L1202–1248): intervalo desde ocorrência anterior `< interv_min -> trata_erro(440)` | O intervalo em dias desde o término da última ocorrência da mesma espécie (`daysSincePreviousOccurrence`) deve ser `>= effectiveStatutoryRule.intervMinDias`. |
| **`ERG-00444`** | `freq.limites_acumulados_mes_ano` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_regras_freq` (L1390–1592): tetos `qtd_max_consec_mes`, `interp_mes`, `consec_ano`, `interp_ano -> 444` | Respeita os 4 tetos acumulados mensais/anuais (`qtdMaxConsecMes`, `qtdMaxInterpMes`, `qtdMaxConsecAno`, `qtdMaxInterpAno`). Suporta `AUGMENT` por órgão (`CERG-09101`). |
| **`ERG-00445`** | `freq.exigencia_documento_legal` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `EPB__FREQUENCIAS` / `REGRAS_FREQ.PONTLEI`: exigência de ato/documento comprobatório | Quando `effectiveStatutoryRule.exigeDocumentoLegal == true`, exige que o lançamento informe `draft.documentoLegalId != null`. |
| **`ERG-00446`** | `freq.limite_retroatividade_lancamento` | `DOMAIN_DECISION` | `PARAMETERIZABLE` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` + `RuleParameterTemplate` (`ergon.frequencias:retroactive-launch-bounds@1.0`) | `EP__VERIFICA_FREQUENCIAS` (`ERG-10400`/`ERG-10600`): janela máxima de lançamento retroativo por órgão | Limita a quantidade de dias retroativos (`draft.retroactiveDays`) entre `minRetroactiveDays` e `maxRetroactiveDays` (default `0..60` dias no Órgão 1; parametrizado para `0..15` dias no Órgão 2). |
| **`ERG-00417`** | `freq.concomitancia_afastamento_ativo` | `DOMAIN_DECISION` | `REPLACEABLE` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `verifica_afastamento` (L728–747) / `verifica_concomitancia_afast` (L896–938): `417`/`425` | Bloqueia lançamento de frequência em período coberto por `Afastamento` ativo quando `effectiveStatutoryRule.permiteConcomitancia != true`. Autorizado para `REPLACE_EXACT` (`CERG-00417`). |
| **`ERG-00424`** | `freq.sobreposicao_ocorrencia_mesmo_tipo` | `POST_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JAVA` (`ergon.frequencias:sobreposicao-periodo-overlap@1.0`) | `verifica_concomitancia_periodo` (L866–887): interseção de `[dtini, dtfim]` no mesmo vínculo/tipo -> `424` | Verifica na coleção pré-carregada `existingFrequencias` se já existe ocorrência ativa não cancelada sobreposta no período. |

---

## 4. Matriz de Linhagem — `AfastamentoConcessaoRuleSet` (`ergon.administracao-pessoal.afastamentos.concessao`)

- **`domainScope`:** `workforce.frequency-and-absence`
- **`resourceKey`:** `administracao-pessoal.afastamentos`
- **`ruleSetKey`:** `ergon.administracao-pessoal.afastamentos.concessao`
- **`availableRoots`:** `["draft", "vinculo", "pessoa", "tipoFrequencia", "codigoFrequencia", "documentoLegal", "effectiveStatutoryRule", "existingAfastamentos", "activeFrequencias", "afastamentoOrigem", "substitutoVinculo", "tenant", "context"]`

| Código | `slotKey` | `DecisionStage` | `OverridePolicy` | Cardinalidade / Agregação | Executor | Linhagem PL/SQL (`pck_lic_afast.pkb` / `erg_pm_geralicafast.prc`) | Semântica Determinística |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`ERG-04312`** | `afast.periodo_ordem_valida` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L158–164): `dtfim < dtini -> 4312`; `dtprevfim < dtini -> 4313` | Coerência cronológica entre `dtini`, `dtprevfim` e `dtfim` (`dtprevfim >= dtini` e `dtfim >= dtini`). |
| **`ERG-04314`** | `afast.vinculo_em_exercicio_no_periodo` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `valid_licafast` (L457–488): `dtini < v_dtexerc` ou após vacância/aposentadoria -> `4314` | O início e o término do afastamento devem ocorrer após a entrada em exercício (`vinculo.dtexerc`) e antes de vacância/aposentadoria. |
| **`ERG-04306`** | `afast.modalidade_habilitada_afastamento` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` / `pck_codigos_freq`: código exige flag `afastamento = 'S'` | O código de frequência deve existir, pertencer ao `tipofreq` informado e estar habilitado para afastamento (`codigoFrequencia.afastamento == true`). |
| **`ERG-04310`** | `afast.compatibilidade_sexo_servidor` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L187–193): `v_cod_freq.sexo <> 'A' AND v_cod_freq.sexo <> v_sexo -> trata_erro(4310)` | Compatibilidade entre o sexo do servidor e a restrição de gênero da modalidade de licença (`codigoFrequencia.sexo`). |
| **`ERG-04305`** | `afast.regra_estatutaria_obrigatoria` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `valid_reg_licafast` (L756–785): `v_cod_freq.regras_freq = 'S'` sem regra vigente -> `4305` | Quando `codigoFrequencia.regrasFreq == true`, exige existência de regra estatutária vigente (`effectiveStatutoryRule.present == true`). |
| **`ERG-04301`** | `afast.vedacao_nao_se_aplica` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `valid_reg_licafast` (L790–796): `v_regras_freq.nao_se_aplica = 'S' -> trata_erro(4301/4302)` | Veda a concessão de licença/afastamento quando `effectiveStatutoryRule.naoSeAplica == true` para o escopo funcional do vínculo. |
| **`ERG-04304`** | `afast.duracao_minima_estatutaria` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_reg_licafast` (L813–815, 827–829): duração `< qtd_min -> trata_erro(4304)` | A duração em dias do afastamento (`effectiveDurationDays`) deve ser maior ou igual a `codigoFrequencia.qtdmin` e `effectiveStatutoryRule.qtdMinDias`. |
| **`ERG-04303`** | `afast.duracao_maxima_estatutaria` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_reg_licafast` (L809–811, 835–836): duração `> qtd_max -> trata_erro(4303)` | A duração em dias do afastamento (`effectiveDurationDays`) não pode superar `codigoFrequencia.qtdmax` nem `effectiveStatutoryRule.qtdMaxDias`. |
| **`ERG-04317`** | `afast.prorrogacao_estatutaria_valida` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_reg_licafast` (L840–885): validação de prorrogação, janela `int_prorrog` e soma `<= qtd_max` | Quando `draft.prorrogacao == true`, exige permissão estatutária (`prorrogacaoPermitida != false`), `afastamentoOrigem` válido do mesmo vínculo, intervalo `<= intProrrogDias` e soma acumulada `<= qtdMaxDias`. |
| **`ERG-04309`** | `afast.intersticio_minimo_entre_concessoes` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_reg_licafast` (L888–935): intervalo entre concessões `< interv_min -> trata_erro(4309)` | Exige cumprimento do interstício mínimo (`daysSincePreviousAfastamento >= effectiveStatutoryRule.intervMinDias`) desde o término da concessão anterior da mesma modalidade. |
| **`ERG-04311`** | `afast.limites_acumulados_mes_ano` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `valid_reg_licafast` (L940–1095): excesso consecutivo ou interpolado no mês/ano -> `4311` | Verifica tetos mensais e anuais (`qtdMaxConsecMes`, `qtdMaxInterpMes`, `qtdMaxConsecAno`, `qtdMaxInterpAno`). Suporta `AUGMENT` por órgão (`CERG-09201`). |
| **`ERG-07513`** | `afast.dispensa_pericia_documento_legal` | `DOMAIN_DECISION` | `PARAMETERIZABLE` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` + `RuleParameterTemplate` (`ergon.afastamentos:pericia-waiver-bounds@1.0`) | `erg_pm_geralicafast.prc` / `EPB__LIC_AFAST`: dispensa de perícia/documento legal até `N` dias por órgão | Permite dispensa de documento/laudo pericial quando `effectiveStatutoryRule.exigeDocumentoLegal != true` e a duração está dentro da faixa parametrizada `minDaysWithoutPericia..maxDaysWithoutPericia` (default `1..15` dias no Órgão 1; `1..5` dias no Órgão 2). |
| **`ERG-04318`** | `afast.substituto_designado_valido` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `EP__VERIFICA_AFAST_SUBSTITUTO.FNC`: consistência do par `(numfuncSubstituto, numvincSubstituto)` | Se houver substituto informado, exige par completo, veda auto-substituição (`numfuncSubstituto != draft.numfunc`) e exige vínculo substituto ativo no mesmo órgão. |
| **`ERG-04316`** | `afast.concomitancia_frequencia_ativa` | `DOMAIN_DECISION` | `REPLACEABLE` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `valid_licafast` (L507–518): conflito com frequência ativa no período -> `4316` | Bloqueia concessão de afastamento quando há `Frequencia` ativa no período e `effectiveStatutoryRule.permiteConcomitancia != true`. Autorizado para `REPLACE_EXACT` (`CERG-04316`). |
| **`ERG-04300`** | `afast.sobreposicao_afastamento_ativo` | `POST_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JAVA` (`ergon.afastamentos:sobreposicao-afastamento-overlap@1.0`) | `MAIN_POS` (L342–356): `PACK_HADES.EH_CONCOMITANTE` entre afastamentos do vínculo -> `4300` | Verifica na coleção pré-carregada `existingAfastamentos` se há outro afastamento ativo/homologado com período sobreposto no mesmo vínculo. |

---

## 5. Os 4 Modos de Customização por Órgão (`empCodigo = 1` vs `empCodigo = 2`)

1. **Modo 1 — `OverridePolicy.FORBIDDEN` (Guardas Estruturais Protegidas):**
   - Tentativas de sobrescrever `freq.periodo_ordem_valida` (`ERG-00415`) ou `afast.periodo_ordem_valida` (`ERG-04312`) são rejeitadas estaticamente pelo `PraxisRulePlanCompiler` com `RulePlanIssueCode.PLAN_OVERRIDE_INVALID`.
2. **Modo 2 — `OverridePolicy.PARAMETERIZABLE` + `CompositionPolicy.PARAMETERIZE` (`RuleParameterTemplate` Contrato 1.6):**
   - Em `FrequenciaLancamentoRuleSet`: o template `ergon.frequencias:retroactive-launch-bounds@1.0` (`ERG-00446`) define default `maxRetroactiveDays = 60` para o Órgão 1 e é parametrizado para `maxRetroactiveDays = 15` no Órgão 2 via RFC 6901 JSON Pointer.
   - Em `AfastamentoConcessaoRuleSet`: o template `ergon.afastamentos:pericia-waiver-bounds@1.0` (`ERG-07513`) define dispensa de documento/perícia até `maxDaysWithoutPericia = 15` dias no Órgão 1 e é parametrizado para `maxDaysWithoutPericia = 5` dias no Órgão 2.
3. **Modo 3 — `OverridePolicy.RESTRICT_ONLY` + `CompositionPolicy.AUGMENT` (`DENY_OVERRIDES`):**
   - Em `FrequenciaLancamentoRuleSet`: o Órgão 2 adiciona `CERG-09101` ao slot `freq.limites_acumulados_mes_ano`, exigindo `documentoLegalId != null` sempre que a ocorrência ultrapassar `3` dias, mantendo os tetos mensais/anuais do produto (`ERG-00444`) ativos.
   - Em `AfastamentoConcessaoRuleSet`: o Órgão 2 adiciona `CERG-09201` ao slot `afast.limites_acumulados_mes_ano`, exigindo `atoPublicacaoId != null` (publicação em Diário Oficial) para afastamentos com duração superior a `30` dias, mantendo `ERG-04311` ativo.
4. **Modo 4 — `OverridePolicy.REPLACEABLE` + `CompositionPolicy.REPLACE_EXACT` (Substituição Cirúrgica de Slot Único):**
   - Em `FrequenciaLancamentoRuleSet`: o Órgão 2 substitui `freq.concomitancia_afastamento_ativo` (`ERG-00417` -> `CERG-00417`), vedando qualquer lançamento de frequência durante afastamento ativo mesmo quando `effectiveStatutoryRule.permiteConcomitancia == true`, sem desligar nenhum dos outros 15 slots do produto.
   - Em `AfastamentoConcessaoRuleSet`: o Órgão 2 substitui `afast.concomitancia_frequencia_ativa` (`ERG-04316` -> `CERG-04316`), vedando concessão de afastamento sobre frequência ativa mesmo quando `effectiveStatutoryRule.permiteConcomitancia == true`, mantendo todos os demais 14 slots do produto ativos.
