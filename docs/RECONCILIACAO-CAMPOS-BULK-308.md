# Reconciliação dos campos da Issue #2 / Metadata #308

Investigação de 28/10/2026, sem alteração das 16 tabelas de domínio.

| Campo solicitado | Evidência primária | Conclusão de contrato |
|---|---|---|
| `recebeAdiantamento` em pessoa | Inventário PostgreSQL de todas as colunas/comentários de `public`: não existe coluna de adiantamento. `funcionarios.tipopag` descreve modalidade bancária, não direito a adiantamento. No legado, `FERIAS.tab` documenta `ADIANTDTERC` como pedido de adiantamento do décimo terceiro e `ADIANTSAL` como pedido de adiantamento salarial; `parametros_conta.tab` e `peraqfer.tab` tratam direitos por parâmetros/período aquisitivo | Não há equivalente cadastral unívoco em `funcionarios`. Não converter forma de pagamento em indicador de adiantamento nem transferir atributo de férias para pessoa. Requisito depende de futura ampliação de domínio; permanece lacuna explícita em relação ao exemplo da issue |
| `cargaHorariaPadrao` em cargo | `cargos_.tab` não contém essa coluna. `ERG_HIST_CARGO_JORNADAS` possui `CARGO`, `DTINI`, `JORNADA` e `EH_PADRAO`; o comentário de `EH_PADRAO` define preenchimento automático de jornada em lançamento de evento. `HORARIO_TRAB_.CARGA_HR_SEMANA` pertence ao horário de trabalho. No banco provisionado só `vinculos.jornada_semanal` representa horas semanais contratuais/estatutárias | Jornada padrão de cargo envolve histórico e referência à jornada; não equivale à jornada individual do vínculo. As tabelas legadas correspondentes não estão nas 16 provisionadas. Não introduzir alias, campo JSONB ou DDL. O requisito de editar padrão do cargo fica explicitamente não atendido neste schema |
| CLEAR de `correio` | PostgreSQL: `vinculos.correio boolean NOT NULL`; comentário: recebimento de correspondências/contracheques impressos. `VINCULOS.TAB` preserva `CORREIO CHAR(1) NOT NULL DEFAULT 'N'` na alteração TAREFA104918 de 01/10/2024. `ERGadm00229.xml` usa checkbox `chkCorreio`, campo `correio` | SET aceita `true` e `false`; CLEAR recusado. Não anunciar limpeza incompatível com a coluna. A descrição ilustrativa “malote” não transforma o indicador em código |
| `controlaVaga` | JPA/Neon: `Cargo.controleVaga` / `cargos.controle_vaga`; legado `CARGOS_.CONTROLE_VAGA` | Nome público real `controleVaga`, sem alias |

Fontes legadas consultadas em `D:/Developer/Techne/ErgonX/migracao/docs-legado/v7x/aps/`:
`funcionarios.tab`, `cargos_.tab`, `cargos_empresa.tab`, `VINCULOS.TAB`,
`FERIAS.tab`, `parametros_conta.tab`, `peraqfer.tab`,
`erg_hist_cargo_jornadas.tab` e `horario_trab_.tab`.
Tela: `v7x/java/techne-archon-ergon/src/main/resources/techne/archon/ergon/crodata/trans/Administracao/ERGadm00229.xml`.

A consulta PostgreSQL cruzou `pg_class`, `pg_namespace`, `pg_attribute` e
`col_description`, procurando nomes e comentários de adiantamento, carga horária,
jornada, correio e pagamento. Foram encontrados `funcionarios.tipopag`,
`vinculos.tipopag`, `vinculos.jornada_semanal` e `vinculos.correio`; nenhum
indicador de adiantamento ou jornada padrão de cargo. A ausência no schema foi
confirmada na branch original designada, sem escrita.

Também foram examinados o catálogo governado `custom_field_definitions` (17
definições) e as chaves JSONB efetivamente utilizadas em cargos e pessoas.
Pessoas usam `curriculo_lattes_url`, `numero_calcado` e `tamanho_fardamento`;
cargos usam `acumulavel_constitucional`, `conselho_classe_obrigatorio` e
`exige_porte_arma`. Nenhuma definição ou chave existente corresponde a
adiantamento ou carga horária padrão. Portanto, a ausência não foi deduzida
somente dos nomes de propriedades JPA; não há extensão governada provisionada
que possa ser reutilizada para esses dois requisitos.

Essas diferenças devem constar no scorecard e no PR. A investigação não autoriza
tratar os exemplos ausentes como capacidade implementada nem mudar o schema.
