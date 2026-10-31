# Adoção canônica de rascunho de Cargo

Classe: contrato-publico, transversal. Aderência: contrato já suportado pelo
Starter, ainda não adotado pelo host. Não criar endpoint ou envelope paralelo.

## Impacto e fases

1. Integrar no candidato isolado as alterações neutras existentes no checkout
   canônico do Metadata, preservando sua origem e o checkout de terceiros.
   Validar suporte estrutural, availability, controller, actions e schema.
2. Adotar AbstractDuplicateDraftResourceController e DuplicateDraftResourceService
   em Cargo. O draft usa CreateCargoDTO; copiar somente nome, categoria,
   subcategoria, tipoCargo, cargoFuncao, escolaridade e cbo. Não copiar identidade,
   órgão, documento legal, vagas, datas, agregação, JSONB, auditoria ou versão.
   Manter os defaults do novo cadastro para os demais campos. Exigir leitura e
   escrita, origem do órgão corrente e aplicar as mesmas restrições no service.
   O preparo não persiste nem reserva vaga. O POST final revalida o cadastro.
3. Provar no Neon isolado: origem ausente/inacessível, read sem write, disclosure,
   capabilities/actions/links, schema de criação, descarte, tabela e versão
   inalteradas, nenhum recibo de comando/lote e criação final válida/inválida.
   Executar a bateria do host após trocar a dependência e atualizar recibos,
   inventário atribuído, relatório, scorecard e PR. Excluir a cópia após as provas.

## Fronteiras

Fonte canônica: praxis-metadata-starter (tipos neutros, contrato de disponibilidade).
Consumidor: host Ergon Cargo e runtimes de formulário/discovery. As bibliotecas
Angular ficam na frente delegada; não serão alteradas nesta adoção backend.
Derivados: README e documentação/evidências do host, inventário canônico do auditor,
proveniência do candidato e PR do Starter. Não há novo vocabulário x-ui.
Risco: troca das bases antigas do SDK é quebra Java beta já prevista pelo contrato;
validar o conjunto integrado sem aliases de compatibilidade nem release.

Schema Neon intacto, ddl-auto=validate, flyway.enabled=false. Não consultar o
quickstart. Não certificar artefatos públicos ou aceite visual a partir de mocks.

Fonte de domínio consultada: docs-legado/v7x/aps/cargos_.tab, confrontada com
CargoDados, CargoService e ErgonInvariants. Classificação e escolaridade podem
servir de preenchimento inicial; quadro de vagas, identidade e ato legal pertencem
à criação independente do novo cargo. A decisão anterior permanece histórica.

## Ajustes após a campanha de integração

A primeira prova focal encontrou duas comparações sensíveis à ordem de UNION ALL;
o teste passou a ordenar o snapshot por tabela. Na campanha integral seguinte,
os seis casos de lote passaram, mas a preparação de Cargo recebeu 503 por esgotar
o orçamento da chamada HTTP interna de OpenAPI. Os testes MockMvc de domínio
também expuseram transação externa incompatível com o fence do novo avaliador.
A campanha foi interrompida após registrar logs, relatórios disponíveis e stacks;
não é uma campanha integral aprovada.

Preservar a guarda do Starter. Separar a transação de preparação de fixtures das
requisições MockMvc, manter rollback apenas nas provas diretas de invariantes e
restaurar dados de domínio por DML transacional na branch descartável. O baseline
fica em memória, respeita FKs e verifica igualdade antes do commit; sem TRUNCATE,
DDL, desativação de constraints ou exportação de dados pessoais.

Publicar explicitamente uma vez por contexto de aplicação. Contextos já publicados
usam a fotografia instalada; cada requisição continua passando pela atestação LIVE.
Não repetir RECONCILE entre classes apenas por compartilharem o mesmo contexto,
nem fazer retry automático de publicação ou ampliar seus prazos. A ocorrência de
503 permanece documentada; este ajuste de preparação não certifica sua eliminação.

Uma frente concorrente iniciou a organização de ApiPaths no checkout principal.
O usuário confirmou sua propriedade e pediu preservação. A campanha desta frente
foi isolada em `.worktrees/ergon-cargo-draft`, a partir de 1769b0958b, sem incorporar
essa refatoração. Os resultados devem identificar exatamente o candidato validado.

## T08 — reconciliação das decisões humanas de 28/10

Classificação: contrato-publico. O adendo T07-decisoes-mantenedor.md substitui a política anterior: as três flags iniciam null e são obrigatórias no CREATE; somente origem com dtExtincao nula prepara draft. Não acrescentar veto ao pai extinto.

Plano: (1) separar CargoDuplicateDraftDTO do request obrigatório, aplicar estado no contrato canônico e no serviço; (2) preservar defaults de UPDATE e impedir merge de identidade nova na criação; (3) atualizar fixtures e provar flags, estados, não mutação, concorrência e schemas; (4) registrar pacote local T08 e scorecard com lacunas explícitas, sem push/publicação.

Impacto: host Cargo, consumidores do CREATE que devem informar três booleanos, provas HTTP e documentação do host. Starter continua dono do protocolo neutro; não alterar Angular. Quebra intencional: omissão dos booleanos deixa de significar false no CREATE. Não publicar versão nesta etapa. Browser permanece gate de T09/T10. Inventário não recebe aprovação independente antecipada.

A bateria intermediária de 16 testes passou antes desta reconciliação (logs e Surefire em .local/duplicate-adapted-green-reports). Não prova os novos requisitos. Worktree isolada movida para .worktrees/ergon-cargo-draft-proof/praxis-ergon-showcase para conservar a identidade do auditor; checkout compartilhado e ApiPaths de outra frente preservados.

## T08 — conflito de constraint no flush JPA

Classificação: contrato-publico/transversal; suporte parcial. O handler já publica 409/DATA_INTEGRITY_VIOLATION para DataIntegrityViolationException, mas ConstraintViolationException nativa do Hibernate escapa de flush direto e vira 500. Reproduzido por duas criações HTTP concorrentes no Neon, sem overwrite.

Plano/impacto: completar o handler canônico com a exceção tipada de constraint, preservando envelope/código/mensagem sanitizados; testar dispatch HTTP e a falha inesperada; gerar candidato privado distinto e repetir host. Consumidores recebem o conflito já existente, sem novo formato, retry ou dependência Angular. Atualizar documentação técnica do erro; corpus e landing não publicam este detalhe de implementação. Sem release/push; não consultar quickstart.

## T08 — formato do contexto antes de availability

Classificação: contrato-publico no host. A campanha detectou `X-Tenant=invalid` no preparo respondendo 403: o resolver lança 400 dentro do provider e a composição canônica de availability converte a exceção em negação segura. Esse comportamento seguro do SDK deve permanecer.

Plano/impacto: validar a sintaxe do contexto na entrada MVC dos handlers `@ApiResource`, antes de discovery e execução. Reusar TenantContext, sem mapear novas rotas nem inferir autorização por órgão. Handlers não relacionados permanecem fora dessa semântica. Validar formatos inválidos, positivos, ausência/default e handler alheio em MockMvc, repetir o cenário HTTP real e a campanha final. Atualizar relato/recibos; não há mudança de schema, contrato Angular ou publicação de artefato externo.

A compilação focal separada executou 9 cenários MVC com êxito, sem tocar nas classes da campanha em curso. Não é a execução Maven final nem prova Neon; sua aplicação aos fontes aguarda o encerramento e arquivo da campanha atual.
