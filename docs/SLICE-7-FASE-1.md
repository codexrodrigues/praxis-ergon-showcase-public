# Slice 7 · Fase 1

A projeção lê a posição corrente, uma linha por designação em exercício de chefia ou direção cuja função permanece vigente. A data de referência é a data civil da sessão SQL. O identificador da linha é o da designação. O radar procura férias e afastamentos do vínculo titular no mesmo órgão e conta como cobertura o exercício interino cujo próprio impedimento permanece válido na data de referência. Pendências de folha são contabilizadas por designação.

| Recurso | Linhagem consultada em `docs-legado/v7x/aps` |
| --- | --- |
| Função | `FUNCOES_EV_.tab` define o marco de controle. `funcoes_ev_empresa.tab` registra o recorte por empresa. `hist_funcao_ev.tab` distingue permissão e tipo de substituição. `fontes_ergon/funcoes.vw` reúne códigos de função e os cargos classificados como função. |
| Vaga | `VAGAS_FUNCAO.TAB` e `HIST_VAGA_FUNCAO.TAB` admitem número de doze dígitos. `fontes_ergon/erg_vw_vagas_funcao.vw` preserva origem, extinção e publicação. |
| Designação | `DESIGNACOES_EV.tab` e `EVENTO_FUNC.TAB` preservam vínculo, vaga e atos. `fontes_ergon/designacoes.vw` integra eventos com natureza de designação. `fontes_ergon/ergadm00127_designacoes.vw` explicita ordem e identidade dos dois substitutos. |
| Substituição | `substituicoes_ev.tab` e `SUBST_AUSENC_EV.tab` ligam o registro ao evento precursor. `SUBST_EVENTUAIS.tab` e `SUBST_INDICADOS.tab` distinguem exercício eventual de indicação. `fontes_ergon/substituicoes.vw` distingue vigência funcional e remuneratória e usa o evento titular como origem. |
| Radar | `fontes_ergon/vw_chf_tit_exerc.vw` resolve titular e exercício por setor, empresa e data. `fontes_ergon/chefe_func.vw` relaciona provimento, setor e vínculo do chefe. |

As chaves compostas dos vínculos usam `VinculoId`. Os campos flexíveis das tabelas de origem correspondem a extensões JSONB por recurso. Os intervalos históricos de função e vaga embasam o cadastro de vigência desta fase. A modelagem conserva atos de publicação e fundamento legal separadamente. Os links para cadastros já publicados reutilizam os identificadores de superfícies 360° desses recursos. As novas fichas publicam a superfície `detail`.

## Schema e dados demonstrativos

A V8 cria os quatro cadastros e a view do radar. A V9 inclui funções nos órgãos 1 e 2 e usa unidades, vínculos e impedimentos vigentes já cadastrados para montar as vagas, designações e exercícios interinos. Quando um órgão tem unidade vigente e vínculo ativo, mas nenhum titular impedido elegível, o seed registra uma designação ordinária na vaga 705. A fixture exercita férias cobertas, afastamento coberto e afastamento sem substituição nos dois órgãos, além da instalação sem impedimentos correntes.

A configuração do showcase valida o schema na inicialização e mantém a execução automática de migrações desabilitada. A instalação do schema de desenvolvimento deve incluir V8 e V9 no processo de migração do host.

## Decisão de disponibilidade de duplicação nesta fase

- **Funções de confiança**, **Designações** e **Substituições** publicam CRUD cadastral. O plano atribui a decisão de preparo assistido e sua política de cópia à Fase 2.
- **Vagas de função** têm número próprio dentro do órgão e origem em ato de distribuição. A inclusão usa cadastro explícito da posição.
- **Radar de chefias** é uma projeção de consulta sem inclusão.
