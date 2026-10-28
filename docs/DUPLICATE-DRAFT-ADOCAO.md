# Decisão de adoção de duplicate-draft — Ergon Showcase

Data: 28/10/2026. Autor da avaliação: Codex, agente responsável pelo host, a pedido
do mantenedor nesta rodada. Não equivale a aprovação humana ou revisão independente
do PR inteiro. Base do host: `ac6dd29`; SDK consumido: `3822eca0`, candidato privado.

Esta avaliação separa aplicabilidade de domínio de suporte no SDK. A duplicação
canônica prepara um rascunho sem persistir; não deve clonar automaticamente uma
entidade JPA. O POST de criação posterior continua obrigado a validar identidade,
órgão, autorização, campos e invariantes. Ausência de implementação não é N/A.

## Decisões por recurso

| Recurso | Decisão | Fundamentação e fronteira |
|---|---|---|
| Cargos | **Pendente, aplicável** | Classificação, escolaridade e natureza do cargo podem servir de referência para outro cargo. Não copiar código, órgão implicitamente, extinção, autorização de vagas, documento legal, vínculo aglutinador ou JSONB indiscriminadamente. A criação requer nova identidade e nova fundamentação. Há valor de negócio para um preparo com allowlist; não marcar N/A para superar o auditor. |
| Órgãos | **Não aplicável ao cadastro atual** | Cada registro identifica uma instituição responsável pela gestão de pessoas, com identidade e CNPJ próprios. Não há caso de uso aprovado de criação por modelo entre instituições. Endereço e contato são dados da instituição, não defaults compartilhados. Criar pelo formulário cadastral e validar CNPJ é o fluxo adotado. Isso não é proibição universal de preparar modelos de órgão: reconsiderar mediante caso de uso e política explícitos. |
| Pessoas | **Não aplicável** | O registro representa uma pessoa física única. Nome, CPF, documentos, filiação, deficiência, contato e dados bancários não constituem modelo de outra pessoa. Novo vínculo deve referenciar a pessoa existente, não gerar outra pessoa copiando sua ficha. Reutilização desses dados entre pessoas traz risco de identificação errada e divulgação indevida. |
| Vínculos | **Não aplicável à cópia genérica** | Novo provimento é um novo assentamento da mesma pessoa ou de outra pessoa; depende de cargo, vaga, órgão, ato, datas e acumulação. A ficha anterior contém situação e fatos históricos que não autorizam um novo vínculo. O fluxo adotado é criação explícita com determinações/invariantes. Uma futura jornada de reingresso ou novo provimento precisa de comando e política próprios; não pode ser inferida como cópia da ficha anterior. |
| Setores | **Não aplicável neste recurso de consulta** | Controller e service são read-only. Publicam ficha e referências da estrutura organizacional existente, sem operação create. Preparo de duplicação pressupõe criação autorizada e não cabe nesta fronteira; eventual manutenção de organograma terá revisão própria. |
| Documentos legais | **Não aplicável neste recurso de consulta** | Controller e service são read-only. O registro referencia um ato e sua fundamentação, não um template de ato novo. Não há create nesta fronteira. Copiar identificação, publicação ou anexo de um ato produziria falsa referência jurídica. |

## Evidências consultadas

- `praxis-metadata-starter/docs/technical/DUPLICATE-DRAFT-STRUCTURAL-SUPPORT.md` e
  `DUPLICATE-DRAFT-AVAILABILITY.md`: opt-in, POST real, pré-requisitos create/view,
  não persistência e revalidação final. São arquivos de trabalho de outra frente;
  não estão integrados ao candidato testado. A própria nota aponta Cargo como piloto.
- Host `CargoService.beforeCreate/update` e `ErgonInvariants.cargo`: identidade,
  órgão, vagas ocupadas e aglutinador. `ErgonInvariants.pessoa/orgao`: CPF/CNPJ únicos.
  `ErgonInvariants.vinculo`: cronologia, provimento, órgão e acumulação.
- `SetorController/Service` e `DocumentoLegalController/Service` herdam as bases
  read-only e descrevem consulta de referências, sem manutenção cadastral.
- Legado `v7x/aps/cargos_.tab`: classificação, extinção, controle de vagas,
  aglutinador e `PONTLEI` como referência a embasamento legal.
- Legado `v7x/java/techne-archon-ergon/src/main/resources/techne/archon/ergon/crodata/trans/Administracao/ERGadm00104.xml`:
  consulta individual da pessoa por número e CPF; não prova uma jornada de cópia.
- Mesmo diretório, `ERGadm00229.xml`: seleção de um vínculo pelo servidor e número
  do vínculo; não prova autorização de novo provimento por cópia.
- Legado `Doc-Sistemas/Empresas e Setores/Empresas e Setores.docx`, seção Empresas:
  instituição responsável pela gestão de pessoas, CNPJ/identificação próprios;
  distinção entre empresa e subempresa. O showcase mapeia as colunas reais de seu
  banco e não importa automaticamente todos os detalhes do modelo legado.

As fontes legadas acima ficam em `D:/Developer/Techne/ErgonX/migracao/docs-legado`.
As decisões de não adoção são análise arquitetural deste escopo, não afirmação de
que o legado ou uma norma jurídica proíba toda forma de reaproveitamento.

## Gate remanescente de Cargo

Consumir primeiro o contrato canônico neutro integrado. Definir DTO de rascunho
com allowlist deliberada, autorização da origem/órgão/campos e defaults explícitos.
Provar HTTP: origem inacessível, leitura sem criação, sem vazamento entre órgãos,
rascunho sem escrita/versão/recibo, descarte sem efeito e POST final revalidando
identidade, vagas e aglutinador. Validar o consumidor oficial em frente própria.
Não se adotará a hierarquia legada nem uma implementação local paralela para
antecipar o contrato em andamento. Cargo permanece pendente no inventário.
