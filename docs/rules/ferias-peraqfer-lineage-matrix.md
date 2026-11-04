# Matriz de Linhagem 4-Hops — Parâmetros de Contagem, Períodos Aquisitivos, Gozo de Férias e Abono Pecuniário (`PARAMETROS_CONTA`, `PERAQFER`, `FERIAS` e `ERG_ABONOS_FERIAS`)

Este artefato documenta a extração arqueológica em **4 Hops** das regras de negócio de parametrização estatutária/celetista de contagem de férias (`PARAMETROS_CONTA`), apuração e manutenção de períodos aquisitivos (`PERAQFER`), programação/concessão de parcelas de gozo (`FERIAS`) e venda de abono pecuniário (`ERG_ABONOS_FERIAS`) do legado Oracle PL/SQL do Ergon RH, sua classificação canônica e sua materialização determinística em **3 `RuleSets` governados** sobre o `praxis-rules-engine:0.1.0-beta.17` (Contrato `1.6`), apoiados pelo resolvedor canônico de precedência (`EffectiveVacationParameterResolver` — Tarefa 24610).

---

Os seis slots de extensão preservam as chamadas antes e depois da validação nas packages de origem. O produto não instala bindings nesses slots. Uma política efetivamente publicada no slot declara o próprio código e a sua condição de rejeição.

## 1. Protocolo de Extração em 4 Hops

| Hop | Camada Legada | Artefato / Objeto Inspecionado | Papel no Fluxo de Escrita |
|---|---|---|---|
| **Hop 1** | **Tela / Transação XML** | `ERGadm00105` (Parâmetros de Contagem), `ERGadm00118` (Períodos Aquisitivos de Férias), `ERGadm00119` (Concessão e Gozo de Férias / Abono Pecuniário) | Captura do escopo funcional (`EMP_CODIGO`, `REGIMEJUR`, `TIPOVINC`, `CATEGORIA`, `SUBCATEGORIA`), ciclo aquisitivo (`NUMFUNC`, `NUMVINC`, `CHAVE`, `DTINI`, `DTFIM`, `TOTDIAS`, `FALTAS`, `DT_PRESCRICAO`, overrides individuais) e programação de parcelas (`ORDEM`, `DTINI`, `DTPREVFIM`, `DTFIM`, `ABONO_PECUNIARIO`, `DIAS_ABONO`, `ABONO_CONST`, `ADIANT_13`, `ADIANT_SAL`, `EMPRESTIMO`, substituto). |
| **Hop 2** | **Triggers / Packages de Tabela** | `D:\Developer\Techne\ErgonX\migracao\docs-legado\v7x\aps\fontes_ergon\pck_parametros_conta.pkb`<br>`D:\Developer\Techne\ErgonX\migracao\docs-legado\v7x\aps\fontes_ergon\pck_peraqfer.pkb`<br>`D:\Developer\Techne\ErgonX\migracao\docs-legado\v7x\aps\fontes_ergon\pck_ferias.pkb`<br>`D:\Developer\Techne\ErgonX\migracao\docs-legado\v7x\aps\fontes_ergon\pck_erg_abonos_ferias.pkb` | `PCK_PARAMETROS_CONTA.MAIN_PRE` / `MAIN_POS`: invariantes de limites de dias, parcelas, abono, contagem em dobro, licença especial, progressão, tempo de serviço, sexta-parte e não-concomitância de vigência.<br>`PCK_PERAQFER.MAIN_PRE` / `MAIN_POS` / `VALID_PERAQFER` / `VERIFICA_DT_PRESCR`: cronologia, vínculo ativo, bloqueio de averbação, teto e redução de dias de direito, consistência dos overrides e prescrição.<br>`PCK_FERIAS.MAIN_PRE` / `MAIN_POS` / `VALID_FERIAS` e `PCK_ERG_ABONOS_FERIAS.MAIN_PRE`: elegibilidade de férias e benefícios financeiros, antecedência do PA por ordem (1º/2º/3º/>3º), prescrição, limites de parcelas/dias, abono pecuniário, substituição e saldo residual. |
| **Hop 3** | **Packages Auxiliares e Resolução de Contexto** | `PCK_PERAQFER.GET_PARAMETROS_CONTA`, `PCK_FERIAS.GET_PARAMETROS_CONTA` (Tarefa 24610), `PCK_FERIAS.ORDEM_PERAQFER`, `PCK_FERIAS.VERIFICA_AFAST`, `PCK_VINCULOS.DATA_EXERCICIO`, `PCK_VINCULOS.DATA_VACANCIA`, `PACK_HADES.GET_OPCAO` | Resolução do `PARAMETROS_CONTA` vigente mais específico para `(EMP_CODIGO, REGIMEJUR, TIPOVINC, CATEGORIA, SUBCATEGORIA)` na data de referência (`PA.DTFIM` / `FERIAS.DTINI`), aplicação do **Merge de Precedência (`PARAMETROS_CONTA (+) PERAQFER -> FERIAS`)**, cálculo da ordem ordinal do PA no vínculo (`1º`, `2º`, `3º`, `>3º`) e verificação de conflito com `FREQUENCIAS` e `AFASTAMENTOS`. |
| **Hop 4** | **User Exits / Hooks de Cliente (`EPB__*` / `EPA__*` / `EP__*`)** | `EPB__PARAMETROS_CONTA`, `EPA__PARAMETROS_CONTA`<br>`EPB__PERAQFER`, `EPA__PERAQFER`, `EP__VERIFICA_DT_PRESCR`<br>`EPB__FERIAS`, `EPA__FERIAS`, `EP__TRATA_ABONO`, `EP__ABONO_CONST`, `EP__ADIANT_DECTERC`, `EP__MAX_PERIOD`, `EP__DIAS_ABONO`, `EP__ORDEM_PERAQFER`<br>`EPB__ERG_ABONOS_FERIAS`, `EPA__ERG_ABONOS_FERIAS` | Pontos formais de extensão customizada por órgão/cliente antes (`PRE_DECISION`) e após (`POST_DECISION`) a decisão padrão do produto, além da substituição governada de políticas de prescrição, fracionamento e abono pecuniário. |

---

## 2. Mecanismo Canônico de Merge com Precedência (`PARAMETROS_CONTA (+) PERAQFER -> FERIAS` — Tarefa 24610)

No Ergon legado (`PCK_PERAQFER.GET_PARAMETROS_CONTA`, linhas 354–476, e `PCK_FERIAS.GET_PARAMETROS_CONTA`, linhas 534–645), a validação de `PERAQFER` e de `FERIAS` nunca lê isoladamente apenas a tabela `PARAMETROS_CONTA`. Ela executa um merge determinístico em duas etapas:

1. **Etapa 1 — Resolução por Especificidade de `ParametroContagemFerias`:**
   - Filtra registros ativos (`active = true`) cuja vigência `[dtini, dtfim]` contenha a data de referência (`pa.dtfim` quando informada, ou `ferias.dtini` / `pa.dtini`).
   - Avalia a compatibilidade com o vínculo (`empCodigo`, `regimeJuridico`, `tipoVinculo`, `categoria`, `subcategoria`), pontuando a especificidade (`empCodigo` exato > `empCodigo == null` global; `regimeJuridico`, `tipoVinculo`, `categoria`, `subcategoria` exatos > `null`/wildcard) e desempatando pela maior `dtini` e menor `id`.
2. **Etapa 2 — Aplicação dos Overrides Individuais de `PeriodoAquisitivoFerias` (`PERAQFER`):**
   - **Atributos numéricos** (`maxPerFer`, `minPerFer`, `maxDiasPerFer`, `minDiasPerFer`, `minDiasPerPrincFer`, `maxDiasVendFer`, `minDiasPvendFer`, `numDiasAbonoFer`, `denDiasAbonoFer`, `minDiasPeraqfer`, `maxDiasPeraqfer`): quando **não nulos** no `PeriodoAquisitivoFerias`, sobrescrevem o valor de `ParametroContagemFerias`; quando `null`, herdam do `ParametroContagemFerias`.
   - **Atributos tri-state (`'I'` / `'S'` / `'N'` ou `'I'` / `'D'` / `'G'` / `'N'`)**:
     - `abonoDirGozo`: `'I'` ou `null` herda de `ParametroContagemFerias.abonoDirGozo`; `'D'`, `'G'` ou `'N'` sobrescreve;
     - `abonoConstFer`: `'I'` ou `null` herda de `ParametroContagemFerias.abonoConstFer`; `'S'` (`true`) ou `'N'` (`false`) sobrescreve;
     - `adiant13Fer`: `'I'` ou `null` herda de `ParametroContagemFerias.adiant13Fer`; `'S'` (`true`) ou `'N'` (`false`) sobrescreve;
     - `adiantSalFer`: `'I'` ou `null` herda de `ParametroContagemFerias.adiantSalFer`; `'S'` (`true`) ou `'N'` (`false`) sobrescreve;
     - `emprestFer`: `'I'` ou `null` herda de `ParametroContagemFerias.emprestFer`; `'S'` (`true`) ou `'N'` (`false`) sobrescreve;
     - `contDobroFerias`: `'I'` ou `null` herda de `ParametroContagemFerias.contDobroFerias`; `'S'` (`true`) ou `'N'` (`false`) sobrescreve;
     - `permiteGozoPosPrescricao`: `'I'` ou `null` herda de `ParametroContagemFerias.permiteGozoAposPrescricao`; `'S'` (`true`) ou `'N'` (`false`) sobrescreve;
     - `permiteFerEstePer` (Tarefa 24610 — `PCK_FERIAS.PKB`, linhas 565–577): quando `'S'` ou `'N'`, sobrescreve simultaneamente as 4 flags de gozo antecipado (`permiteFerPrimPer`, `permiteFerSegunPer`, `permiteFerTercPer`, `permiteFerPerInc`); quando `'I'` ou `null`, preserva as 4 flags individuais herdadas de `ParametroContagemFerias`.

Na Plataforma Praxis, esse mecanismo é materializado em `br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver` e exposto como o nó de fatos `effectiveParametroConta` para o `PeriodoAquisitivoFeriasRuleSet`, para o `FeriasGozoRuleSet`, para `ErgonInvariants` e para `ErgonDeterminationService`.

---

## 3. Classificação Canônica das Regras Extraídas

Cada regra identificada nos 4 pacotes PL/SQL foi classificada em uma das 3 categorias arquiteturais:
- **`DB_INVARIANT`**: invariante estrutural de integridade relacional/cronológica irrevogável (`ModificationPolicy.FORBIDDEN`, `SlotPosition.PROTECTED_GUARD`).
- **`PRODUCT_DECISION`**: regra funcional padrão do produto Ergon (`ModificationPolicy.FORBIDDEN`, `PARAMETERIZABLE`, `RESTRICT_ONLY` ou `REPLACEABLE`).
- **`CUSTOMER_EXTENSION_HOOK`**: ponto de extensão formal (`EPB__*` / `EPA__*` / `EP__*`) modelado como slot `PRE_DECISION` / `POST_DECISION` (`ModificationPolicy.RESTRICT_ONLY` ou `REPLACEABLE`).

---

## 4. Matriz 4-Hops — `ParametroContagemFeriasRuleSet` (`PCK_PARAMETROS_CONTA`)

- **RuleSet Key:** `ergon.administracao-pessoal.parametros-contagem-ferias.matriz`
- **RuleParameterTemplate (Modo 2):** `ergon.parametros-conta:vacation-days-bounds@1.0` (`minDiasDireitoPadrao`, `maxDiasDireitoPadrao`, `maxParcelasTeto`)
- **Executor Java Versionado (`POST_DECISION`):** `ergon.parametros-conta:concomitancia-vigencia-overlap@1.0`

| Ordem | Slot Key | Código Legado | Origem PL/SQL (`pck_parametros_conta.pkb`) | Classificação | Posição / Política | Regra Determinística / Semântica de Negócio |
|---:|---|---|---|---|---|---|
| 1 | `parametros-conta.guard.vigencia-ordem-valida` | `ERG-01873` | `MAIN_PRE` (L687–690) | `DB_INVARIANT` | `PROTECTED_GUARD` / `FORBIDDEN` | `dtfim == null` ou `dtini <= dtfim`. A data de início da vigência não pode ser posterior à data de término. |
| 2 | `parametros-conta.hook.pre-validacao-orgao` | Extensão sem código de produto | `EPB__PARAMETROS_CONTA` (L47) | `CUSTOMER_EXTENSION_HOOK` | `PRE_DECISION` / `RESTRICT_ONLY` | Hook pré-decisão para restrições estatutárias específicas do órgão na parametrização de contagem. |
| 3 | `parametros-conta.core.prescricao-e-flags-gozo` | `ERG-01870` (`ERG-01871`, `ERG-01872`, `ERG-00099`) | `MAIN_PRE` (L658–685) | `PRODUCT_DECISION` | `CORE_DECISION` / `FORBIDDEN` | `criterioPrescricao` deve pertencer a `['I', 'F']` e indicadores booleanos de prescrição/concomitância devem ser íntegros. |
| 4 | `parametros-conta.core.ferias-habilitadas-obrigatorios` | `ERG-01800` | `MAIN_PRE` (L692–707) | `PRODUCT_DECISION` | `CORE_DECISION` / `FORBIDDEN` | Quando `temFerias == true`, os atributos `totDiasFer`, `maxPerFer`, `minPerFer`, `maxDiasPerFer` e `minDiasPerFer` são obrigatórios e `totDiasFer > 0`. |
| 5 | `parametros-conta.core.ferias-limites-dias-template` | `ERG-01806` | `MAIN_PRE` (L749–761) | `PRODUCT_DECISION` | `CORE_DECISION` / `PARAMETERIZABLE` | Parametriza via `ergon.parametros-conta:vacation-days-bounds@1.0` os limites corporativos de `totDiasFer` (`minDiasDireitoPadrao..maxDiasDireitoPadrao`) e `maxPerFer <= maxParcelasTeto`. |
| 6 | `parametros-conta.core.parcelamento-coerencia` | `ERG-01809` (`ERG-01810`, `ERG-01851`, `ERG-01852`) | `MAIN_PRE` (L789–812) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Quando `temFerias == true`: `minPerFer <= maxPerFer`, `minDiasPerFer <= maxDiasPerFer`, `maxPerFer <= totDiasFer`, `maxDiasPerFer <= totDiasFer` e `minDiasPerPrincFer >= minDiasPerFer`. |
| 7 | `parametros-conta.core.abono-pecuniario-fracao-limites` | `ERG-01802` (`ERG-01801`, `ERG-01803`, `ERG-01804`, `ERG-01805`, `ERG-01807`, `ERG-01808`) | `MAIN_PRE` (L709–788) | `PRODUCT_DECISION` | `CORE_DECISION` / `REPLACEABLE` | `abonoDirGozo` em `['D', 'G', 'N']`; se `'D'` ou `'G'`, exige `numDiasAbonoFer >= 0`, `denDiasAbonoFer > 0`, `numDiasAbonoFer <= denDiasAbonoFer`; se limites de venda informados, `minDiasPvendFer <= maxDiasVendFer <= totDiasFer`. |
| 8 | `parametros-conta.core.ferias-desabilitadas-sem-limites` | `ERG-01811` (`ERG-01812`) | `MAIN_PRE` (L814–837) | `PRODUCT_DECISION` | `CORE_DECISION` / `FORBIDDEN` | Quando `temFerias == false`, veda preenchimento positivo de `totDiasFer`, `maxPerFer`, `minPerFer`, `maxDiasPerFer`, `minDiasPerFer`, `minDiasPvendFer` e `maxDiasVendFer`. |
| 9 | `parametros-conta.core.contagem-dobro-ferias-coerencia` | `ERG-01853` (`ERG-01854`, `ERG-01855`, `ERG-01856`, `ERG-00604`, `ERG-00605`, `ERG-00606`) | `MAIN_PRE` (L598–655) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Se `contDobroFerias == true`, exige `minDiasPeraqfer != null`, `maxDiasPeraqfer != null` e `0 <= minDiasPeraqfer <= maxDiasPeraqfer`; se `contDobroFerias == false`, veda `minDiasPeraqfer` e `maxDiasPeraqfer`. |
| 10 | `parametros-conta.core.licenca-especial-progressao-coerencia` | `ERG-01813` (`ERG-01814`..`ERG-01828`) | `MAIN_PRE` (L843–1014) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Coerência dos parâmetros de Licença Especial (`temLicEsp`, `anosLicEsp`, `mesesLicEspPer`, fração de abono, contagem em dobro e venda), Progressão (`temProgressao`, `anosProgressao`), Tempo de Serviço (`temTempoServ`, `anos1TempoServ`) e Sexta-Parte (`temSextaParte`, `anosSextaParte`). |
| 11 | `parametros-conta.post.concomitancia-vigencia-escopo` | `ERG-01850` (`ERG-01874`) | `MAIN_POS` (L1035–1160) | `DB_INVARIANT` | `POST_DECISION` / `FORBIDDEN` | Executor Java `ergon.parametros-conta:concomitancia-vigencia-overlap@1.0`: impede vigências ativas sobrepostas `[dtini, dtfim]` para a mesma chave lógica `(empCodigo, regimeJuridico, tipoVinculo, categoria, subcategoria)`. |
| 12 | `parametros-conta.hook.pos-validacao-orgao` | Extensão sem código de produto | `EPA__PARAMETROS_CONTA` (L463) | `CUSTOMER_EXTENSION_HOOK` | `POST_DECISION` / `RESTRICT_ONLY` | Hook pós-decisão para governança complementar do órgão. |

---

## 5. Matriz 4-Hops — `PeriodoAquisitivoFeriasRuleSet` (`PCK_PERAQFER`)

- **RuleSet Key:** `ergon.administracao-pessoal.periodos-aquisitivos-ferias.aquisicao`
- **RuleParameterTemplate (Modo 2):** `ergon.peraqfer:accumulation-and-reduction-bounds@1.0` (`maxPeriodosAcumuladosAbertos`, `maxFaltasSemPerdaTotal`, `diasDireitoPadrao`)
- **Executor Java Versionado (`POST_DECISION`):** `ergon.peraqfer:sobreposicao-e-acumulo-periodos@1.0`

| Ordem | Slot Key | Código Legado | Origem PL/SQL (`pck_peraqfer.pkb`) | Classificação | Posição / Política | Regra Determinística / Semântica de Negócio |
|---:|---|---|---|---|---|---|
| 1 | `peraqfer.guard.cronologia-datas-valida` | `ERG-01851` (`ERG-00099`) | `MAIN_PRE` (L911–952) | `DB_INVARIANT` | `PROTECTED_GUARD` / `FORBIDDEN` | `dtini <= dtfim`, `dtPrescricao >= dtfim` (quando informada) e `dtPgtoComMulta >= dtfim` (quando informada). |
| 2 | `peraqfer.guard.vinculo-exercicio-vacancia` | `ERG-01850` | `MAIN_PRE` (L921–936) | `DB_INVARIANT` | `PROTECTED_GUARD` / `FORBIDDEN` | `dtini >= vinculo.dtexerc` (salvo `geradoPorAverbacao == true`) e anterior a `vinculo.dtvac` / `vinculo.dtaposent`. |
| 3 | `peraqfer.hook.pre-validacao-orgao` | Extensão sem código de produto | `EPB__PERAQFER` (L265) | `CUSTOMER_EXTENSION_HOOK` | `PRE_DECISION` / `RESTRICT_ONLY` | Hook pré-decisão para regras específicas de abertura de período aquisitivo por órgão. |
| 4 | `peraqfer.core.bloqueio-averbacao-e-criterio-prescricao` | `ERG-00099` (`ERG-00603`) | `MAIN_PRE` (L887–910, L954–975) | `PRODUCT_DECISION` | `CORE_DECISION` / `FORBIDDEN` | Protege registros `geradoPorAverbacao == true` contra alteração manual indevida (`lockedAverbacaoMutation`) e valida `criterioPrescricao` em `['I', 'F']` e `semPgtoMulta` em `['I', 'S', 'N']`. |
| 5 | `peraqfer.core.direito-teto-e-reducao-faltas` | `ERG-00600` | `VALID_PERAQFER` (L497–516) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | `totDiasDireito <= effectiveParametroConta.totDiasFer`, respeita a escala progressiva de redução por `faltasApuradas` (`0..5 -> 30`, `6..14 -> 24`, `15..23 -> 18`, `24..32 -> 12`, `>32 -> 0`) e garante `totDiasDireito >= diasConsumidos` (`diasGozados + diasVendidosAbono + diasConvertidosDobro`). |
| 6 | `peraqfer.core.acumulo-e-faltas-limites-template` | `ERG-00612` | `VALID_PERAQFER` / Governança de Acúmulo | `PRODUCT_DECISION` | `CORE_DECISION` / `PARAMETERIZABLE` | Parametriza via `ergon.peraqfer:accumulation-and-reduction-bounds@1.0` o limite máximo de períodos aquisitivos abertos acumulados (`openAccumulatedPeriodsCount <= maxPeriodosAcumuladosAbertos`) e o teto de faltas (`faltasApuradas <= maxFaltasSemPerdaTotal` quando `totDiasDireito > 0`). |
| 7 | `peraqfer.core.overrides-contagem-dobro` | `ERG-00604` (`ERG-00605`..`ERG-00610`) | `MAIN_PRE` (L978–1034) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Coerência entre o override `contDobroFerias` (`'I'`, `'S'`, `'N'`) e `minDiasPeraqfer` / `maxDiasPeraqfer`, além da validação de `diasConvertidosDobro` dentro da faixa efetiva `[minDiasPeraqfer, maxDiasPeraqfer]`. |
| 8 | `peraqfer.core.overrides-parcelamento-e-abono` | `ERG-01809` (`ERG-01801`..`ERG-01810`) | `VALID_PERAQFER` (L518–608) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Valida o resultado efetivo do merge `PARAMETROS_CONTA (+) PERAQFER`. A modalidade efetiva de abono deve ser `D`, `G` ou `N` (`ERG-01801`, `pck_peraqfer.pkb` L424–425). O domínio do valor informado no período é verificado separadamente por `ERG-01827`. Também valida `minPerFer <= maxPerFer <= totDiasDireito`, `minDiasPerFer <= maxDiasPerFer <= totDiasDireito`, `minDiasPerPrincFer >= minDiasPerFer`, fração `numDiasAbonoFer / denDiasAbonoFer` e `minDiasPvendFer <= maxDiasVendFer <= totDiasDireito`. |
| 9 | `peraqfer.core.prescricao-parcelas-ativas` | `ERG-00615` | `VERIFICA_DT_PRESCR` (L681–779) | `PRODUCT_DECISION` | `CORE_DECISION` / `REPLACEABLE` | Quando `effectiveParametroConta.permiteGozoAposPrescricao == false` e `dtPrescricao != null`, nenhuma parcela ativa de `FeriasGozo` do período pode ultrapassar `dtPrescricao` (avaliando `dtini` quando `criterioPrescricao == 'I'` ou `dtfim`/`dtprevfim` quando `criterioPrescricao == 'F'`). |
| 10 | `peraqfer.post.sobreposicao-e-acumulo-periodos` | `ERG-00601` | `MAIN_POS` (L1044–1077) | `DB_INVARIANT` | `POST_DECISION` / `FORBIDDEN` | Executor Java `ergon.peraqfer:sobreposicao-e-acumulo-periodos@1.0`: impede sobreposição de intervalo `[dtini, dtfim]` ou duplicidade de `chave` entre períodos aquisitivos ativos do mesmo `(numfunc, numvinc)`. |
| 11 | `peraqfer.hook.pos-validacao-orgao` | Extensão sem código de produto | `EPA__PERAQFER` (L594) | `CUSTOMER_EXTENSION_HOOK` | `POST_DECISION` / `RESTRICT_ONLY` | Hook pós-decisão para validações complementares do órgão sobre o período aquisitivo. |

---

## 6. Matriz 4-Hops — `FeriasGozoRuleSet` (`PCK_FERIAS` + `PCK_ERG_ABONOS_FERIAS`)

- **RuleSet Key:** `ergon.administracao-pessoal.ferias-gozo.concessao`
- **RuleParameterTemplate (Modo 2):** `ergon.ferias:fracionamento-and-abono-bounds@1.0` (`maxParcelasPermitidas`, `minDiasParcela`, `minDiasParcelaPrincipal`, `maxDiasAbonoPecuniario`, `antecedenciaMinimaDias`)
- **Executor Java Versionado (`POST_DECISION`):** `ergon.ferias:concomitancia-e-saldo-fracionamento@1.0`

| Ordem | Slot Key | Código Legado | Origem PL/SQL (`pck_ferias.pkb` / `pck_erg_abonos_ferias.pkb`) | Classificação | Posição / Política | Regra Determinística / Semântica de Negócio |
|---:|---|---|---|---|---|---|
| 1 | `ferias-gozo.guard.cronologia-e-interrupcao-valida` | `VALID_FERIAS` (`ERG-00501`) | `VALID_FERIAS` (L911–980) | `DB_INVARIANT` | `PROTECTED_GUARD` / `FORBIDDEN` | `dtini <= dtprevfim`, `dtini <= dtfim` (quando informada), `dtInterrupcao` dentro de `[dtini, dtprevfim]`, `diasGozo > 0` e coerência com o período aquisitivo vinculado. |
| 2 | `ferias-gozo.guard.vinculo-exercicio-vacancia-frequencia` | `VALID_FERIAS` | `VALID_FERIAS` (L982–1095) / `VERIFICA_AFAST` (L740–790) | `DB_INVARIANT` | `PROTECTED_GUARD` / `FORBIDDEN` | Gozo dentro da vigência do `Vinculo` (`>= dtexerc` e anterior a `dtvac`/`dtaposent`) e sem conflito com `Frequencia` normal (`overlappingNormalFrequenciasCount == 0`) ou `Afastamento` impeditivo (`overlappingAfastamentosCount == 0`). |
| 3 | `ferias-gozo.hook.pre-validacao-orgao` | Extensão sem código de produto | `EPB__FERIAS` (L91) | `CUSTOMER_EXTENSION_HOOK` | `PRE_DECISION` / `RESTRICT_ONLY` | Hook pré-decisão para restrições de escala de férias específicas do órgão. |
| 4 | `ferias-gozo.core.direito-ferias-habilitado` | `ERG-00506` | `MAIN_PRE` (L1281–1286) | `PRODUCT_DECISION` | `CORE_DECISION` / `FORBIDDEN` | `effectiveParametroConta.temFerias == true` e período aquisitivo em situação apta à concessão (não `CANCELADO` nem `PRESCRITO`). |
| 5 | `ferias-gozo.core.substituto-funcional-valido` | `ERG-00502` (`ERG-00503`, `ERG-00504`) | `MAIN_PRE` (L1170–1272) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Par `(numfuncSubstituto, numvincSubstituto)` completo, vedada autodesignação (`numfuncSubstituto != numfunc`), vínculo substituto ativo no mesmo órgão e proteção de substituição ativa no período. |
| 6 | `ferias-gozo.core.beneficios-financeiros-elegibilidade` | `ERG-00507` (`ERG-00508`, `ERG-00509`, `ERG-00510`, `ERG-00512`, `ERG-00513`) | `MAIN_PRE` (L1288–1435) | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Verifica permissão efetiva (`PARAMETROS_CONTA (+) PERAQFER`) para `adiantamento13` (`ERG-00507` e unicidade no ano civil `ERG-00513`), `adiantamentoSalario` (`ERG-00508`), `emprestimoFerias` (`ERG-00509`) e `abonoConstitucional` (`ERG-00510` e unicidade no PA `ERG-00512`). |
| 7 | `ferias-gozo.core.gozo-antes-termino-pa-por-ordem` | `ERG-00516` (`ERG-00527`, `ERG-00528`, `ERG-00530`, `ERG-00517`) | `MAIN_PRE` (L1437–1506) / `ORDEM_PERAQFER` | `PRODUCT_DECISION` | `CORE_DECISION` / `RESTRICT_ONLY` | Quando `effectiveParametroConta.permiteGozoAntesPaFer == false`: veda `dtini < pa.dtini` (`ERG-00516`) e, para `dtini < pa.dtfim`, exige permissão específica conforme a ordem do PA no vínculo (`1º PA -> permiteFerPrimPer` `ERG-00527`, `2º PA -> permiteFerSegunPer` `ERG-00528`, `3º PA -> permiteFerTercPer` `ERG-00530`, `>3º PA -> permiteFerPerInc` `ERG-00517`). |
| 8 | `ferias-gozo.core.gozo-apos-prescricao-pa` | `ERG-01880` | `MAIN_PRE` (L1508–1536) | `PRODUCT_DECISION` | `CORE_DECISION` / `REPLACEABLE` | Quando `effectiveParametroConta.permiteGozoAposPrescricao == false` e `pa.dtPrescricao != null`, veda gozo após `dtPrescricao` (avaliando `dtini > dtPrescricao` para critério `'I'` ou `dtfimEfetivo > dtPrescricao` para critério `'F'`). |
| 9 | `ferias-gozo.core.fracionamento-limites-template` | `ERG-00519` (`ERG-00518`, `ERG-00520`) | `MAIN_PRE` (L1538–1585) | `PRODUCT_DECISION` | `CORE_DECISION` / `PARAMETERIZABLE` | Valida limites de duração da parcela (`minDiasPerFer <= diasGozo <= maxDiasPerFer`, exceto quando `situacao == 'INTERROMPIDA'`) e número máximo de parcelas no PA (`totalParcelasNoPa <= maxPerFer`), parametrizável por órgão via `ergon.ferias:fracionamento-and-abono-bounds@1.0`. |
| 10 | `ferias-gozo.core.abono-pecuniario-elegibilidade-e-limites` | `ERG-00522` (`ERG-00521`, `ERG-03311`..`ERG-03315`) | `PCK_FERIAS.MAIN_PRE` (L1587–1640) / `PCK_ERG_ABONOS_FERIAS.MAIN_PRE` | `PRODUCT_DECISION` | `CORE_DECISION` / `REPLACEABLE` | Quando `abonoPecuniario == true`: exige `effectiveParametroConta.abonoDirGozo != 'N'` (`ERG-00522`), respeita `permiteLancConcPa` quando já há abono em outra parcela do PA (`ERG-00521`), exige `iniFimAbono` em `['I', 'F']`, `diasAbonoVendidos > 0` e dentro de `[minDiasPvendFer, maxDiasVendFer]` e do teto proporcional `numDiasAbonoFer / denDiasAbonoFer`. |
| 11 | `ferias-gozo.post.concomitancia-e-saldo-fracionamento` | `ERG-00523` (`ERG-00514`, `ERG-00525`, `ERG-00526`, `ERG-00531`, `ERG-00532`) | `PCK_FERIAS.MAIN_POS` (L1690–1940, Tarefa 98160) | `DB_INVARIANT` | `POST_DECISION` / `FORBIDDEN` | Executor Java `ergon.ferias:concomitancia-e-saldo-fracionamento@1.0`: impede sobreposição de datas entre parcelas de férias do vínculo (`ERG-00514`), excesso de consumo sobre `totDiasDireito` (`ERG-00523`), impossibilidade matemática de esgotar o PA nas parcelas restantes (`ERG-00525`), saldo residual inferior a `minDiasPerFer` ou violação da parcela principal `minDiasPerPrincFer` (`ERG-00526`), quitação antes de `minPerFer` (`ERG-00531`) e saldo residual positivo na última parcela `maxPerFer` (`ERG-00532`). |
| 12 | `ferias-gozo.hook.pos-validacao-orgao` | Extensão sem código de produto | `EPA__FERIAS` (L424) | `CUSTOMER_EXTENSION_HOOK` | `POST_DECISION` / `RESTRICT_ONLY` | Hook pós-decisão para governança complementar do órgão na concessão de férias. |

---

## 7. Governança dos 4 Modos de Customização por Órgão (`empCodigo = 1` vs `empCodigo = 2`)

Todos os 3 `RuleSets` suportam e provam em suíte JUnit dedicada (`FeriasRuleEngineAndDeterminationsContractTest`) os **4 Modos Formais de Customização** do Contrato `1.6` do `praxis-rules-engine`:

1. **Modo 1 — `FORBIDDEN` (Invariante Protegida / Guard):**
   - Qualquer tentativa de substituir ou relaxar slots `ModificationPolicy.FORBIDDEN` (ex.: `parametros-conta.guard.vigencia-ordem-valida` `ERG-01873`, `peraqfer.guard.cronologia-datas-valida` `ERG-01851`, `ferias-gozo.post.concomitancia-e-saldo-fracionamento` `ERG-00523`) é rejeitada em tempo de compilação do plano pelo `DefaultRuleDecisionPlanner` com `RulePlanException` (`FORBIDDEN_MODIFICATION`).
2. **Modo 2 — `PARAMETERIZABLE` (`PARAMETERIZE` via `RuleParameterTemplate`):**
   - `ParametroContagemFeriasRuleSet`: template `ergon.parametros-conta:vacation-days-bounds@1.0`;
   - `PeriodoAquisitivoFeriasRuleSet`: template `ergon.peraqfer:accumulation-and-reduction-bounds@1.0` (ex.: Órgão 2 reduz `maxPeriodosAcumuladosAbertos` de `2` para `1`);
   - `FeriasGozoRuleSet`: template `ergon.ferias:fracionamento-and-abono-bounds@1.0` (ex.: Órgão 2 exige `minDiasParcela = 10`, `minDiasParcelaPrincipal = 15`, `maxParcelasPermitidas = 2`).
3. **Modo 3 — `RESTRICT_ONLY` (`AUGMENT`):**
   - Permite ao Órgão 2 adicionar restrições cumulativas (`ConflictResolutionStrategy.FIRST_DENY_WINS`) nos hooks `PRE_DECISION`/`POST_DECISION` ou slots `RESTRICT_ONLY` (ex.: exigir `documentoLegalId != null` na concessão de férias ou na abertura manual de PA), sem jamais afrouxar a regra base do produto.
4. **Modo 4 — `REPLACEABLE` (`REPLACE_EXACT`):**
   - Permite ao Órgão 2 substituir controlada e auditavelmente slots `ModificationPolicy.REPLACEABLE` (como a política de prescrição `peraqfer.core.prescricao-parcelas-ativas` ou a política de abono pecuniário `ferias-gozo.core.abono-pecuniario-elegibilidade-e-limites`), preservando rastreabilidade de `DecisionSource.CUSTOMER` no diagnóstico de avaliação.
