# P5-C — Reconciliação do inventário de duplicação assistida

Este registro descreve a implementação local da Issue #10. A decisão histórica
de não duplicar assentamentos como atos administrativos permanece válida:
o preparo de um rascunho não cria um ato nem transfere seu direito, autorização
ou pagamento. Os quatro controllers publicam o preparo pelo contrato
`AbstractBulkGovernedDuplicateDraftResourceController`; o novo cadastro passa
pelo create e por suas validações.

| Recurso | Sugestões copiadas | O novo cadastro exige |
| --- | --- | --- |
| Averbação de tempo | Descrição, instituição, tipo de tempo, regime e finalidade | Identidade, titular, vínculo, datas e certidão próprios |
| Período aquisitivo de licença especial | Descrição, órgão e dias de direito propostos | Vínculo, chave e datas próprios; não copia a averbação de origem nem consumo |
| Gozo de licença especial | Descrição, órgão, dias e proposta coerente de abono | Período, ordem e datas; nova validação do saldo e da política de abono |
| Venda avulsa de licença especial | Descrição, órgão e dias propostos. A modalidade do novo rascunho é PECUNIA | Período e datas próprios. O cadastro revalida saldo e requisitos da venda |

Na venda, a origem elegível é uma conversão avulsa em PECUNIA, desvinculada de gozo,
em situação SOLICITADA, AUTORIZADA ou PAGA. O serviço aplica a mesma política ao
snapshot de disponibilidade e ao preparo. O abono vinculado continua legível
conforme o acesso ao recurso e nasce da homologação do gozo. Essa fronteira
reflete o tratamento separado de venda e abono em `PCK_ERG_VENDA_LICESP` e
a geração de abono em `PCK_LIC_ESP` no legado Ergon v7x.

O serviço verifica acesso à origem antes de preparar o rascunho e rejeita origem
em estado terminal nos casos definidos em cada serviço. Cargo mantém a adoção
e o recibo de revisão focal de 29/10/2026. As regressões permanentes são
`P5CustomFieldsAndDraftTest`, `TempoLicencaSlice6Phase2Test` e
`CargoDraftContractTest`. A revisão deste inventário não equivale a uma prova
em Neon, Angular ou em produção.
