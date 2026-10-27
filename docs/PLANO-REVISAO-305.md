# Complementação arquitetural da issue 305

Classificação: contrato-publico e arquitetural. Branch de trabalho: feat/ergon-showcase-scaffold-pilots.

Fontes: comentários 6455474606 (issue 305) e 6455474233 (PR 1), lidos integralmente e equivalentes; entidades locais e consulta information_schema da branch Neon br-super-sound-b7urizmd em 27/10/2026.

## Impacto

- Dono canônico do vocabulário, filtros, intents, actions e surfaces: praxis-metadata-starter rc.179.
- Dono dos dados e invariantes Ergon: este host e seu schema PostgreSQL.
- Consumidores: contratos públicos dos quatro pilotos, futuro frontend e catálogos de descoberta.
- Artefatos derivados: DTOs, mappers, schemas de prova, README, relatório e testes do host. Sem mudança prevista em Angular ou corpus global nesta etapa.
- Risco: ampliação de campos sensíveis e novos comandos; campos de ciclo de vida permanecem exclusivos dos comandos, protegidos por If-Match e estado persistido.

## Divergências a resolver antes de implementar a persistência

1. empresas não possui cnae, nat_juridica, cod_mun_ibge, numero_logradouro, complemento, bairro, inscricao_estadual, responsavel_nome ou responsavel_cpf. Orgao já expõe todos os seus campos institucionais persistidos; falta expor auditoria em leitura.
2. cargos não possui permite_acumulacao, exige_registro_conselho ou conselho_classe. As regras requeridas dependem de decisões persistidas que ainda não existem.
3. funcionarios representa PcD por deffisica, defvisual, defauditiva, defmental e defintelectual, além de obs_deficiencia. Não existem deficiente ou tipodeficiencia; preservar múltiplas deficiências exige definir a representação canônica antes de adicionar um discriminador singular.
4. A revisão usa nomes de apresentação diferentes dos atributos persistidos: rgnum=numrg, rgorgao=orgaorg, rguf=ufrg, rgdtexp=expedrg; dtvalcnh=validcnh; anoChegadaBrasil=chegbrasil; endereço usa tipologender, nomelogender, numender, complender, bairroender, cidadeender, ufender e cepender. A implementação deve mapear explicitamente os nomes, sem duplicar contratos com aliases.
5. Alguns documentos solicitados não têm coluna correspondente (RIC e detalhes adicionais militares/CTPS). Precisam de evolução de schema ou revisão explícita da cobertura esperada.

Não usar JSONB custom_fields como armazenamento oculto de campos institucionais canônicos nem publicar campos que descartem silenciosamente a escrita.

## Sequência proposta

1. Preservar integralmente o schema; usar as cinco colunas específicas de PcD e os nomes reais das entidades, conforme definição posterior do solicitante.
2. Completar contratos cadastrais e de leitura por grupos, com descrições deliberadas de domínio, governança pessoal/financeira/sensível e visibilidade de detalhe. Excluir do PUT todas as transições controladas por ações.
3. Implementar matriz de filtros e quick filters; provar cada predicado, inclusive joins e janelas temporais, com conjuntos de resultados verificáveis.
4. Implementar os três PATCH especializados e cinco novas ações, com DTOs próprios, permissões, estado, ETag e validação transacional. Publicar fatos calculados e disponibilidade contextual coerente.
5. Centralizar invariantes aplicadas em criação, PUT, PATCH e ações pertinentes: cronologia, organização, provimento, ocupação, acumulação por categoria, registro profissional, CPF/CNPJ e representação real de PcD. Serializar decisões de saldo de vagas pelo cargo e de acumulação pela pessoa para impedir corridas concorrentes.
6. Expandir NeonIntegrationTest com caminhos positivos/negativos e concorrência, atualizar evidências e relatório, executar mvn clean test e mvn clean package, atualizar o PR 1.

## Estado

Escopo resolvido pelo solicitante: schema intacto, sem DDL/migrations; nomes reais da entidade; PcD pelos cinco indicadores; acumulação pelas categorias MAGISTERIO e SAUDE. Permanecem ddl-auto=validate e flyway.enabled=false. Não serão publicados campos sem persistência.
