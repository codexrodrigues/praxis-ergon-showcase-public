# Preparar um novo cargo a partir de referência

Esta implementação trata a pendência de Cargos; seu encerramento depende da revisão independente e das provas atuais.
Órgãos, pessoas, vínculos, setores e documentos legais conservam as decisões
fundamentadas em `DUPLICATE-DRAFT-ADOCAO.md`. O estado das provas executadas
consta no scorecard e em `evidence/PILLARS-CAMPAIGN.json`.

## Contrato e responsabilidade

O Starter é o dono de `AbstractDuplicateDraftResourceController`,
`DuplicateDraftResourceService`, disponibilidade, envelope, links e resolução
de schema. O host implementa a seleção dos dados de negócio. A operação
`POST /api/ergon/cargos/{id}/duplicate-draft` devolve `CargoDuplicateDraftDTO` sem criar
registro, reservar código ou vaga, alterar versão ou emitir recibo de lote.

Somente `nome`, `categoria`, `subcategoria`, `tipoCargo`, `cargoFuncao`,
`escolaridade` e `cbo` são copiados. Identidade, órgão, documento legal, vagas,
datas, cargo aglutinador, atributos livres, versão e auditoria não são copiados.
`controleVaga`, `porReferencia` e `eAglutinador` iniciam `null`, sem escolha silenciosa.
O CREATE rejeita omissão ou null e aceita false explícito; `customFields` inicia vazio.
O DTO parcial não herda obrigatoriedade nem a prévia do CREATE. Não se usa clone da entidade ou
mapper genérico que possa incluir futuramente campos sem revisão.

Fonte de domínio: `docs-legado/v7x/aps/cargos_.tab`, confrontada com `CargoDados`,
`CargoService` e `ErgonInvariants`. O enquadramento serve de preenchimento inicial;
o ato legal e o quadro de vagas pertencem ao novo cadastro independente. Somente origem VIGENTE serve de referência: qualquer `dtExtincao` não nula,
inclusive futura, nega o preparo. Essa decisão não acrescenta veto ao pai
aglutinador extinto nem transfere autorização legal para o novo cargo.

## Autorização e isolamento

O endpoint e o serviço exigem `ERGON_READ` e `ERGON_WRITE`. A consulta da origem
inclui seu código e o órgão corrente no SQL. Origem inexistente ou fora desse
órgão resulta na mesma negação pública, sem nome, atributos livres ou estado.
Capabilities, actions e links usam o avaliador canônico; o provider do host
informa os pré-requisitos de criação e leitura. O snapshot de estado de Cargo
também consulta com escopo de órgão.

O serviço abre uma transação própria somente de leitura para não descarregar
alterações pendentes do contexto de persistência do chamador. A leitura não
bloqueia nem reserva a origem. Não se promete uma cópia congelada para criação:
o formulário é revisado pelo usuário e o POST final revalida as invariantes.

Limite corporativo existente: `X-Tenant` seleciona o órgão no showcase, mas não
representa identidade federada nem comprova autorização de participação no órgão.
A proteção desta operação não certifica isolamento multi-tenant de produção.

## Jornada e validação

“Preparar novo cargo” é uma ação disponível somente quando o usuário pode ler
a referência e criar. O consumidor segue o link publicado, recebe o rascunho e
usa o schema canônico de criação. Código e órgão permanecem obrigatórios.
O código é entrada de nova identidade positiva, não lookup de registro existente.
Descartar não exige comando de cancelamento porque nada foi persistido.

O POST final conserva as validações de identidade única, órgão, controle de vagas
e integridade do cargo aglutinador. A capacidade de preparar não substitui a
autorização ou as regras da criação.

`CargoDuplicateDraftNeonTest` cobre cenários por HTTP e serviço, com hashes
e contagens de todas as tabelas de domínio e do ledger técnico antes/depois.
O harness do host acrescenta consumo HTTP do link e do schema, normalização e
submit pelo runtime oficial Angular. Essa prova não monta tela e não certifica
orquestração visual da ação, responsividade ou acessibilidade de uma aplicação
Angular. Nenhum código de biblioteca Angular é alterado nesta frente.

Não há migration, alteração de grants ou novo vocabulário `x-ui`. Os derivados
são este guia, README, relatório, scorecard e evidências.
O contrato neutro e sua documentação permanecem no PR Metadata #311; o candidato
privado de validação não equivale à rc.179 declarada no POM nem a release público.

## Reconciliação T08

Decisões humanas: `T07-decisoes-mantenedor.md`, 28/10/2026. A política anterior de flags false e origem extinta foi substituída; provas anteriores não certificam este ajuste. O pacote é local, sem push ou publicação GitHub. A mudança de obrigatoriedade no CREATE exige que consumidores informem as três flags; UPDATE conserva seus defaults anteriores. O indicador booleano do host não equivale integralmente a CONTROLE_VAGA L/Q/N ou POR_REFER do legado.
