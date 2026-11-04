# Matriz de Linhagem 4-Hops — Dependentes, Vínculos e Campos Customizados (`PCK_DEPENDENTES` / `PCK_VINCULOS`)

> **Escopo Canônico:** Extração determinística e catálogo de regras de negócio para os domínios **Dependentes e Elegibilidade a Benefícios** (`administracao-pessoal.dependentes`), **Provimento, Acumulação e Ciclo de Vida de Vínculos** (`administracao-pessoal.vinculos`) e **Validação Governada JSON Logic em Campos Customizados** (`configuracao.campos-customizados`), migrados do legado Oracle PL/SQL (`PCK_DEPENDENTES`, `PCK_DEPENDENCIAS`, `PCK_VINCULOS`, `PCK_FUNCIONARIOS`, `PACK_ERGON`, `PCK_BEFORE_CERGON`, `EP__VINCULOS_DTIDADE` e `EP__FILTRA_DEPENDENTES`) para o motor declarativo e determinístico **`praxis-rules-engine` (`0.1.0-beta.17`)**.

---

## 1. Protocolo Direto de 4 Saltos (`4-Hop Lineage Protocol`)

A extração seguiu a cadeia de rastreabilidade ponta a ponta a partir dos artefatos legados em `docs-legado/v7x/aps/`:

1. **Salto 1 — Superfícies Transacionais XML (`transacoes/*.xml`):**
   - `ERGadm00217.xml` (*Cadastro de Dependentes do Servidor* — tabela `DEPENDENTES`, form legado `ERG0068.FMB`)
   - `ERGadm00229.xml` (*Dependências e Benefícios por Dependente* — tabela `DEPENDENCIAS`: IRRF, Salário-Família, Plano de Saúde e Pensão)
   - `ERGadm00104.xml` (*Vínculos Funcionais e Provimento* — tabela `VINCULOS`)
   - `ERGadm00001.xml` (*Ingresso e Dados Pessoais do Servidor* — tabela `FUNCIONARIOS`)
2. **Salto 2 — Packages de Tabela e Regras Centrais (`fontes_ergon/*.pkb`):**
   - `pck_dependentes.pkb` (`MAIN_PRE`, `MAIN_POS`, `VALID_IDADE_LIMITE`, `PRC_VALID_CPF`, `VALIDA_OBRIGATORIEDADE_CPF`, `VALIDA_SEXO_DEP`, `CONSISTE_DOC_DEP`)
   - `pck_vinculos.pkb` (`MAIN_PRE`, `MAIN_POS`, `VALID_VINCULOS`, `VALID_VINC_CONCOMIT`, `VERIFICA_TIPOVINC_VALID`, `VERIFICA_LICAFAST`, `VERIFICA_CATEG_CARGO_PROV`, `VERIFICA_TIPOVINC_COMIS`)
   - `pck_funcionarios.pkb` (`VALID_CPF`, `VALID_DTNASC`, `CONSISTE_DOCUMENTOS`)
3. **Salto 3 — Packages e Procedures Auxiliares de Domínio (`fontes_ergon/*.pkb` / `*.fnc`):**
   - `PACK_HADES.VALIDA_ITEMTAB` (`Parentesco`, `ERG_TIPO_DEPEN`, `ERG_ONUS`, `ERG_TIPO_REQUIS`, `ERG_RESSARCIMENTO`)
   - `ERG_TIPOVINC_VALID` / `TIPO_VINC` (`IDADE_MINIMA`, compatibilidade entre `REGIMEJUR`, `TIPOVINC` e `CATEGORIA`)
   - `ERGON_ERRO_PACK.TRATA_ERRO` (catálogo de códigos `ERG-00300` a `ERG-00431`, `ERG-03162` a `ERG-03296`, `ERG-21400`)
4. **Salto 4 — Pontos de Extensão e User Exits de Clientes (`fontes_c_ergon/*`):**
   - `pck_before_cergon.pkb` (`EPB__DEPENDENTES`, `EPB__DEPENDENCIAS`, `EPB__VINCULOS`, `EPB__PROVIMENTOS_EV`)
   - `pck_after_cergon.pkb` (`EPA__DEPENDENTES`, `EPA__VINCULOS`)
   - `ep__vinculos_dtidade.fnc` (customização da validação de idade mínima em `DTNOM`, `DTPOSSE`, `DTEXERC`, `DTINICONTR`, `DTCONC`)
   - `ep__filtra_dependentes.fnc` e `ep__gera_numvinc.fnc`

---

## 2. Árvore Canônica de Fatos (`Fact Schema`) e Pré-Cálculo Determinístico

Como a gramática JSON Logic do `praxis-rules-engine` opera sobre uma árvore JSON imutável e determinística (`PraxisJsonLogicEngine`), o `ErgonRuleEngineFacade` pré-calcula fatos temporais, etários e relacionais antes de invocar o `RuleEnginePlanner`:

- **Fatos de `Dependente` (`ergon.administracao-pessoal.dependentes.elegibilidade`):**
  - `draft.*`: `id`, `empCodigo`, `numfunc`, `numero`, `nome`, `dtnasc`, `sexo`, `parentesco`, `cpf`, `tipodepen`, `dtini`, `dtfim`, `depIr`, `depSalarioFamilia`, `depPlanoSaude`, `invalido`, `universitario`, `excepcional`, `pensionista`, `representanteLegal`, `numrg`, `orgaorg`, `ufrg`, `expedrg`, `tipodocCert`, `numCert`, `livroCert`, `folhaCert`, `cartorioCert`, `matriculaCert`, `documentoLegalId`, `ageYears` (idade inteira na data de referência), `referenceDate`.
  - `pessoa.*`: `present`, `numero`, `cpf`, `dtnasc`, `sexo`.
  - `tipoDependente.*`: `present`, `codigo`, `geraIr`, `geraSalarioFamilia`, `limiteIdadeAnos`, `limiteIdadeUniversitario`.
  - `documentoLegal.*`: `present`, `id`, `empCodigo`, `sameTenant`.
  - `existingDependentes`: lista de dependentes ativos do mesmo `numfunc` para verificação de unicidade de CPF (`JAVA` executor).
- **Fatos de `Vinculo` (`ergon.administracao-pessoal.vinculos.provimento`):**
  - `draft.*`: `numfunc`, `numero`, `empCodigo`, `matric`, `regimejur`, `tipovinc`, `categoria`, `cargo`, `cargoComissao`, `setor`, `setorExercicio`, `situacao`, `dtnom`, `dtposse`, `dtexerc`, `dtvac`, `formavac`, `dtaposent`, `tipoapos`, `dtPgtoAte`, `dtinicontr`, `dtfimcontr`, `dtprorrogcontr`, `dtiniCessao`, `dtfimCessao`, `dtHomologEstagio`, `jornadaSemanal`, `documentoLegalId`, `atoPublicacaoId`, `creating`, `occupyingPosition`, `ageAtIngressoYears`, `daysPosseToExercicio`, `contractDurationDays`.
  - `orgao.*`: `present`, `empresa`, `ativa`.
  - `pessoa.*`: `present`, `numero`, `dtnasc`, `cpf`.
  - `cargo.*` e `cargoComissao.*`: `present`, `sameTenant`, `extinct`, `controleVaga`, `vagasAutorizadas`, `ocupadas`, `hasVacancyBalance`, `categoria`, `cargoFuncao`.
  - `setor.*` e `setorExercicio.*`: `present`, `sameTenant`.
  - `documentoLegal.*`: `present`, `sameTenant`.
  - `openAfastamentosCount`: total de afastamentos em aberto do vínculo.
  - `existingVinculos`: datas e categoria dos demais vínculos do titular, consultados na fonte autorizada para acumulação constitucional (`JAVA` executor). Ausência da fonte é inconclusiva.

---

## 3. RuleSet 4: `ergon.administracao-pessoal.dependentes.elegibilidade`
**Recurso Praxis:** `administracao-pessoal.dependentes` | **Escopo Semântico:** `workforce.dependents-and-benefits`

| Ordem | `slotKey` | Estágio | Política (`OverridePolicy`) | Cardinalidade / Combinação | Executor | Código Legado | Linhagem PL/SQL (`docs-legado`) | Regra Determinística no Produto |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 10 | `dep.periodo_vigencia_valido` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03162` | `pck_dependentes.pkb:396-412` (`DTINI < DTNASC`, `DTFIM < DTINI`, `EXPEDRG < DTNASC`) | `dtini >= dtnasc`, `(dtfim == null \|\| dtfim >= dtini)` e `(expedrg == null \|\| expedrg >= dtnasc)`. |
| 20 | `dep.data_nascimento_valida` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03182` | `pck_dependentes.pkb:560-571` (`VALID_IDADE_LIMITE`: `DTNASC > SYSDATE`, `ADD_MONTHS(DTNASC, 1440) < SYSDATE`) | `dtnasc <= referenceDate` (não futura) e `ageYears <= 120` anos. |
| 30 | `dep.coerencia_cronologica_parentesco` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03183` | `pck_dependentes.pkb:572-615` (`VALID_IDADE_LIMITE` / `PRC_VALID_CPF`: `FILHO(A)` vs `PAI/MAE` vs CPF titular) | CPF do dependente não pode coincidir com `pessoa.cpf`; quando `pessoa.dtnasc != null`, `FILHO`/`FILHA`/`ENTEADO`/`NETO` exige `dtnasc > pessoa.dtnasc` e `PAI`/`MAE`/`GENITOR`/`AVO` exige `dtnasc < pessoa.dtnasc`. |
| 40 | `dep.cpf_obrigatorio_irrf` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03185` | `pck_dependentes.pkb:620-690` (`VALIDA_OBRIGATORIEDADE_CPF`) | Dependente com `depIr == true` exige CPF válido informado (`cpf != null && cpf != ""`). |
| 50 | `dep.compatibilidade_tipo_dependente` | `DOMAIN_DECISION` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03171` | `pck_dependentes.pkb:355-395` (`PACK_HADES.VALIDA_ITEMTAB` / `TIPO_DEPENDENTES`) | Quando `tipodepen != null`, exige `tipoDependente.present == true`, `(depIr != true \|\| tipoDependente.geraIr == true)` e `(depSalarioFamilia != true \|\| tipoDependente.geraSalarioFamilia == true)`. |
| 60 | `dep.elegibilidade_etaria_beneficios` | `DOMAIN_DECISION` | `PARAMETERIZABLE` (`ergon.dependentes:benefit-age-ceilings@1.0`) | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03177` | `pck_dependentes.pkb:544-615` / `pck_dependencias.pkb` (tetos etários de IRRF, Salário-Família e Saúde com isenção por invalidez/excepcionalidade) | Tetos etários parametrizáveis por órgão (`maxAgeIrrfStandard = 21`, `maxAgeIrrfUniversity = 24`, `maxAgeFamilyAllowance = 14`, `maxAgeHealthPlan = 24`), com dispensa automática quando `invalido == true` ou `excepcional == true`. |
| 70 | `dep.exigencia_documento_invalidez_guarda` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `ERG-21400` | `pck_dependentes.pkb:710-780` (`CONSISTE_DOC_DEP` / `EPB__DEPENDENTES`) | Exige `documentoLegalId != null` válido no órgão quando `invalido == true`, `excepcional == true`, `representanteLegal == true` ou `parentesco` for `MENOR_TUTELADO` / `MENOR_SOB_GUARDA` / `CURATELADO`, além de consistência dos grupos RG (`numrg`, `orgaorg`, `ufrg`) e Certidão (`numCert`, `tipodocCert`, `cartorioCert`) e vedação de benefícios ativos quando `dtfim != null`. |
| 80 | `dep.politica_conjuge_plano_saude` | `DOMAIN_DECISION` | `REPLACEABLE` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-03198` | `pck_dependentes.pkb` / `EPB__DEPENDENCIAS` | No produto padrão, `depPlanoSaude == true` não exige `documentoLegalId` adicional para dependentes comuns; no Órgão 2 (`REPLACE_EXACT`), toda inclusão em `depPlanoSaude == true` exige `documentoLegalId` comprobatório (`CERG-03198`). |
| 90 | `dep.unicidade_cpf_dependente_servidor` | `POST_DECISION` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JAVA` (`ergon.dependentes:cpf-unicidade-servidor@1.0`) | `ERG-03193` | `pck_dependentes.pkb:640-685` (`PRC_VALID_CPF`) | Impede duplicidade de `cpf` entre dependentes do mesmo servidor (`HTTP 409 CONFLICT`). |

---

## 4. RuleSet 5: `ergon.administracao-pessoal.vinculos.provimento`
**Recurso Praxis:** `administracao-pessoal.vinculos` | **Escopo Semântico:** `workforce.employment-links`

| Ordem | `slotKey` | Estágio | Política (`OverridePolicy`) | Cardinalidade / Combinação | Executor | Código Legado | Linhagem PL/SQL (`docs-legado`) | Regra Determinística no Produto |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 10 | `vinc.cadeia_cronologica_provimento` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-00324` | `pck_vinculos.pkb:3170-3395` (`VALID_VINCULOS`: `DTNOM`, `DTPOSSE`, `DTEXERC`, `DTVAC`, `DTAPOSENT`, `DT_PGTO_ATE`, `DTINICONTR`, `DTFIMCONTR`, `DTPRORROGCONTR`) | Valida toda a cadeia temporal: `dtnom <= dtposse <= dtexerc`, `dtexerc <= dtvac`, `dtexerc <= dtaposent`, `dtvac <= dtPgtoAte`, `dtinicontr <= dtfimcontr <= dtprorrogcontr`, `dtiniCessao <= dtfimCessao`, `dtexerc <= dtiniCessao` e `dtexerc <= dtHomologEstagio`. |
| 20 | `vinc.idade_minima_constitucional_ingresso` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-00325` | `pck_vinculos.pkb:3195-3240` (`VALID_VINCULOS` / `TIPO_VINC.IDADE_MINIMA` / `ep__vinculos_dtidade.fnc`) | Exige idade mínima constitucional de `18` anos na data de posse (`dtposse`) ou exercício (`dtexerc`) quando `pessoa.dtnasc != null`. |
| 30 | `vinc.compatibilidade_regime_vinculo_cargo` | `PROTECTED_GUARD` | `FORBIDDEN` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-00354` | `pck_vinculos.pkb:3110-3130, 3510-3520, 3700-3765` (`VERIFICA_TIPOVINC_VALID`, `VERIFICA_CATEG_CARGO_PROV`, `VERIFICA_TIPOVINC_COMIS`) | Garante órgão ativo na admissão, isolamento de tenant (`setor`, `setorExercicio`, `cargo`, `cargoComissao` do mesmo `empCodigo`), vedação de cargo extinto ou sem saldo de vagas para vínculo ocupante e compatibilidade entre `regimejur` e `tipovinc` (ex.: `TEMPORARIO` não pode usar regime `ESTATUTARIO` efetivo com estágio probatório). |
| 40 | `vinc.limites_jornada_e_prazo_exercicio` | `DOMAIN_DECISION` | `PARAMETERIZABLE` (`ergon.vinculos:provimento-operational-bounds@1.0`) | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-00315` | `pck_vinculos.pkb:3195-3225` (`DTPOSSE` vs `DTEXERC` e limites de carga horária semanal por órgão) | Parâmetros por órgão: `maxDaysPosseToExercicio` (`30` dias no Órgão 1; `15` dias no Órgão 2) e `maxWeeklyHours` (`168`h no Órgão 1; `44`h no Órgão 2), além da faixa positiva `1..168`. |
| 50 | `vinc.vacancia_sem_afastamento_aberto` | `DOMAIN_DECISION` | `RESTRICT_ONLY` | `MULTIPLE` / `DENY_OVERRIDES` | `JSON_LOGIC` | `ERG-00342` | `pck_vinculos.pkb:1839-1855, 3380-3415` (`VERIFICA_LICAFAST`, `FORMAVAC`, `TIPOAPOS`) | Vacância exige `dtvac != null`, `formavac != null` e `openAfastamentosCount == 0`; aposentadoria exige `dtaposent != null`; no Órgão 2 (`AUGMENT`), provimento em cargo comissionado ou vacância exige `documentoLegalId != null` (`CERG-09401`). |
| 60 | `vinc.politica_contrato_temporario_cessao` | `DOMAIN_DECISION` | `REPLACEABLE` | `SINGLE` / `FIRST_APPLICABLE` | `JSON_LOGIC` | `ERG-00330` | `pck_vinculos.pkb:3285-3330` (`DTINICONTR`, `DTFIMCONTR`, `EPB__VINCULOS`) | Produto exige `dtinicontr` e `dtfimcontr` quando `tipovinc == "TEMPORARIO"` e `dtiniCessao`/`orgaoReq` quando `situacao == "CEDIDO"`; no Órgão 2 (`REPLACE_EXACT`), contrato `TEMPORARIO` exige `documentoLegalId != null` e duração contratual `<= 730` dias (`CERG-00330`). |
| 80 | `vinc.acumulacao_constitucional_cargos` | `POST_DECISION` | `FORBIDDEN` | `SINGLE` / `SINGLE_RESULT` | `JAVA` (`ergon.vinculos:acumulacao-constitucional-cargos@2.1`) | `ERG-00300` | `pck_vinculos.pkb:1810-1820` (`VALID_VINC_CONCOMIT` / Art. 37, XVI da CF/88) | Nega três vínculos com sobreposição efetiva ou par de categorias incompatível na vigência aplicável. A combinação que envolve professor observa a EC 138/2025. Pares elegíveis dependem de prova concreta de horários e ficam inconclusivos na fonte atual. |

---

## 5. Os 4 Modos de Customização por Órgão (`empCodigo = 1` vs `empCodigo = 2`) e Campos Customizados JSON Logic

1. **Modo 1 — `FORBIDDEN` (Guarda Constitucional / Estrutural Imutável):**
   - Slots `dep.periodo_vigencia_valido`, `dep.data_nascimento_valida`, `dep.coerencia_cronologica_parentesco`, `dep.unicidade_cpf_dependente_servidor`, `vinc.cadeia_cronologica_provimento`, `vinc.idade_minima_constitucional_ingresso`, `vinc.compatibilidade_regime_vinculo_cargo` e `vinc.acumulacao_constitucional_cargos`.
   - Tentativas de `PARAM_BIND`, `AUGMENT` ou `REPLACE_EXACT` são rejeitadas em tempo de planejamento pelo `RuleEnginePlanner` com `RulePlanException` (`OVERRIDE_FORBIDDEN`).
2. **Modo 2 — `PARAMETERIZABLE` (`SlotPatchMode.PARAM_BIND` via `RuleParameterTemplate` — Contrato 1.6):**
   - Em `DependenteElegibilidadeRuleSet`, o template `ergon.dependentes:benefit-age-ceilings@1.0` parametriza os tetos etários (`maxAgeIrrfStandard`, `maxAgeIrrfUniversity`, `maxAgeFamilyAllowance`, `maxAgeHealthPlan`). No Órgão 2 (`empCodigo = 2`), o teto de plano de saúde é reduzido para `21` anos (`maxAgeHealthPlan = 21`).
   - Em `VinculoProvimentoRuleSet`, o template `ergon.vinculos:provimento-operational-bounds@1.0` parametriza `maxDaysPosseToExercicio` (`30` no Órgão 1 vs `15` no Órgão 2) e `maxWeeklyHours` (`168` no Órgão 1 vs `44` no Órgão 2).
3. **Modo 3 — `RESTRICT_ONLY` (`SlotPatchMode.AUGMENT` com `DENY_OVERRIDES`):**
   - Em `dep.exigencia_documento_invalidez_guarda`, o Órgão 2 adiciona a regra `cerg.dep.universitario_exige_documento` (`CERG-09301`), exigindo `documentoLegalId != null` sempre que `universitario == true`.
   - Em `vinc.vacancia_sem_afastamento_aberto`, o Órgão 2 adiciona a regra `cerg.vinc.comissao_ou_vacancia_exige_documento` (`CERG-09401`), exigindo `documentoLegalId != null` sempre que `cargoComissao != null` ou `dtvac != null`.
4. **Modo 4 — `REPLACEABLE` (`SlotPatchMode.REPLACE_EXACT`):**
   - Em `dep.politica_conjuge_plano_saude`, o Órgão 2 substitui a regra padrão por `cerg.dep.plano_saude_exige_documento_orgao2` (`CERG-03198`), exigindo `documentoLegalId != null` para qualquer dependente com `depPlanoSaude == true`.
   - Em `vinc.politica_contrato_temporario_cessao`, o Órgão 2 substitui a regra padrão por `cerg.vinc.contrato_temporario_teto_bienal_orgao2` (`CERG-00330`), exigindo `documentoLegalId != null` e duração `<= 730` dias em contratos temporários.
5. **Validação Estrutural e Execução em Runtime de `DefinicaoCampoCustomizado.validationRules` (`PraxisJsonLogicEngine`):**
   - No cadastro/atualização de `DefinicaoCampoCustomizado`, qualquer expressão JSON Logic informada em `validationRules` é validada sintática e estruturalmente com `PraxisJsonLogicEngine.validate(...)` sobre as raízes `["value", "customFields", "draft", "entity"]`, rejeitando operadores desconhecidos, aridades inválidas ou variáveis malformadas com `HTTP 422 UNPROCESSABLE_ENTITY` (`ERG-21501`).
   - Na validação de `customFields` de `Vinculo`, `Afastamento`, `Frequencia`, `Dependente`, `ServidorPessoa` e `Cargo`, toda definição ativa com `validationRules` JSON Logic é avaliada em tempo real via `PraxisJsonLogicEngine.evaluateResult(...)` contra `{"value": <valorDoCampo>, "customFields": <mapa>, "draft": <entidade>}`, bloqueando com `HTTP 422 UNPROCESSABLE_ENTITY` (`ERG-21502`) quando o resultado for `false`.
