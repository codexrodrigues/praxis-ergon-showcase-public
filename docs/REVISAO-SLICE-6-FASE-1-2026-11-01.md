# Revisão abrangente — Slice 6, Fase 1

> Histórico da revisão anterior às correções. O estado atual, as correções locais
> e as contenções de escrita estão em [CORRECOES-SLICE-6-2026-11-01.md](CORRECOES-SLICE-6-2026-11-01.md).

**Veredito: CHANGES REQUESTED. A entrega não está pronta para operação corporativa.**

Revisão de 01/11/2026 dos bytes locais da implementação da Issue #9, sobre
`praxis-metadata-starter:8.0.0-rc.186`. Esta revisão substitui a interpretação
anterior de que restariam apenas gates de fases posteriores. Foram encontradas
falhas funcionais dentro da Fase 1, além das pendências já registradas.

## Escopo, método e proveniência

Foram examinados os cinco recursos, entidades, DTOs, mappers, serviços, bases
herdadas, SQL V5, registry de opções, testes e documentação. O comportamento do
filtro foi conferido também no bytecode do JAR selecionado, evitando inferência
a partir de fontes de outra versão. O renderer oficial de checkbox foi
consultado somente para análise de contrato de UX.

Não houve mudança de código de produção, dependência, banco, configuração ou
inventário nesta revisão. Foram acrescentados este relatório e as ressalvas
nos documentos anteriores. Classificação das edições: `docs-apenas`.

Provas locais reproduzíveis estão em `.local/Slice6ReviewProbe.java`,
`.local/slice6-review-probe.txt`, `.local/slice6-review-tests.log` e
`.local/slice6-review-audit.txt`. O probe usa as classes compiladas do host, a
dependência selecionada e mocks de Criteria/EntityManager/executor; não grava
dados e não certifica persistência ou respostas HTTP.

## Achados priorizados

### R1 — P1: 11 filtros publicados são ignorados pelo executor

**Evidência:** `dto/TempoLicencaFilterDTO.java:16` introduz a superclasse dos cinco
FilterDTOs. `GenericSpecificationsBuilder.getAnnotatedFields` no rc.186 percorre
somente `filter.getClass().getDeclaredFields()`. O bytecode confirma essa chamada.

Cada filtro tem 17 annotations contabilizadas estaticamente, mas o executor
descobre apenas os seis campos próprios da classe concreta. Omnibox, órgão,
pessoa, vínculo, prefixo da descrição, situação, conjuntos de situações e
intervalos comuns de datas não geram predicados. No probe, preencher órgão,
pessoa, vínculo, situação e busca ampla produziu zero acessos ao Root e apenas
`criteriaBuilder.and()` vazio.

**Cenário corporativo:** pesquisar um servidor pode retornar assentamentos de
outros servidores. A mesma divergência atinge exportação filtrada e estatísticas.
A cascata declarada por pessoa/vínculo nos lookups também não restringe a consulta
por esses campos. O escopo de órgão aplicado separadamente ao `/filter` continua
independente dessa falha: não se deve confundir filtro do usuário com autorização.

**Correção e aceite:** suporte à herança deve ser resolvido no executor canônico
do starter, com testes de predicados e integração. Com o baseline rc.186 fixado,
o host precisa usar uma autoria de filtros efetivamente suportada. Não criar
executor SQL paralelo ou remapeamento de filtros no frontend. Exigir prova
positiva e negativa dos 17 campos em `/filter`, export, stats e cascata.

Classificação: `lacuna-real-de-contrato` para herança no executor; composição
incompatível com o baseline selecionado no host.

### R2 — P1: os novos lookups não aplicam o escopo de órgão ao reload

**Evidência:** os serviços sobrescrevem `resolveResourceFilterAccessScope`, mas
não `normalizeOptionSourceFilter`. No rc.186, os caminhos de option-source usam
`buildResourceSpecification`, sem intersectar automaticamente aquele escopo. O
hook de normalização padrão devolve o filtro recebido.

Chamando `VendaLicencaEspecialService.byIdsOptionSourceOptions` com tenant
explícito 1, o probe capturou a chamada real ao executor:

```text
LOOKUP_GET_BY_IDS_ARGS=byIdsOptions count=7 specification=null filter=null
```

A busca por IDs no executor JPA combina os IDs com essa specification; ela não
reintroduz o escopo do serviço. As dependências no descriptor são metadados e
não constituem uma restrição obrigatória do contexto de acesso. Em POSTs com
filtros, R1 ainda faz os campos comuns serem ignorados.

**Cenário corporativo:** um usuário consegue solicitar um ID de opção de outro
órgão e receber seus metadados de identificação pelo lookup, embora a consulta
normal por ID do recurso rejeite o mesmo órgão. Não houve teste contra dados
reais; a ausência da restrição foi reproduzida na fronteira serviço→executor.

**Correção e aceite:** aplicar o contexto confiável em todos os caminhos
canônicos de opções, incluindo GET by-ids, POST by-ids, includeIds e filter.
Provar isolamento com dois órgãos, inclusive quando o payload omite ou diverge
das dependências. Para produção, vincular o escopo à identidade autenticada:
`X-Tenant` opcional/controlado pelo cliente continua sendo apenas a convenção
demonstrativa já documentada.

Classificação: `ja-suportado-mal-nomeado-ou-mal-materializado`; fronteira
principal no host, com revisão do comportamento compartilhado do starter.

### R3 — P1: os filtros JSONB anunciados não são executáveis

**Evidência:** as quatro entidades e o radar mapeiam `customFields` com
`@JdbcTypeCode`/`@Column`, mas não declaram `@CustomFields`. Os DTOs declaram a
annotation. O motor `CustomFieldJpaCriteriaSupport` exige localizar o contêiner
anotado na entidade, usando `root.getJavaType()`.

O probe confirmou `CustomFieldSupport.property(...).isPresent() == false` nas
cinco entidades e reproduziu:

```text
CUSTOM_FILTER=Entity has no @CustomFields container
```

**Cenário corporativo:** um campo institucional pode aparecer no catálogo e no
formulário, mas sua pesquisa JSONB falha ao construir a consulta. Isso afeta
também recortes e exportações que dependam desses atributos.

**Correção e aceite:** completar a integração canônica nas entidades e validar
chaves, tipos, permissão de filtro e tenant com o provider real. No radar, revisar
explicitamente o reuso do resourceKey de campos do período de origem. Testar
JSONB efetivo, não apenas a presença da annotation nos DTOs.

Classificação: `ja-suportado-mal-nomeado-ou-mal-materializado`; dono: host.

### R4 — P1: gravações de gozo/venda ignoram elegibilidade e consistência do direito

**Evidência:** `TempoLicencaValidation.hydrate` valida identidade e órgão do pai.
Os métodos `validate` de gozo e venda não verificam cancelamento do pai, saldo,
permissão de venda, antecipação vedada, prescrição, limites individuais ou
consistência entre duração e datas. O período mantém seu saldo como valor
editável, sem sincronização após create/update/delete das parcelas.

O probe montou um período cancelado, saldo zero, venda vedada e antecipação
explicitamente falsa. A validação aceitou:

- Gozo de 999 dias, com intervalo previsto de dois dias e antes do fim da aquisição;
- Venda de 999 dias sobre esse mesmo período;
- Abono de 30 dias em parcela marcada sem abono.

Essa prova alcança a validação usada antes de salvar. Não afirma que houve
persistência. Os CHECKs da V5 limitam dias a 1–999 e integridade de datas/chaves,
mas não protegem saldo/elegibilidade entre registros.

**Cenário corporativo:** o mesmo direito pode ser consumido reiteradamente por
gozo e venda; a posição do período continua inalterada. O operador recebe campos
com aparência de governança sem que as permissões publicadas controlem a escrita.

**Correção e aceite:** definir a fonte única da contabilidade, proteger campos
de saldo/consumo contra PUT cadastral e executar atos por comandos canônicos
transacionais, com concorrência, disponibilidade e idempotência. Enquanto esse
fluxo não estiver admitido, restringir as capacidades de escrita operacional
canonicamente. A separação de fases precisa incluir uma fronteira de operação
segura, não apenas adiar as invariantes mantendo CRUD irrestrito.

Classificação: `suportado-parcialmente`; dono: serviços e comandos do host.

### R5 — P1: ETag publicado não protege o PUT contra formulários desatualizados

**Evidência:** os quatro serviços novos herdam diretamente de
`ErgonResourceService` e não implementam `VersionedCreateUpdateResourceService`.
O probe confirmou isso para venda. O controller canônico só exige e verifica
If-Match quando o serviço implementa essa interface. Os mappers ignoram versão,
e a entidade é carregada novamente durante o update ordinário.

**Cenário corporativo:** dois operadores abrem a versão 7. O primeiro salva e
produz a versão 8. O segundo envia o formulário antigo depois do primeiro commit;
o serviço carrega a versão 8 e aplica o snapshot antigo, sem comparar o ETag
original. `@Version` protege conflitos entre transações sobrepostas, mas não
esse caso de edição sequencial a partir de uma representação antiga.

**Correção e aceite:** adotar a atualização versionada canônica, como já ocorre
nos recursos anteriores do showcase. Provar ausência de If-Match → 428, ETag
antigo → 412 e sucesso com a versão correta em HTTP e no serviço.

Classificação: `ja-suportado-mal-nomeado-ou-mal-materializado`; dono: host.

### R6 — P2: o radar muda de representação sem mudar seu ETag forte

**Evidência:** `RadarTempoServicoLicencaEspecialService.java:27` usa somente
`pa.version` para a versão da representação. A consulta depende também de três
tabelas agregadas e de `current_date`. Uma nova parcela/venda ou a passagem de um
dia altera os indicadores sem incrementar essa versão do período.

O probe reproduziu dois corpos com dias até prescrição distintos, mesma
identidade/versão do período e ETag idêntico:
`RADAR_CHANGED_BODY_SAME_ETAG=true`. O serviço canônico de ETag assina
scope/resourceKey/ID/versão, sem incluir o corpo.

**Cenário corporativo:** consumidores que usam o ETag como revisão do drawer
podem tratar uma representação antiga como atual. A revisão da projeção precisa
representar todas as dependências relevantes e a data de avaliação. Até existir
essa garantia, o radar não deve anunciar revisão forte baseada apenas no PA.

Classificação: `suportado-parcialmente`; dono: contrato de revisão da projeção.

### R7 — P2: desaverbação e finalidades foram achatadas sem uma trilha de eventos

**Evidência:** `AverbacaoTempoServico` une a certidão com uma finalidade e mantém
um único par data/motivo de desaverbação e um total de dias retirados. A view
legada de `AVERB_OQUE_CONTA` liga finalidades à `CHAVEAVERB`; a tabela de
desaverbações registra seus próprios intervalos, efeitos e justificativas.
Na V5, a finalidade participa da unicidade do registro de averbação. Modelar
mais de uma finalidade exige repetir os dados da certidão; uma segunda retirada
parcial não tem um evento próprio nesse contrato.

**Cenário corporativo:** retificações sucessivas podem substituir o motivo
anterior e divergir entre cópias da certidão. `created_at`/`updated_at` e uma
coluna de observações não registram, por si, autor, razão e conteúdo de cada ato.

**Correção e aceite:** fechar a matriz certidão→finalidades→desaverbações e a
contabilidade de cada efeito. Documentar explicitamente quais informações são
projeções consolidadas e quais eventos devem permanecer rastreáveis. A cobertura
100% das novas entidades não mede cobertura do modelo legado nem paridade.

Classificação: `suportado-parcialmente`; dono: modelo de domínio do host.

### R8 — P2: a UX mistura herança com falso e usa campos genéricos para atos distintos

**Evidência:** `PeriodoAquisitivoLicencaEspecialDados.java:98` representa o
override anulável por CHECKBOX, embora descreva três estados: nulo herda,
verdadeiro permite, falso veda. No renderer oficial, a interação produz boolean
e o estado indeterminado é configuração visual; não existe nesse contrato uma
escolha explícita de voltar a herdar após editar.

Os cinco Quick Views repetem início/término/situação comuns. Em venda, o rótulo
“Início do intervalo” representa a data do evento; “Término do intervalo” é
opcional e normalmente sem utilidade. A data de pagamento fica fora dos cinco
campos executivos. Gozo, aquisição e venda exigem tarefas e vocabulários próprios.

**Correção e aceite:** oferecer escolha explícita Herdar/Permitir/Vedar que
preserve o valor nulo, e testar o round-trip. Curar campos executivos por recurso:
venda deve destacar dias, modalidade e pagamento; gozo deve distinguir previsão
e término efetivo; período deve destacar aquisição, saldo e prazo. Automatizar
cálculos derivados no fluxo canônico para evitar conciliação manual de saldo.
Validar desktop/narrow, teclado e estados vazio/erro com screenshots.

Não houve navegador nesta revisão: a falha de contrato de três estados e a
curadoria dos campos foram verificadas em código; aparência e responsividade
continuam sem aceitação visual.

Classificação: `ja-suportado-mal-nomeado-ou-mal-materializado` no contrato do host;
eventual necessidade de controle reutilizável pertence ao dono canônico da UI.

## Planejado, executado e comprovado

| Dimensão | Evidência atual | Avaliação |
|---|---|---|
| Cinco recursos, entidades, DTOs e rotas | Presentes, com bindings resolvidos | Estrutura entregue |
| Contabilidade de colunas | 100% no modelo novo; 30/33/21/19/19 colunas de negócio | Não prova paridade ou invariantes |
| Quick View / identidade / microvisualização | Cinco campos executivos por recurso; metadata emitida | Presença passa; UX requer R8 |
| Filtros | 17 annotations por recurso; apenas 6 descobertas pelo executor | **FAIL — R1** |
| Lookups ricos e cascata | Descriptor completo; escopo/restrições não demonstrados pelo fluxo efetivo | **FAIL — R1/R2** |
| Campos customizados | Anotados em DTOs, ausentes nas entidades | **FAIL — R3** |
| Escrita de direitos | Validação de vínculo/chaves; invariantes incompletas | **FAIL para operação — R4** |
| Concorrência | @Version/ETag presentes; precondição de PUT ausente | **FAIL — R5** |
| Radar | Agregações prévias evitam multiplicação por joins; revisão insuficiente | **FAIL na revisão da representação — R6** |
| Export/stats | Contratos e allowlist presentes; filtros herdados afetam o conjunto | Não aprovar exportação/analytics operacional |
| V5 e seed | SQL escrito; sem V1–V4 no checkout e sem aplicação a banco | Persistência/paridade sem prova |
| Gate estrito | Reexecutado: 21/25 linhas PASS, 9 pendências, zero editorial issues | **FAIL** |
| Testes focais | 15 testes passaram nesta revisão | Gate insuficiente para R1–R8 |

O scorecard completo, com os contadores de WorkflowAction, ResourceIntent,
BulkEditable/BulkOps, Facts, UiAnalytics, ReactiveDet e editorial, continua em
`evidence/SLICE-6-FASE-1-SCORECARD.md`. O auditor foi reexecutado sem alterações
e manteve as quatro reprovações de recursos e cinco decisões pendentes. Ele não
detecta as falhas semânticas acima. Seus 21 PASS são resultados de análise
estática, não aprovações operacionais.

## Integridade da documentação e dos testes anteriores

O relatório anterior foi transparente sobre não aplicar a V5 e sobre gates
pendentes. Entretanto, a afirmação de que os filtros e a cascata estariam
implementados não estava sustentada por execução de predicados. Os testes
percorriam a herança para contar campos e comparar nomes de Quick Filters; o
executor real do rc.186 não faz isso. O teste de lookup verificava o descriptor,
não o conjunto retornado. O round-trip do mapper aceitava amostras que não
passavam pelas invariantes de negócio e não exercitava banco/HTTP.

As expressões de microvisualização e a cobertura estrutural continuam provadas
no alcance executado. Os 220 testes da entrega anterior permanecem um resultado
histórico real; não reexecutei a suíte inteira nesta revisão. Reexecutei os 15
testes focais, que passaram apesar dos contraexemplos do probe. Isso evidencia
uma lacuna do gate, não confiabilidade dos fluxos não testados.

Também não foi demonstrado que a V5 pode ser aplicada ao baseline real, que o
Flyway de domínio possui um procedimento admitido ou que o Hibernate valida o
schema resultante. O seed associa registros sintéticos aos primeiros seis
vínculos existentes: sua aplicação deve permanecer restrita ao ambiente
demonstrativo admitido. Não corresponde a migração de direitos reais.

## Ordem de correção e critérios de aceite corporativo

1. Fechar isolamento e execução de consultas: corrigir R1/R2/R3 e provar os
   conjuntos retornados com pelo menos dois órgãos e dois titulares por órgão.
   Cobrir filtro, cursor, locate, export, stats e todos os caminhos de lookup.
2. Definir a fronteira de escrita segura da fase: comandos canônicos e
   contabilidade única, ou indisponibilidade explícita das operações ainda não
   admitidas. Adotar precondições versionadas e provar R4/R5 com casos negativos,
   duplicação de requisição e concorrência real em PostgreSQL isolado.
3. Fechar a projeção: revisão/ETag das dependências e data de avaliação,
   distinção entre tempo certificado e dias de direito, eventos de desaverbação e
   limites de paridade documentados. Corrigir a UX de herança e Quick Views.
4. Validar V5 no schema anterior admitido, ampliar os testes com os
   contraexemplos reproduzidos e formalizar as decisões de adoção e de fases.
   Atualizar o relatório de aceitação somente depois desses gates.

A ausência de atributos de governança no rc.186 e as exigências transversais do
auditor continuam sendo divergências reais de escopo/contrato. Resolver essas
divergências, isoladamente, não corrige os problemas de integridade encontrados
nesta revisão.
