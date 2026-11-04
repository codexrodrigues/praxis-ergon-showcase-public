# Auditoria focal de paridade Ergon — Slices 1 a 4

O checkout já continha os seis RuleSets e a remoção das restrições de data nos DTOs de nascimento e exercício. A revisão preservou esse trabalho e corrigiu as lacunas encontradas nas rotas de escrita.

| Caso | Fonte e comportamento verificado |
|---|---|
| Dependente | `PCK_DEPENDENTES`, linhas 319, 586 e 858–859, e `PCK_DEPENDENCIAS`: nascimento hoje aceito, nascimento futuro rejeitado com `ERG-03182`. As regras `ERG-03162` e `ERG-21400` governam cronologia e completude. A consulta de irmãos fornece registros reais ao motor. |
| Pessoa servidora | `PCK_FUNCIONARIOS.VALID_B_I_FUNC`, linhas 729–737 e 761–766: limites de idade e regras independentes `ERG-00241` e `ERG-00242` para o ano de primeiro emprego. `funcionarios.tab` tem `ANOPRIMEMP` e `VALIDCNH`, sem data de expedição de CNH. Não foi criada uma regra dependente desse dado ausente. |
| Cargo | `PCK_CARGOS.MAIN_PRE`, linhas 204–218, e `PCK_VAGAS_CARGO`: cronologia e quadro. O modelo booleano do host usa `false` por omissão; políticas locais permanecem identificadas como `CERG-*`. O pacote disponível se chama `pck_cargos.pkb`; a tabela é `cargos_.tab`. |
| Vínculo | `PCK_VINCULOS.VALID_VINCULOS`, linhas 3174–3208, e `EP__VINCULOS_DTIDADE`: exercício programado aceito, ordem de datas avaliada no motor. Cessão, retorno, aposentadoria, estágio e vacância não precisam de validação cronológica anterior ao RuleSet. |
| Frequência e afastamento | `PCK_REGRAS_FREQ`, `PCK_FREQUENCIAS`, `PCK_LIC_AFAST`, `ERG_PM_GERALICAFAST` e `EP__VERIFICA_FREQUENCIAS`. Os serviços resolvem modalidade, regra efetiva e ocorrências relacionadas. `LIC_AFAST.MOTIVO` é opcional; texto em branco vira `null`. `PCK_LIC_AFAST` linhas 158–163 distinguem término efetivo (`ERG-04312`) de previsto (`ERG-04313`). |
| Órgão e setor | `PCK_SETORES_ERGON` disponível. O pacote solicitado `pck_empresas.pkb` não foi localizado no acervo entregue; a equivalência de regras desse pacote não é afirmada. |

O contrato alterado é do host. A ação `registrar-afastamento` aceita `atoPublicacaoId`, permite que as regras do órgão decidam concomitância em vínculos não encerrados e preserva uma cessão vigente. A ação registra licença já iniciada; o POST de afastamento admite início programado. O README e as expectativas de integração da ação foram atualizados. A busca não encontrou exemplos publicados dessa ação no corpus HTTP, na landing ou nas bibliotecas Angular que exigissem atualização.

O scorecard abaixo resume as linhas focais do auditor estrito. Colunas/DTO mostra colunas de negócio e cobertura de leitura; Escrita mostra a contabilidade das mutáveis. Lookup rico é verificado no inventário global de option sources, que não atribui uma contagem por linha.

| Recurso | Colunas/DTO | Escrita | Filtros / ops | QuickFilter | Quick View / identidade rica | Referências / preview | Microviz | Intents / ações | Bulk campos / ops | Fatos / analytics / reativas | Editorial |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Órgão | 12 / 100% | 12/12 | 11 / 7 | 2 | 5 / sim | 1/1 | 0 | 0 / 2 | 0 / 0 | 1 / 0 / 0 | 0 |
| Setor | 12 / 100% | Leitura | 7 / 5 | 2 | 5 / sim | 4/4 | 0 | 0 / 0 | 0 / 0 | 1 / 0 / 0 | 0 |
| Cargo | 17 / 100% | 17/17 | 16 / 8 | 4 | 5 / sim | 5/5 | 1 | 0 / 2 | 5 / 2 | 2 / 5 / 1 | 0 |
| Pessoa | 69 / 100% | 69/69 | 26 / 10 | 2 | 5 / sim | 2/2 | 1 | 1 / 0 | 7 / 2 | 2 / 5 / 1 | 0 |
| Vínculo | 59 / 100% | 59/59 | 33 / 12 | 6 | 6 / sim | 14/14 | 1 | 2 / 9 | 10 / 4 | 4 / 5 / 1 | 0 |
| Dependente | 32 / 100% | 32/32 | 29 / 16 | 5 | 6 / sim | 7/7 | 2 | 2 / 4 | 26 / 3 | 3 / 5 / 1 | 0 |
| Frequência | 14 / 100% | 14/14 | 15 / 8 | 4 | 6 / sim | 11/11 | 1 | 2 / 5 | 6 / 3 | 4 / 5 / 1 | 0 |
| Afastamento | 18 / 100% | 18/18 | 30 / 16 | 5 | 6 / sim | 16/16 | 2 | 2 / 6 | 8 / 3 | 3 / 5 / 1 | 0 |

**Validação local:** 147 testes focais com Java 21, sem falhas nem skips; auditor estrito com 30/30 recursos em `STATIC PASS`; `git diff --check` limpo. Os testes exercitam regras, determinações, projeção de erro HTTP e políticas distintas para os Órgãos 1 e 2. A prova HTTP usa os controladores reais em MockMvc, com fronteiras de persistência simuladas. A linhagem do catálogo é exposta na resposta como `violations[].lineage`. A auditoria não certifica equivalência exaustiva de todos os ramos PL/SQL, ativação de user exits em Oracle, transações JPA/Neon, browser ou implantação.
