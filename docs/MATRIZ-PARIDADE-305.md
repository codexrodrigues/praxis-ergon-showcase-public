# Paridade delimitada dos pilotos — revisão 61fc36b

Esta matriz admite somente o cadastro e as operações explicitamente implementadas. A cobertura das colunas não certifica o produto Ergon inteiro, espécies estatutárias de licença ou requisitos jurídicos de acumulação e aposentadoria. Fontes primárias: comentários PostgreSQL em database-schema.json e documentação em D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps, especialmente FUNCIONARIOS.TAB, CARGOS_.TAB e VINCULOS.TAB. As telas ERGadm00104/ERGadm00229 orientam os blocos cadastrais; não se alega equivalência visual com elas.

| Piloto / referência | Cobertura admitida | Operações e relações | Prova executada |
|---|---|---|---|
| orgaos / EMPRESAS | 12/12 atributos de negócio: identidade institucional, razão/fantasia, CNPJ, endereço, contato e estado | Cadastro com CNPJ único; ativar/inativar com versão; ficha e quadro de cargos paginado | revisionProfessionalRegistrationAndUniqueness; reviewInstitutionalActionsProtectStateAndETag; phaseTwoRelatedCollectionsRespectParentAndPagination |
| cargos / CARGOS_ | 17/17: classificação, função/escolaridade, aglutinação, controle e autorização de vagas, referência normativa e extinção | Saldo por titular ou comissão sem dupla contagem; mesmo órgão e natureza aglutinadora; extinção sem ocupantes; coleções titulares/comissão | revisionBusinessRulesRejectInvalidVacanciesAndAggregation; reviewCommissionOccupancyPreventsExtinctionAndDoubleCounting; phaseThreeAggregatorPreviewRevalidatesEligibility |
| servidor-pessoas / FUNCIONARIOS / ERGadm00104 | 68/68: biografia, documentos, endereço, contatos, banco, cinco indicadores PcD e observação | CPF único; identificação profissional em par; PATCH cadastral especializado; ficha e vínculos paginados; prévia PcD | revisionProfessionalRegistrationAndUniqueness; phaseTwoPartialFormsRejectStorageOverflowBeforeWriting; phaseThreeSubmitRejectsForgedPreview; phaseTwoRelatedCollectionsRespectParentAndPagination |
| vinculos / VINCULOS / ERGadm00229 | 59/59: ingresso/contrato, lotação/exercício, cargos, requisição/cessão, previdência, pagamento, vacância e referências normativas | Cronologia com extremos não nulos; provimento em cargo elegível e com vagas; manutenção histórica; PATCHs especializados; cessão, aposentadoria, estágio, vacância e afastamento/retorno | phaseTwoChronologyRejectsReverseEndpointsWithMissingMiddle; phaseTwoHistoricalMaintenanceAllowsExtinctCargo; revisionBusinessRulesRejectCrossOrgAndExtinctCargo; revisionCessaoAndRetirementTransitions; reviewAfastamentoRoundTripPersistsLicenseAndVersion |

IDs, versão, auditoria e associações espelhadas não integram esses denominadores. Toda coluna de negócio dos pilotos possui projeção de leitura e destino de escrita permitido; isso não amplia a escrita cadastral para campos controlados por ações. Setores (12/12) e documentos legais (13/13) são consultas auxiliares; não são novos módulos de manutenção das outras tabelas.

## Política de referência documental

Manutenção cadastral, lotação, aposentadoria e estágio aceitam uma referência existente de emissor externo: um diploma pode ter emissor diferente do órgão do vínculo. A existência da referência não certifica sua aplicabilidade jurídica. Registrar/encerrar afastamento exige documento do próprio órgão, pois registra o ato administrativo da concessão/retorno. phaseThreeDocumentScopeIsExplicitPerOperation prova aceitação da referência externa no PATCH, recusa no afastamento e aceitação do documento local. Não há inferência universal de isolamento pela coluna empCodigo nem nova taxonomia normativa no schema.

## Aplicabilidade das determinações e decisões

| Recurso | Determinação de formulário implementada | Fonte e limite |
|---|---|---|
| orgaos | Nenhuma fórmula derivada exigida pelo cadastro atual | CRUD, estado e quadro de cargos; não se cria cálculo artificial para cumprir contagem |
| cargos | Nome do cargo aglutinador selecionado, CREATE/EDIT | HOST; mesma elegibilidade de órgão e indicador eAglutinador utilizada no domínio |
| servidor-pessoas | Disjunção dos cinco indicadores PcD, CREATE/EDIT | HOST; mesma função do fato definitivo; não determina diagnóstico nem benefício |
| vinculos | Nome e saldo do cargo selecionado, CREATE/EDIT | HOST; consulta não reserva vaga; submit revalida a ocupação |

Há seis contratos por operação e seis execuções do runtime oficial Angular, com proteção contra resultado obsoleto e rejeição visível. Os previews são informativos e recusados no submit; a decisão de persistência continua nos serviços.

**Escopo aprovado pelo responsável após e6b9fa0:** não existe snapshot externo de Decisões Governadas destinado a esta etapa. As determinações HOST, as invariantes transacionais e a prova do runtime Angular encerram a Fase 3A com PASS. O gate governedDecisions é NOT_APPLICABLE nesta fase por decisão explícita do responsável; Config/rules-engine pertence a uma fase futura dedicada. O fluxo administrativo de afastamento não é um motor de elegibilidade estatutária; acumulação e aposentadoria não recebem certificação jurídica integral nesta entrega.

## Limites de aceitação

Paridade admitida: cadastro mapeado e operações listadas nesta matriz. Permanecem fora da evidência: execução visual desktop/narrow de um host Angular, adoção de pacote npm publicado, carga concorrente para disputar a última vaga e paridade integral das demais rotinas legadas. A prova Angular exercita fonte oficial, normalização de schema e ReactiveDeterminationRuntimeService; não é uma prova visual das fichas ou coleções 360°. Uma nova revisão independente ainda deve avaliar esses limites antes do merge.
