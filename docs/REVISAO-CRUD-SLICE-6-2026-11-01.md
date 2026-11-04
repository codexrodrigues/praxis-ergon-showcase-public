# Revisão do CRUD — Slice 6, Fase 1

**Snapshot anterior às correções.** Os achados R9–R15 abaixo foram tratados em
[CORRECOES-CRUD-SLICE-6-R9-R15.md](CORRECOES-CRUD-SLICE-6-R9-R15.md), com testes
de regressão. Este arquivo conserva os contraexemplos e o parecer daquele
recorte; o relatório de correção registra os gates e limites atuais.

Parecer de 01/11/2026: **CHANGES REQUESTED**. O CRUD tem precondições de versão e
contabilidade básica, mas ainda aceita operações contrárias às políticas
publicadas. Não está admitido para uso corporativo com direitos reais.

Esta revisão é posterior à remoção da contenção de escrita. Inspeciona os bytes
locais dos quatro serviços, validação compartilhada, DTOs, mappers, lookup,
migração V5, radar, segurança, testes e documentação. A árvore já continha
alterações locais; não se atribuem todos os achados globais a esta entrega.
Classificação das alterações desta rodada: `docs-apenas`, com probes de revisão
fora do código de produção. Não houve correção funcional, commit ou publicação.

## Achados reproduzidos

Os IDs R9–R15 continuam a numeração da revisão anterior. As reproduções C1–C8
executam os serviços e mappers reais com o mesmo armazenamento simulado do teste
entregue. **São contraexemplos de serviço, não provas HTTP/PostgreSQL.**

### R9 — P1: venda herdada ignora a matriz geral

[TempoLicencaValidation.java:129](../src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java#L129)
recusa somente o override `N`. `I` e ausência passam sem resolver a matriz,
inclusive seus limites herdados. O contrato de
[PeriodoAquisitivoLicencaEspecialDados.java:82](../src/main/java/br/com/praxis/ergonshowcase/dto/PeriodoAquisitivoLicencaEspecialDados.java#L82)
publica expressamente `I = herdar`.

C1: período com `vendLicEsp=I`, matriz ativa e compatível com `vendLicEsp=false`,
venda de 10 dias: aceita, saldo passa de 90 para 80. A ausência da matriz também
não participa da decisão. Um cliente ou agente pode executar uma venda que a
política publicada veda.

Correção: resolver os parâmetros efetivos de licença especial no domínio do
host, com precedência individual explícita, data de referência fundamentada no
legado e negação quando faltar a decisão obrigatória. Usar a mesma resolução em
validação e futuras prévias. Testar herança positiva, negativa, ausente e limites
herdados. O legado `fontes_ergon/pck_erg_venda_licesp.pkb:174–194` consulta a
matriz, aplica overrides e recusa venda sem permissão; não autoriza substituir
essa cadeia pelo simples teste de `N`.

### R10 — P1: abono desconsidera sua vedação individual

[TempoLicencaValidation.java:109](../src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java#L109)
confere apenas a combinação flag/quantidade/posição quando a flag é falsa. Com
abono verdadeiro, não aplica `abonoDirGozoLicEsp`, sua herança ou a proporção
numerador/denominador publicada nos DTOs.

C2: `abonoDirGozoLicEsp=N`, `vendLicEsp=N`, teto de venda zero; parcela de 10 dias
com abono de 5: aceita e registra `diasVendidos=5`. A vedação própria do abono já
é suficiente para tornar o resultado incorreto, independentemente da discussão
sobre venda avulsa.

Correção: validar a política efetiva de abono antes do débito e definir sua
quantidade válida segundo a base D/G. O legado `pck_lic_esp.pkb:262–325` contém a
vedação e os cálculos, inclusive a restrição da base D. Isso é proteção da
escrita já aberta, não depende da entrega de workflow ou editor reativo.

### R11 — P1: o teto acumulado usa uma soma diferente da contabilidade

[TempoLicencaValidation.java:103](../src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java#L103)
considera somente vendas avulsas no acumulador `vendido`. O teste de teto em
`132` usa esse acumulador; o ramo de gozo nem verifica o teto. Entretanto,
`sincronizar`, em `157`, contabiliza abono e venda juntos em `diasVendidos`.

C3: teto de 10 dias, abono já registrado de 5, nova venda de 10: aceita e deixa
`diasVendidos=15`. A ordem inversa também encontra um ramo sem a verificação
comum do teto. O saldo total pode permanecer não negativo enquanto o limite
financeiro específico é violado.

Correção: usar a mesma base de consumo para o teto e para o total persistido,
excluindo a contribuição anterior do próprio item em update. Testar as duas
ordens de criação e edição. No legado, o abono gera registro de venda
(`pck_lic_esp.pkb:368–378`) e entra no teto acumulado
(`pck_erg_venda_licesp.pkb:247–258`).

### R12 — P2: mudança de política impede desfazer consumo

[TempoLicencaValidation.java:97](../src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java#L97)
e os testes de elegibilidade rodam antes de o cancelamento zerar o consumo, em
`136`. Assim, uma operação de estorno é submetida às condições de uma nova
concessão.

C4: venda válida de 10 dias; após o período passar a `vendLicEsp=N`, o PUT da
venda para `CANCELADO` retorna 422 e o saldo continua 80. C5: cancelar um gozo
após o pai ser cancelado também retorna 422. O período permite a mudança de
situação sem exigir o cancelamento prévio dos filhos, criando esse impasse.

Correção: separar criação/reativação/aumento de consumo de cancelamento e
redução. Preservar autorização, vínculo, precondição e atomicidade do estorno;
definir a regra do cancelamento do pai com filhos ativos. Não orientar o usuário
a excluir o histórico para conseguir liberar o saldo.

### R13 — P2: origem aceita no serviço contraria a seleção publicada

[PeriodoAquisitivoLicencaEspecialService.java:88](../src/main/java/br/com/praxis/ergonshowcase/service/PeriodoAquisitivoLicencaEspecialService.java#L88)
confere somente existência, órgão, pessoa e vínculo da averbação. Não distingue
uma nova origem elegível de uma referência histórica mantida.

C6: nova aquisição ligada a averbação `CANCELADO`, com `dataRemocao` preenchida
e finalidade `APOSENTADORIA`: aceita. O lookup declara que canceladas devem
servir apenas para referências históricas existentes
([OptionSourcesConfig.java:118](../src/main/java/br/com/praxis/ergonshowcase/config/OptionSourcesConfig.java#L118)).
Enviar o ID diretamente contorna essa experiência de seleção.

Correção: aplicar elegibilidade de origem na escrita para novos vínculos e
trocas de origem; conservar separadamente a política de retenção de referências
históricas. Validar a finalidade e a remoção segundo a regra de origem do
direito, sem presumir que a seleção visual autorize o vínculo.

### R14 — P2: término efetivo não resolve a quantidade contabilizada

[LicencaEspecialGozoDados.java:22](../src/main/java/br/com/praxis/ergonshowcase/dto/LicencaEspecialGozoDados.java#L22)
define `dtfim` como último dia efetivamente usufruído e mantém a previsão para
interrupções/encerramento. Mas a validação, em
[TempoLicencaValidation.java:109](../src/main/java/br/com/praxis/ergonshowcase/service/TempoLicencaValidation.java#L109),
amarra `diasGozo` exclusivamente à previsão; o recálculo ignora `dtfim`.

C7: início 01/01, previsão 10/01, término efetivo 05/01, `diasGozo=10`: aceita,
`diasGozados=10`. C7b: corrigir a quantidade para 5 mantendo a previsão original
retorna 422. Há discrepância entre o significado publicado e a conta executada.

Correção: fechar a distinção entre reserva/programação e consumo efetivo.
Definir o efeito do encerramento/interrupção no saldo e no radar. Se a quantidade
continuar sendo uma reserva, publicar essa semântica expressamente e não a
apresentar como dias já usufruídos; preservar a previsão histórica.

### R15 — P2: formulário pede ao operador que mantenha totais derivados

[PeriodoAquisitivoLicencaEspecialDados.java:37](../src/main/java/br/com/praxis/ergonshowcase/dto/PeriodoAquisitivoLicencaEspecialDados.java#L37)
publica `diasGozados`, `diasVendidos` e `saldoDias` como campos editáveis de
create/update. A escrita recusa totais divergentes e os filhos já os recalculam,
de modo que o contrato combina responsabilidade do servidor com entrada manual.

C8: criar direito de 30 dias mantendo os demais defaults retorna 422 porque o
saldo nasce em 90. O operador precisa saber ajustar a equação antes do primeiro
registro; o mesmo problema afeta um agente que envia somente o novo direito.

Correção: tornar os totais calculados responsabilidade canônica do serviço,
publicá-los como leitura/prévia, preservar a contabilidade Entity→DTO e manter
como entradas apenas os dados efetivamente autoráveis. Eventuais ajustes
administrativos de saldo precisam de operação e lastro próprios. Não acrescentar
uma fórmula exclusiva de frontend para encobrir essa divisão de responsabilidade.

## Completude e fase planejada

O [scorecard reexecutado](evidence/SLICE-6-CRUD-REVISAO-SCORECARD.md) retorna
**exit 1: 51 entradas de reprovação**: 15 recursos (quatro do Slice 6 e onze
anteriores) e 36 achados de paridade do catálogo/regras/testes. São entradas do
auditor, não 51 defeitos funcionais novos. Há 10 recursos PASS entre os 25.
O relatório histórico da entrega registrava 36 entradas; não representa o
snapshot atual do auditor. O script não foi alterado por esta revisão.

| Recurso | Colunas entidade / response / escritas | Cobertura | QuickViewCols | RichIdentity | Refs com preview | MicroViz | RichEntityLookup | EditorialIssues | Filterable / operações | QuickFilters |
|---|---|---|---:|---|---:|---:|---|---:|---|---:|
| Averbação | 30 / 32 / 30 | 100% | 4 | Sim | 7 | 1 | Declarado | 0 | 17 / 12 | 3 |
| Período licença | 33 / 35 / 33 | 100% | 5 | Sim | 8 | 1 | Declarado | 0 | 17 / 12 | 3 |
| Gozo licença | 21 / 23 / 21 | 100% | 5 | Sim | 8 | 1 | Declarado | 0 | 17 / 11 | 3 |
| Venda licença | 19 / 21 / 19 | 100% | 5 | Sim | 8 | 1 | Declarado | 0 | 17 / 10 | 3 |
| Radar tempo/licença | 19 / 21 / N/A | 100% | 4 | Sim | 8 | 1 | Reutiliza referências | 0 | 17 / 11 | 3 |

As contagens são de evidência estática. Os descriptors de lookup têm seleção,
detalhe, display, filtering e dependências declarados; isso não prova sua
materialização no Angular. O problema R13 demonstra o limite dessa prova.

| Recurso | Surfaces | Intents | WorkflowActions | BulkEditable / BulkOps | Facts | UiAnalytics | ReactiveDet | Invariantes / determinações testadas |
|---|---:|---:|---:|---|---:|---:|---:|---|
| Averbação | 1 | 0 | 0 | 0 / 0 | 1 | 0 | 0 | CRUD/versionamento e dependências; sem determinação |
| Período licença | 1 | 0 | 0 | 0 / 0 | 1 | 0 | 0 | CRUD/versionamento, totais e dependências; R13/R15 abertos |
| Gozo licença | 1 | 0 | 0 | 0 / 0 | 1 | 0 | 0 | Consumo e antecipação focais; R10–R12/R14 abertos |
| Venda licença | 1 | 0 | 0 | 0 / 0 | 1 | 0 | 0 | Consumo/limites focais; R9/R11/R12 abertos |
| Radar tempo/licença | 1 | 0 | 0 | N/A | 1 | 0 | 0 | Consulta/metadados locais; sem prova SQL integrada |

Bulk, intents, analytics, workflow e determinações continuam nas Fases 2/3
planejadas. Os R9–R15 dizem respeito às operações e aos significados que já
estão publicados; não são substituídos por esse adiamento. A decisão documental
de duplicate-draft continua sem pendências ou divergências no inventário.

## Uso corporativo e riscos ainda sem prova integrada

- **Autorização por órgão:** `TenantContext` lê `X-Tenant` e o perfil do showcase
  concede autoridades globais. Não há vínculo entre esse seletor e os órgãos
  permitidos à identidade. Isso é limite explícito da demonstração, não isolamento
  multitenant admitido. Produção exige decisão de autorização do host antes do
  escopo de dados, com provas de duas identidades e dois órgãos.
- **Auditoria administrativa:** `AuditoriaErgon` registra timestamps; não substitui
  autoria/motivo/ato e histórico de cancelamento, desaverbação ou exclusão. A posição
  consolidada de desaverbação continua com a limitação já registrada em R7.
- **Concorrência:** update bloqueia o filho e depois o pai; delete bloqueia o pai
  e o flush posterior remove o filho. A ordem oposta pode formar deadlock em um
  update e delete concorrentes da mesma parcela. É risco pela inspeção da ordem
  de aquisição; não foi reproduzido em PostgreSQL. Padronizar a ordem do agregado
  e provar duas transações concorrentes, versão, rollback e tratamento do conflito.
- **Escala:** cada sincronização materializa listas de parcelas. Delete em lote
  recalcula após cada item. Para muitos filhos do mesmo pai, o volume acumulado
  de leitura pode ser quadrático. Medir cenário corporativo e considerar SUMs e
  recálculo por agregado, preservando todas as guardas e a transação.
- **UX:** contadores editáveis, previsão versus fruição e recuperação do 422
  precisam de correção semântica antes de polimento. Validar depois criação com
  direito não padrão, abono vedado, interrupção, cancelamento após troca de política
  e recuperação de 412 sem perda do rascunho, em desktop e viewport estreito.

## Evidências executadas e fronteiras

| Gate | Resultado desta revisão |
|---|---|
| `mvn test "-Dtest=!*Neon*"`, Java 21 | 233 testes, zero falhas/erros/ignorados; BUILD SUCCESS |
| Auditor de completude `--strict` | Exit 1; 51 entradas; zero editorial no Slice 6; duplicate-draft coerente |
| Probes C1–C8 | Contraexemplos acima reproduzidos em serviços/mappers reais com mocks |
| Metamodel Hibernate 6.6.53.Final | Mapeamentos construídos sem conexão; `IS_FALSE` no override convertido aceito na construção da query |
| HTTP/autorização integrada | Não executado |
| V5, constraints, concorrência e rollback PostgreSQL | Não executado |
| Browser/Angular desktop e estreito | Não executado |

A suspeita de incompatibilidade imediata entre `IS_FALSE` e o enum com converter
não se confirmou na construção real do Criteria; não é apresentada como defeito.
Esse probe não executou SQL. Não foi iniciada aplicação integrada nem alterado
banco nesta revisão; a V5 continua sem prova de aplicação nesta sessão. Assim,
a avaliação de UX é do contrato de formulário/seleção, sem aceite visual,
responsividade, foco ou acessibilidade renderizada.

[Probes e manifesto dos arquivos inspecionados](evidence/slice6-crud-review/README.md)
preservam a reprodução e os hashes. Os logs locais da suíte estão em
`.local/review-slice6/maven-tests.txt`. Os testes verdes continuam válidos para
os cenários que executaram; não cobrem as negações e estornos descobertos aqui.

A ordem recomendada de correção é: política efetiva e teto comum; estorno e
elegibilidade de origem; semântica de consumo e campos derivados; provas
integradas de transação/HTTP e materialização da UX. O aceite da Fase 1 precisa
ser reavaliado após esses gates, mantendo o backlog próprio das Fases 2/3.
