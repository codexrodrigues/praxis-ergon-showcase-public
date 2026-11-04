# Slice 8 — Onda 1: dados e contratos temporais

Entrega local para revisão independente da coordenação. Não representa liberação de escrita,
aprovação das Ondas 2–3, execução HTTP integrada, publicação ou paridade integral com o Oracle.
O gate completo do auditor continua **CHANGES REQUESTED** pelas capacidades das fases seguintes.

## Contrato entregue

- `/setores` conserva chave `String`, controller somente leitura e `/{id}/ficha-unidade`.
  A tabela física `setores` permanece como destino das FKs anteriores. O mapeamento de leitura
  usa `diretorio_setores_correntes`, que resolve a vigência corrente em `historico_setores`.
  Sem vigência corrente, o diretório usa o último período iniciado, inclusive em uma lacuna.
  Antes do primeiro período, usa o mais próximo no futuro. A linha física só fornece os dados
  quando não há histórico conhecido. O lookup reidrata códigos encerrados, futuros ou extintos
  com a seleção desabilitada e permite conservar o valor já gravado.
- `historico-setores`, `setores-ergon`, `cargos-por-setor`, `subempresas-localidades` e
  `grupos-setores` possuem entidade, repository, mapper, Dados/Create/Update/Response/DuplicateDraft,
  filtros, controller, ficha `detail` e fonte de opções rica. Escritas são negadas tanto pela
  availability quanto pelos hooks dos services. O DTO de rascunho não publica duplicate-draft.
- `radar-organograma-chefias-ocupacao` é uma view viva, sem inserção de estados de radar no seed.
  Sua identidade inclui id do período, início do segmento e hierarquia, evitando colisão entre
  órgãos, períodos e as cinco árvores.
- Os cinco novos lookups publicam código, descrições, status, política de seleção/retenção,
  drawer, diretório, ordenações, pesquisa textual e dependência de `empCodigo`. Total: 32 fontes;
  total de controllers de recurso: 36. Não há reconstrução de endpoint no frontend.
- Campos derivados são descartados na desserialização de requests e ignorados pelos mappers de
  escrita: caminho, nível e ocupação. Nenhuma coluna de negócio foi omitida do ResponseDTO.

## Semântica temporal e fronteiras

`organograma_em(orgao, data, hierarquia)` usa vigências inclusivas e a hierarquia oficial (0)
ou uma das quatro auxiliares (1–4). A travessia usa igualdade exata dos códigos dentro do órgão.
O caminho contém segmentos UTF-8 em hexadecimal separados por `/`; caracteres como `/`, `%`,
`_` e códigos irmãos com prefixos comuns não se confundem. A raiz ocupa nível 1.

Ciclos e superiores ausentes não viram raízes artificiais. Eles ficam fora da árvore, mas podem
ser inspecionados na lista de períodos, com `diagnosticoHierarquia`. Sem data explícita, a ficha
histórica calcula sua estrutura no primeiro dia do período; com data, usa a composição solicitada.
Uma alteração no ancestral recompõe o caminho do filho mesmo sem alterar o período do filho.

Em `/setores`, `filter`, paginação por cursor, `locate` e `options/filter` encaminham critérios
temporais ao histórico, mantendo os IDs públicos String. Nome, sigla, tipo, órgão, superior,
subempresa, localidade, extinção, município, UF, ausência de ato e janelas de encerramento são
pareados. `vigentes` e `encerrados` mantêm a semântica anterior de ausência/presença de término.
As superfícies analíticas, exportação e option-source do diretório continuam correntes e rejeitam
critérios temporais com `InvalidFilterPayloadException`, em vez de ignorá-los.

`referenciaFicha` é o campo de navegação publicado em `ResourceReference` no nome da unidade.
Nas linhas correntes é o próprio código. Nas linhas temporais contém o período, a data de
composição e a hierarquia. O runtime usa esse valor para carregar o item, descobrir sua
superfície contextual e abrir `ficha-unidade` no preview e no drawer. O corpo mantém `id`
com o código administrativo String. Uma referência de outro órgão ou fora da vigência
do período recebe 404. Referências de FK publicadas pelo código continuam a abrir o diretório.

`includeIds` mantém a finalidade canônica de acrescentar referências à página, não de restringir
a coleção. Se qualquer código explícito não resolver no órgão e na data, a consulta temporal
retorna erro genérico 400. Não converte seleção não resolvida em lista vazia que removeria a
distinção entre ausência de entrada e referências inválidas. `X-Tenant` continua sendo o contexto
do showcase; estes testes não certificam autorização multiempresa de produção.

## Comando, quadro e dados desconhecidos

A projeção de comando por função consulta designações, vínculos completos `(numfunc, numvinc)`,
ausências e exercícios interinos no mesmo órgão/data. Reconhece férias, licença especial,
afastamento e frequência com código afastador. A licença usa os estados posteriores à V6;
programação ainda não homologada não prova ausência. Substituto ausente não comprova cobertura.
Múltiplos titulares produzem conflito. Ambiguidade entre exercícios interinos permanece desconhecida.
O radar divide segmentos também no início e no dia seguinte ao término inclusivo da função.
Fora da vigência da função, titular, exercício, motivo e situação ficam sem apuração.

V12 demonstra os seis estados em ambos os órgãos em **04/11/2026**, por registros sintéticos de
origem: titular presente, substituto em exercício, ausência descoberta, vacância, ausência de
parametrização e dupla titularidade. As pessoas e vínculos S8 são isolados; documentos sintéticos
não devem ser usados operacionalmente. A mudança/cancelamento da origem recalcula o resultado.

Comando por cargo depende de autoridade histórica de provimento/eventos ainda não integrada.
As contagens de provimento, exercício físico, cessões/recebimentos e apropriação de folha,
ocupação do quadro e cobertura percentual permanecem **null**, com `escopoApuracao` explícito.
Não se usa `vinculos.setor` corrente para simular esses conceitos. As vagas autorizadas do nó
somam apenas cotas diretas, evitando somar novamente a reserva que abrange a subárvore.
O modelo de reserva/distribuição de `cargos_setor` não certifica a paridade completa de ERG_QUADRO.

## Legado consultado e decisões

Raiz: `D:/Developer/Techne/ErgonX/migracao/docs-legado`.

| Fonte | Evidência e decisão |
|---|---|
| `Doc-Sistemas/Empresas e Setores/Empresas e Setores.docx` | Unidade fiscal, regionalização, alterações de vigência, extinção e histórico; texto extraído na evidência privada. |
| `v7x/aps/fontes_ergon/erg_pm_vwsetores.vw` | MIN/MAX de datas não seleciona necessariamente a vigência corrente; a consulta temporal não pode filtrar apenas a linha consolidada. |
| `pack_ergon.pkb`, funções GET_SETOR_PROV/FUNC/EXERC_FUNC/PAGTO_FUNC, linhas 735–1179 e GET_CHF_EXERC/TIT, 2059–2455 | Quatro conceitos distintos de setor; comando consulta ausências e vínculos completos. GET_CHF_TIT devolve zero para ausência e ambiguidade; os seis diagnósticos são uma distinção nova. |
| `pck_setores_ergon.pkb`, MAIN_PRE/MAIN_POS | Vigência do setor, exclusividade cargo/função e sobreposição no órgão. Comandos ainda bloqueados; não se afirma migração 1:1 das regras. |
| `pck_cargo_setor.pkb`, MAIN_PRE/MAIN_POS/valid_cargo_setor, linhas 46–142 | Validade temporal do cargo e configuração por cargo/setor. O controle amplo de quadro pertence também ao pacote ERG_QUADRO. |
| `pck_erg_quadro.pkb`, linhas 807–839 e 970–991 | SUBORD distingue nó/subárvore, e a relação de descendência depende de data e empresa. |
| `ERGadm00100.xml`, `ERGadm00101.xml`, `ERGadm00102.xml`, `ERGadm00126.xml` em `v7x/java/.../crodata/trans/Administracao` | Tipos funcionais/financeiros, adicionais cargo/setor, parâmetros do setor e consulta de vínculos por setor. |

Divergências deliberadas do plano: Setor permanece read-only; migrations são V11/V12, não V24;
caminho é TEXT para não truncar árvores; ocupação desconhecida é nullable; o radar é view,
não tabela de resultados de seed; classificação histórica permite NULL porque o schema anterior
de setores já permitia essa ausência. O preflight recusa intervalo invertido com diagnóstico
explícito, sem inventar datas ou classificações.

## Scorecard da Onda 1

Contagem estática do auditor, além dos testes do resolver. `Response` inclui campos calculados.
Todos possuem identidade rica, referências com preview, uma ficha de leitura e zero problemas
editoriais detectados. Nenhum número abaixo é prova de operação transacional.

| Recurso | Colunas negócio → Response | Cobertura | Quick View | Refs/preview | Micro | Filtros / operações | Quick filters | Escrita contabilizada |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Setor | 40 → 41 | 100% | 5 | 12/12 | 1 | 19 / 8 | 2 | Somente leitura |
| Histórico | 37 → 39 | 100% | 6 | 11/11 | 1 | 22 / 9 | 2 | 35/37; caminho e nível derivados |
| Parâmetros Ergon | 23 → 24 | 100% | 6 | 6/6 | 1 | 20 / 7 | 2 | 23/23; execução bloqueada |
| Cargos por setor | 16 → 18 | 100% | 6 | 7/7 | 1 | 20 / 8 | 2 | 15/16; ocupação derivada |
| Subempresas/localidades | 25 → 25 | 100% | 6 | 4/4 | 1 | 21 / 7 | 2 | 25/25; execução bloqueada |
| Grupos | 14 → 14 | 100% | 6 | 4/4 | 1 | 18 / 8 | 2 | 14/14; execução bloqueada |
| Radar | 41 → 43 | 100% | 6 | 15/15 | 2 | 21 / 5 | 2 | Somente leitura |

Intents, WorkflowActions, BulkEditable/BulkOps, analytics e determinações dos seis recursos novos:
**0 nesta onda**. Facts novos: 0; Setor preserva seu fact anterior. Regras/invariantes transacionais:
não implementadas/liberadas. O auditor estrito mantém dez apontamentos: cinco decisões de duplicação
pendentes e cinco linhas de recursos com capacidades das próximas fases e/ou campos derivados.
Não foram adicionados ignores, falsos comandos ou regras simbólicas para tornar o auditor verde.

## Validação, baseline e reprodução

JDK 21.0.2 / Maven 3.9.6; starter local `8.0.0-rc.188`, SHA-256
`1CA0E682B3C100D1476A94E87EEE434D78282073AF7EFD1503BEE558B4390D28`.
Não é prova de artefato publicado ou instalado por consumidor remoto.

Evidência privada: `../.codex/slice8-wave1/` na raiz do workspace. Os testes usam cópia congelada
`checkpoint-final`, pois há alterações e builds concorrentes no host.

```text
mvn test -Dtest=ApiPathsContractTest,PraxisMetadataCompletenessTest,OrganogramaSlice8PostgresTest,OrganogramaSlice8CommandBoundaryTest,ShowcaseCanonicalConformanceGateTest
python scripts/workspace/audit-java-resource-completeness.py praxis-ergon-showcase --strict
```

O teste PostgreSQL é condicionado a `SLICE8_JDBC_URL`, `SLICE8_DB_USER` e `SLICE8_DB_PASSWORD`.
Sem essas variáveis ele é ignorado, portanto um verde sem ambiente não comprova o banco.
O launcher privado captura a conexão em memória e não grava credenciais.

Resultado da entrega inicial, anterior à revisão R1–R4: **33 testes, zero falhas, erros ou skips**, sendo 2 de caminhos, 8 de metadata,
16 de PostgreSQL, 3 de bloqueio de comandos e 4 de conformidade canônica. `gate-final.log`
registra BUILD SUCCESS; os testes de licença homologada/programada e frequência cancelada
usam a V11 corrigida. O inventário validado é 36 recursos / 32 option sources.

O README admite cópia direta de `br-super-sound-b7urizmd`, projeto `holy-mouse-40140108`.
Foi usada somente a cópia própria `br-square-pine-b7w614ju`, PostgreSQL **17.11**.
A origem tem o schema inicial; foram reproduzidas as três tabelas de férias a partir da prova
read-only do Slice 7 (`br-floral-bird-b7liwyv1`) e aplicadas V5–V10 locais antes de V11/V12.
Isso demonstra baseline compatível; **não demonstra replay de V1–V4**, ausentes do host.
Flyway continua desabilitado no startup. Nenhuma branch compartilhada foi migrada.

O replay registra hashes de cada SQL, restrições anteriores de setores, rejeição de intervalo
invertido, preservação de `tipo_setor=NULL` e comparação dos registros de todas as tabelas
preexistentes usando somente suas colunas anteriores. Os dados são comparados em memória;
o relatório conserva contagens e hashes, não dados pessoais. Evidência: `postgres-migrations.json`.

Os 284 registros das 29 tabelas preexistentes tiveram zero remoções ou alterações nas colunas
originais. O intervalo invertido foi recusado pela constraint já existente (`23514`); esse replay
não prova a execução do ramo adicional de preflight `22000` em uma baseline sem a constraint.
A branch descartável `br-square-pine-b7w614ju` foi excluída após os testes. A listagem posterior
confirmou sua ausência e a permanência da origem e da branch de prova do Slice 7 (`cleanup.json`).

Os testes cobrem seleção passada/atual/futura, unidade exclusivamente histórica, isolamento
por órgão, includeIds inválidos, cursor/locate, mudança de ancestral, quatro auxiliares,
segmentos especiais, ciclos/órfãos, nomes correlacionados, projeção corrente, seis estados de
comando, cancelamento de substituição, vínculo irmão, substituto ausente e restrições no banco.
Prova JPA/SQL e de contrato local; não substitui HTTP integrado, browser, autorização de produção,
benchmark de capacidade ou validação transacional das Ondas 2–3.

## Handoff

As correções R1–R4 têm [revisão e evidências próprias](../../.codex/slice8-wave1-r1-r4/REVISAO-FINAL.md).

Revisar o delta próprio e seus hashes, a matriz coluna a coluna e os logs no diretório de evidências.
Preservar o trabalho concorrente: o diff Git total contém trabalho anterior de outros Slices.
`provenance.json` diferencia arquivos próprios de mudanças concorrentes observadas.
A matriz detalhada está em `matriz-colunas.csv`; `delta-proprio.patch` parte da cópia inicial,
e não do HEAD Git compartilhado. `verification.json` registra hashes, testes e limites da prova.

Antes de liberar escrita nas ondas seguintes: definir política de cópia, invariantes do legado,
autoridade de tenant, concorrência/idempotência, ciclos e cobertura temporal de referências;
atribuir os três campos derivados pelo owner canônico, implementar intents/actions/bulk/facts/
analytics/export e determinações, e obter revisão independente. Revalidar também a semântica
temporal dos lookups para jornadas de edição histórica e as integrações de comando por cargo,
ocupação/exercício/folha. Nenhuma onda seguinte foi iniciada nesta entrega.
