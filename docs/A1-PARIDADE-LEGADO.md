# A1 — Paridade das entradas e regras por órgão

Revisão de 3 de novembro de 2026 sobre os fluxos dos Slices 1 a 4 no Showcase. A lógica de acumulação da ERG-00300 e suas fontes de horário pertencem à P5-C.

| Entrada | Contrato resultante | Evidência primária |
| --- | --- | --- |
| Dependente em cadastro e manutenção | Bean Validation aceita o dia corrente. O motor rejeita nascimento futuro com ERG-03182 e decide sobre cronologia, RG e certidão. O encerramento coletivo também usa a regra de vigência. | `fontes_ergon/pck_dependentes.pkb`, `VALID_DEPENDENTES`, linhas 854 a 860. |
| Cadastro de vínculo e comandos de cessão, retorno, aposentadoria e estágio | Exercício programado pode ser informado no cadastro. A cronologia dos comandos individuais e da homologação coletiva chega ao motor. | `fontes_ergon/pck_vinculos.pkb`, `VALID_VINCULOS`, linhas 3207 a 3212 para a ordem posse e exercício. |
| Cadastro e concessão de afastamento pelo vínculo | Motivo complementar pode ser omitido. A origem permite inferir prorrogação quando a flag está ausente. Registrar e encerrar usam o contexto do serviço, incluindo a regra estatutária e a concomitância. Término previsto invertido gera ERG-04313. Término efetivo invertido gera ERG-04312. | `lic_afast.tab`, linha 11, e `fontes_ergon/pck_lic_afast.pkb`, `MAIN_PRE`, linhas 158 a 164. |
| Frequência | A chamada que pede avaliação a partir de `ErgonInvariants` delega ao contexto do serviço, assim como os fluxos que já avaliavam no serviço. | `fontes_ergon/pck_frequencias.pkb` e `fontes_ergon/pck_regras_freq.pkb`. |
| Cargo em cadastro, edição e extinção | Flags omitidas assumem false na escrita. Quantidade, ocupação, aglutinação, extinção e documento são avaliados pelo plano do órgão. O Órgão 2 exige documento legal no controle de vagas e na extinção. | `cargos_.tab`, linhas 10 a 13, e `fontes_ergon/pck_cargos.pkb`, `MAIN_PRE`, linhas 193 a 223. |
| Cadastro civil de servidor | Datas, faixa etária, ano do primeiro emprego, expedição de RG, par de registro profissional e unicidade de CPF são avaliados no motor. Duplicidade mantém HTTP 409. O plano do Órgão 2 exige PIS/PASEP. | `fontes_ergon/pck_funcionarios.pkb`, `MAIN_PRE`, linhas 205 a 209, e `VALID_B_I_FUNC`, linhas 727 a 737. |

As referências acima são relativas a `docs-legado/v7x/aps`. O PL/SQL de Cargo distingue controle livre, numerado e por quadro. O default booleano e as políticas de ocupação e aglutinação descrevem o modelo do Showcase. O cadastro civil disponível representa o primeiro emprego por ano e a CNH por validade, sem data de expedição. As idades aplicam os defaults do legado de 10 e 100 anos e preservam o último dia do mês na projeção da data de aniversário. Regras específicas do host e ocorrências do código genérico 99 receberam identificadores próprios para manter o catálogo sem colisões.

O catálogo e os testes distinguem as regras de produto da restrição adicional do Órgão 2. A customização de Cargo e ServidorPessoa é demonstrada pelos planos do Showcase. A comparação com o legado nesta entrega usa leitura do PL/SQL e testes locais com entradas equivalentes.

## Auditoria estática

Comando `python scripts/workspace/audit-java-resource-completeness.py praxis-ergon-showcase --strict`. Os seis recursos alterados passaram. O resultado global foi 26/30, com lacunas de capacidades em Designação, Função, Substituição e Vaga de Função do Slice 7.

| Recurso | Colunas cobertas | Quick View | Identidade rica | Referências com prévia | Microvisualizações | Filtros / operadores | Quick filters | Intents / ações | Bulk campos / operações | Fatos | Analytics | Determinações | Problemas editoriais |
| --- | --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Afastamento | 18/18 (100%) | 6 | Sim | 16 | 2 | 30 / 16 | 5 | 2 / 6 | 8 / 3 | 3 | 5 | 1 | 0 |
| Cargo | 17/17 (100%) | 5 | Sim | 5 | 1 | 16 / 8 | 4 | 0 / 2 | 5 / 2 | 2 | 5 | 1 | 0 |
| Dependente | 32/32 (100%) | 6 | Sim | 7 | 2 | 29 / 16 | 5 | 2 / 4 | 26 / 3 | 3 | 5 | 1 | 0 |
| Frequência | 14/14 (100%) | 6 | Sim | 11 | 1 | 15 / 8 | 4 | 2 / 5 | 6 / 3 | 4 | 5 | 1 | 0 |
| ServidorPessoa | 68/68 (100%) | 5 | Sim | 2 | 1 | 26 / 10 | 2 | 1 / 0 | 7 / 2 | 2 | 5 | 1 | 0 |
| Vínculo | 59/59 (100%) | 6 | Sim | 14 | 4 | 33 / 12 | 6 | 2 / 9 | 10 / 4 | 4 | 5 | 1 | 0 |

## Prova focal

Maven offline com Java 21 em cópia isolada do projeto. As 12 classes focais totalizaram **147 testes aprovados**, sem falhas, erros ou skips. A bateria inclui o gate nativo de conformidade, o contrato HTTP de violações, a exaustividade do catálogo, a validação de DTOs, os comandos de vínculo e extinção de cargo e os planos do Órgão 1 e do Órgão 2. Os testes HTTP de violações e os testes dos comandos são provas locais. A paridade comparada ao PL/SQL não foi executada contra Oracle nem contra um ambiente Neon.

## Correções da revisão do A1

Os comandos que atualizam o afastamento e o vínculo usam a ordem de bloqueio vínculo → afastamento. A prorrogação no próprio registro é reconhecida a partir do estado persistido quando o registro tem a flag de prorrogação e não tem uma origem separada. Essa combinação só pode ser criada pelo comando de prorrogação. Os comandos próprios de prorrogação e encerramento submetem a cronologia ao motor. A tentativa de repetir ou reduzir uma previsão vigente gera `CERG-04319`.

O comando de extinção recebe `documentoLegalId` opcional no mesmo pedido e reusa o documento já vinculado quando o campo é omitido. As definições de Cargo e ServidorPessoa têm registro por órgão na fachada, seguindo o padrão dos demais recursos. Os planos de demonstração do Órgão 2 são registrados na inicialização.

Para reproduzir as opções do legado no cadastro civil, configure `ergon.cadastro-servidor.por-orgao.<orgao>.idade-minima` e `ergon.cadastro-servidor.por-orgao.<orgao>.idade-maxima-func`. Os defaults são 10 e 100. O teto usa `dtCertFim` quando informada, conforme `PCK_FUNCIONARIOS.VALID_B_I_FUNC`. A nova coluna nullable `funcionarios.dt_cert_fim` corresponde à data de óbito ou desaparecimento do legado e requer a migração V10 nos ambientes que carregam o novo mapeamento.

### Fixture da prova PostgreSQL

A prova `A1AfastamentoNeonTest` usa o afastamento 1 do vínculo 1003:1 na cópia descartável da base de provas. Aplique V10 na cópia e cadastre a regra estatutária da fixture com o papel de provisionamento antes de executar a classe com o papel de runtime. A prova usa `A1_NEON_PROOF=true`, `PROOF_NEON_BRANCH`, `BULK_PROOF_DISPOSABLE_BRANCH` e `BULK_PROOF_EXPECTED_HOST`, conforme o isolamento descrito no README.

```sql
INSERT INTO regras_frequencia_estatutaria (
  id, emp_codigo, regime_juridico, tipo_vinculo, categoria, subcategoria,
  tipofreq, codfreq, dtini, qtd_min_dias, qtd_max_dias, rule_key, documento_legal_id
)
SELECT 95031, v.emp_codigo, v.regimejur, v.tipovinc, v.categoria, v.subcategoria,
       'LICENCA', 201, DATE '2026-01-01', 1, 90, 'a1-neon-prorrogacao', 7
FROM vinculos v
WHERE v.numfunc = 1003 AND v.numero = 1 AND v.emp_codigo = 1;
```

O teste usa a autoridade `ERGON_ACCUMULATION_CHECK` e o órgão autorizado 1 para que o comando do vínculo avalie a P5-C pela política normal. A comparação de acumulação e a configuração de produção permanecem na frente P5-C.

A prova passou no PostgreSQL 17.11, na branch `br-purple-wave-b7zdb9i3`. A primeira transação reteve o lock do vínculo. O observador confirmou via `pg_blocking_pids` que a segunda transação aguardava a primeira. O comando pelo vínculo encerrou o afastamento após prorrogação no próprio registro. A outra transação recebeu rejeição por versão obsoleta. A leitura independente confirmou afastamento prorrogado sem origem separada, previsão e término em 09/04/2026 e vínculo `ATIVO`.

As 13 classes de teste totalizaram **150 aprovações**, incluindo a prova PostgreSQL, sem falhas, erros ou skips. O auditor estrito passou na árvore atual. A prova PostgreSQL cobre as transações dos serviços no recorte do A1.
A branch descartável foi excluída após a prova. A consulta pelo ID confirmou que ela já não existe.
