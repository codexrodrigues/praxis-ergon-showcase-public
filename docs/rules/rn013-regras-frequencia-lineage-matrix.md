# Matriz de Linhagem e Governança Determinística — `RN-013` (`PCK_REGRAS_FREQ`)

## 1. Visão Geral da Migração

Este documento registra a linhagem completa da família de regras de negócio **`RN-013`** do módulo de Administração de Pessoal do Ergon RH Público, originada na package PL/SQL legada `PCK_REGRAS_FREQ` (`pck_regras_freq.pkb`, rotinas `MAIN_PRE` e `MAIN_POS`, acionadas pelas triggers de `REGRAS_FREQ`) e materializada no showcase através do motor determinístico **`io.github.codexrodrigues:praxis-rules-engine:0.1.0-beta.17`**.

### Identidade Canônica do `RuleSetDefinition`
- **`domainScope`:** `workforce.frequency-and-absence`
- **`resourceKey`:** `administracao-pessoal.regras-frequencia`
- **`ruleSetKey`:** `ergon.administracao-pessoal.regras-frequencia.rn013`
- **`operation`:** `validate`
- **`version`:** `1`
- **`availableRoots`:** `["draft", "existingOverlaps", "tenant", "context"]`
- **`failPolicy`:** `RuleFailPolicy.FAIL_CLOSED`
- **Compatibilidade de Runtime:** Engine Contract `1.6` | Dialect `1.0` | Corpus SHA-256 `E890925C7306A2646CF86443C29ABDDEF0C0645C6228F1DE4A8740E45253D181`

---

## 2. Superação do Problema Estrutural do Legado (`IF v_ep THEN` / `EPB__REGRAS_FREQ`)

No legado Oracle (`pck_regras_freq.pkb`, linhas 121–191 e 236–275), a extensibilidade por cliente/órgão operava por meio da procedure dinâmica `EPB__REGRAS_FREQ.MAIN_PRE` / `EPB__REGRAS_FREQ.MAIN_POS`, que retornava um booleano grosseiro `v_ep`:

```sql
v_ep := EXEC_EPB__REGRAS_FREQ('MAIN_PRE', v_regras_freq, p_regras_freq_ant, p_oper);
IF v_ep THEN
  IF v_regras_freq.qtd_max < 0 THEN pack_hades.trata_erro(8380); END IF;
  ...
  IF v_regras_freq.dtfim < v_regras_freq.dtini THEN pack_hades.trata_erro(00019); END IF;
END IF;
```

Quando um órgão precisava alterar **uma única regra** (por exemplo, um teto específico de dias ou uma exigência adicional de ato legal) e retornava `v_ep = FALSE`, **todas as 14 decisões seguintes do bloco padrão eram silenciosamente desligadas**, exigindo copiar e manter todo o código-fonte do produto dentro do user exit regional.

No `praxis-rules-engine`, a `RN-013` é decomposta em **15 `DecisionSlot`s atômicos e independentes**, governados individualmente por `DecisionStage`, `OverridePolicy`, `SlotCardinality`, `DecisionAggregationPolicy` e `RuleParameterTemplate`.

---

## 3. Matriz de Linhagem PL/SQL -> `praxis-rules-engine` (`RN-013`)

| Código | `slotKey` | `DecisionStage` | `OverridePolicy` | Cardinalidade / Agregação | Executor | Linhagem PL/SQL (`pck_regras_freq.pkb`) | Semântica e Expressão Canônica |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`ERG-00019`** | `rn013.periodo_ordem_valida` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L183–187): `IF v_regras_freq.dtfim < v_regras_freq.dtini THEN pack_hades.trata_erro(00019);` | Vigência final não pode anteceder a vigência inicial (`draft.dtfim == null \|\| date(draft.dtfim) >= date(draft.dtini)`). Guarda estrutural imune a override. |
| **`ERG-08380`** | `rn013.qtd_max_non_negative` | `DOMAIN_DECISION` | `PARAMETERIZABLE` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` + `RuleParameterTemplate` (`ergon.rn013:qtd-max-bounds@1.0`) | `MAIN_PRE` (L136–138): `IF v_regras_freq.qtd_max < 0 THEN pack_hades.trata_erro(8380);` | Quantidade máxima por concessão dentro da faixa parametrizada (`draft.qtdMaxDias >= param.minAllowedQty && draft.qtdMaxDias <= param.maxAllowedQty`, default `0..3650`). |
| **`ERG-08381`** | `rn013.qtd_min_non_negative` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L139–141): `IF v_regras_freq.qtd_min < 0 THEN pack_hades.trata_erro(8381);` | Quantidade mínima por concessão não negativa (`draft.qtdMinDias == null \|\| draft.qtdMinDias >= 0`). |
| **`ERG-08382`** | `rn013.qtd_max_coherent_with_min` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L142–144): `IF v_regras_freq.qtd_max < v_regras_freq.qtd_min THEN pack_hades.trata_erro(8382);` | Coerência entre limite máximo e mínimo (`draft.qtdMaxDias == null \|\| draft.qtdMinDias == null \|\| draft.qtdMaxDias >= draft.qtdMinDias`). |
| **`ERG-08383`** | `rn013.consec_mes_non_negative` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L146–148): `IF v_regras_freq.qtd_max_consec_mes < 0 THEN pack_hades.trata_erro(8383);` | Limite máximo consecutivo no mês não negativo (`draft.qtdMaxConsecMes == null \|\| draft.qtdMaxConsecMes >= 0`). |
| **`ERG-08384`** | `rn013.consec_ano_non_negative` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L149–151): `IF v_regras_freq.qtd_max_consec_ano < 0 THEN pack_hades.trata_erro(8384);` | Limite máximo consecutivo no ano não negativo (`draft.qtdMaxConsecAno == null \|\| draft.qtdMaxConsecAno >= 0`). |
| **`ERG-08385`** | `rn013.interp_mes_non_negative` | `DOMAIN_DECISION` | `REPLACEABLE` | `SINGLE` / `SINGLE_RESULT` | `JSON_LOGIC` | `MAIN_PRE` (L152–154): `IF v_regras_freq.qtd_max_interp_mes < 0 THEN pack_hades.trata_erro(8385);` | Limite máximo interpolado no mês não negativo (`draft.qtdMaxInterpMes == null \|\| draft.qtdMaxInterpMes >= 0`). Autorizado para `REPLACE_EXACT` por órgão. |
| **`ERG-08386`** | `rn013.interp_ano_non_negative` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L155–157): `IF v_regras_freq.qtd_max_interp_ano < 0 THEN pack_hades.trata_erro(8386);` | Limite máximo interpolado no ano não negativo (`draft.qtdMaxInterpAno == null \|\| draft.qtdMaxInterpAno >= 0`). |
| **`ERG-08387`** | `rn013.int_prorrog_non_negative` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L159–161): `IF v_regras_freq.int_prorrog < 0 THEN pack_hades.trata_erro(8387);` | Intervalo de prorrogação não negativo (`draft.intProrrogDias == null \|\| draft.intProrrogDias >= 0`). |
| **`ERG-08388`** | `rn013.interv_min_non_negative` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L162–164): `IF v_regras_freq.interv_min < 0 THEN pack_hades.trata_erro(8388);` | Interstício mínimo entre ocorrências não negativo (`draft.intervMinDias == null \|\| draft.intervMinDias >= 0`). |
| **`ERG-08389`** | `rn013.consec_mes_calendar_limit` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L166–168): `IF v_regras_freq.qtd_max_consec_mes > 31 THEN pack_hades.trata_erro(8389);` | Teto calendário mensal para dias consecutivos (`draft.qtdMaxConsecMes == null \|\| draft.qtdMaxConsecMes <= 31`). |
| **`ERG-08390`** | `rn013.interp_mes_calendar_limit` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L170–172): `IF v_regras_freq.qtd_max_interp_mes > 31 THEN pack_hades.trata_erro(8390);` | Teto calendário mensal para dias interpolados (`draft.qtdMaxInterpMes == null \|\| draft.qtdMaxInterpMes <= 31`). |
| **`ERG-08391`** | `rn013.consec_ano_calendar_limit` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L174–176): `IF v_regras_freq.qtd_max_consec_ano > 366 THEN pack_hades.trata_erro(8391);` | Teto calendário anual para dias consecutivos (`draft.qtdMaxConsecAno == null \|\| draft.qtdMaxConsecAno <= 366`). Suporta `AUGMENT` por órgão (`CERG-09001`). |
| **`ERG-08392`** | `rn013.interp_ano_calendar_limit` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `MAIN_PRE` (L178–180): `IF v_regras_freq.qtd_max_interp_ano > 366 THEN pack_hades.trata_erro(8392);` | Teto calendário anual para dias interpolados (`draft.qtdMaxInterpAno == null \|\| draft.qtdMaxInterpAno <= 366`). |
| **`ERG-08393`** | `rn013.concomitancia_vigencia_overlap` | `POST_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JAVA` (`ergon.rn013:concomitancia-vigencia-overlap@1.0`) | `MAIN_POS` (L251–273): `PACK_HADES.EH_CONCOMITANTE(RF.DTINI, RF.DTFIM, v.DTINI, v.DTFIM) = 1` | Verifica sobreposição de intervalos `[dtini, dtfim]` na coleção pré-carregada `existingOverlaps` para a mesma chave de escopo funcional. |

---

## 4. Os 4 Modos de Customização por Órgão Provados em `Rn013RuleEngineContractTest`

1. **Modo 1 — `OverridePolicy.FORBIDDEN` (Guarda Estrutural Protegida):**
   - Qualquer tentativa de anexar um `DecisionBinding` de `DecisionSource.CUSTOMER` a `rn013.periodo_ordem_valida` (`ERG-00019`) é bloqueada em tempo de compilação pelo `PraxisRulePlanCompiler` com `RulePlanException` (`RulePlanIssueCode.PLAN_OVERRIDE_INVALID`).
2. **Modo 2 — `OverridePolicy.PARAMETERIZABLE` + `CompositionPolicy.PARAMETERIZE` (`RuleParameterTemplate`):**
   - O Órgão `empCodigo = 2` parametriza `maxAllowedQty = 12` no template `ergon.rn013:qtd-max-bounds@1.0` sem alterar a árvore JSON Logic do slot `rn013.qtd_max_non_negative` (`ERG-08380`). Enquanto `qtdMaxDias = 30` é `ALLOW` no Órgão 1, é `DENY` (`ERG-08380`) no Órgão 2.
3. **Modo 3 — `OverridePolicy.RESTRICT_ONLY` + `CompositionPolicy.AUGMENT` (`DENY_OVERRIDES`):**
   - O Órgão `empCodigo = 2` adiciona o binding `customer.emp2.rn013.augment_cerg09001` (`CERG-09001`) ao slot `rn013.consec_ano_calendar_limit`, exigindo `documentoLegalId != null` quando `qtdMaxDias > 30`. Tanto a regra do produto (`ERG-08391`) quanto a regra do órgão (`CERG-09001`) permanecem ativas simultaneamente.
4. **Modo 4 — `OverridePolicy.REPLACEABLE` + `CompositionPolicy.REPLACE_EXACT` (Substituição Cirúrgica sem Efeito Colateral):**
   - O Órgão `empCodigo = 2` substitui exclusivamente o slot `rn013.interp_mes_non_negative` (`ERG-08385` -> `CERG-08385`), mantendo todas as outras 14 decisões do produto (`ERG-00019`, `ERG-08380`..`ERG-08384`, `ERG-08386`..`ERG-08393`) integralmente ativas.

---

## 5. Governança Corporativa, Proveniência (`PRODUCT` vs `CUSTOMER`) e Superfícies REST

### 5.1. Paridade de Escopo Nulo em `PCK_REGRAS_FREQ.MAIN_POS` (`ERG-08393`)
No pacote legado Oracle (`pck_regras_freq.pkb`, L254–261), regras estatutárias podem ser cadastradas em escopo geral do ente público (`EMP_CODIGO IS NULL`, `REG_JUR IS NULL`, `TIPOVINC IS NULL`, `CATEGORIA IS NULL`, `SUBCATEGORIA IS NULL`). O `RegraFrequenciaEstatutariaService` carrega candidatos por `(tipofreq, codfreq)` quando qualquer dimensão opcional de escopo é nula e delega a comparação `null == null` de todas as 7 dimensões ao executor determinístico `ergon.rn013:concomitancia-vigencia-overlap@1.0`.

### 5.2. Cobertura Dual de Atributos (`REGRAS_FREQ` + `ERGadm00036` via `ruleParams`)
Além das colunas numéricas canônicas de `RegraFrequenciaEstatutaria`, os slots `ERG-08383`, `ERG-08387` e `ERG-08388` validam também os pares de formulário de `ERGadm00036` quando projetados em `draft.ruleParams` (`prazo` + `tipoUnidadePrazo`, `prorrogacao` `S`/`N` vs `intProrrogDias`, e `intersticio` + `tipoUnidadeInterst`).

### 5.3. Resposta Estruturada RFC 7807 (`ProblemDetail`) e Rastreabilidade de Origem
Quando `ErgonRuleEngineFacade.enforce(...)` ou `RegraFrequenciaEstatutariaService.validateRn013(...)` detecta qualquer decisão diferente de `RuleDecision.ALLOW` (`DENY`, `INCONCLUSIVE`, `TECHNICAL_ERROR`), a `ErgonRuleViolationException` (`HTTP 422 Unprocessable Entity`) popula automaticamente no `ProblemDetail`:
- `ruleSetKey`, `resourceKey`, `planDigest`, `factsDigest`, `decision`;
- `reasonCodes`: lista ordenada de códigos `ERG-*` / `CERG-*`;
- `slotKeys`: lista de `DecisionSlot`s acionados;
- `decisionSources`: lista de origens (`PRODUCT` e/ou `CUSTOMER`);
- `violations`: coleção de `SlotViolationDetail(code, slotKey, bindingKey, source, stage, decision, targetField, messagePtBr, plsqlOrigin)`.

### 5.4. Endpoints Operacionais e de Simulação (`RegraFrequenciaEstatutariaController`)
- `GET /api/ergon/regras-frequencia-estatutaria/rn013/plan?empCodigo={id}`: inspeciona o `RuleDecisionPlan` compilado para o órgão, seus 15 slots, bindings ativos (`PRODUCT` vs `CUSTOMER`) e o `ErgonRuleErrorCatalog`.
- `POST /api/ergon/regras-frequencia-estatutaria/rn013/evaluate`: simula a avaliação de um rascunho de `RegraFrequenciaEstatutaria` sem lançar exceção, retornando o diagnóstico completo por slot.
- `POST /api/ergon/regras-frequencia-estatutaria/rn013/validate`: valida o rascunho em modo *fail-closed* (`200 OK` quando `ALLOW` ou `422 Unprocessable Entity` com `ProblemDetail` estruturado).
